# PRD：问诊患者用户名密码登录

## 文档元信息

| 字段 | 值 |
|---|---|
| 特性名称 | 问诊患者用户名密码登录 |
| 特性 ID | FEAT-PATIENT-LOGIN |
| 文档版本 | v1.0 |
| 创建日期 | 2026-10-30 |
| 状态 | 草稿（待确认） |
| 作者 | AI Coding 助手 |
| 评审人 | 待指定 |
| 关联需求 | `README.md` 需求列表第 3 条："为问诊用户提供用户名密码登录方式" |

---

## 1. 背景与目标

### 1.1 背景

**当前现状**

问诊页面（`web/qa-web/src/views/Consultation.vue`）当前采用「姓名 + 生日」的弱校验方式识别患者：
患者填写姓名与生日后即可进入问诊页面并提交问题；若姓名与生日组合不存在，
`store.verifyPatient()` 会**自动创建一个新患者**并放行。

**存在的问题**

| 问题 | 影响 |
|---|---|
| 无密码，任何人知道他人姓名与生日即可查看其历史问题、代替其问诊 | 严重的隐私泄露 |
| 自动建号导致"输错生日"也会被静默接受 | 用户困惑、垃圾账号 |
| 无账号概念，患者无法跨设备使用，也无法找回自己的问诊记录 | 体验缺失 |
| 患者数据仅存于浏览器内存与 `src/data/patient-user.json` | 数据不可持久、不可审计 |
| 与医生侧已有的用户名密码登录（`DoctorLogin.vue`）体验不一致 | 体系不统一 |

**业务动因**

平台定位为「专业在线医疗问诊平台」，问诊记录属于敏感医疗健康数据。
必须引入真实的身份凭证（用户名 + 密码）与服务端持久化，才能满足基本的
数据安全与合规要求，同时使「医生解答患者问题」这条主链路具备可追溯的责任主体。

### 1.2 目标

| 编号 | 目标 | 度量方式 | 目标值 |
|---|---|---|---|
| G-1 | 患者可用用户名 + 密码注册并登录，登录后方可问诊 | 接口测试 + 人工验收 | 100% 通过 |
| G-2 | 密码以不可逆方式存储，服务端任何接口不返回明文密码 | 代码审查 + 接口响应检查 | 0 处泄露 |
| G-3 | 密码错误的提示不泄露账号是否存在 | 接口响应检查 | 统一文案 |
| G-4 | 患者数据持久化到 MySQL，重启服务后账号仍在 | 库表直查 + 重启验证 | 100% |
| G-5 | 登录态在页面刷新后保持 | 人工验收 | 通过 |
| G-6 | 现有医生登录、医生列表接口功能不回归 | 回归测试 | 全部通过 |

### 1.3 非目标（Out of Scope）

- **不做**短信验证码 / 邮箱验证 / 第三方 OAuth 登录
- **不做**密码找回（重置密码）流程的完整实现
  （本特性仅预留 `email` 字段与接口返回 `emailNotBound` 的提示位，不实现发送邮件）
- **不做**患者角色的权限体系与 RBAC（当前仅区分「已登录患者」与「未登录访客」）
- **不做**问诊记录（questions）的数据库迁移 —— 本特性只处理**患者账号**本身，
  问诊记录仍沿用前端 `store` 存储（见第 10 节风险 R-3）
- **不做**多因素认证（MFA）、登录设备管理
- **不做**注册时的手机号短信验证

### 1.4 术语表

| 术语 | 定义 |
|---|---|
| 患者账号 | 由用户名唯一标识的登录主体，存储于 `patients` 表 |
| 游客模式 | 未登录时访问问诊页的状态，只能浏览医生列表，不能提交问题 |
| BCrypt | 自带的加盐慢哈希算法，用于密码单向不可逆存储 |
| 登录态 | 前端持有的、可证明用户已认证的状态载体 |
| `qa_live` | 本项目 docker-compose 编排的 MySQL 数据库名 |

---

## 2. 用户故事

### US-1 患者注册

**作为** 首次使用平台问诊的患者
**我希望** 用一个用户名和密码创建自己的账号
**以便** 之后能安全地登录、查看并管理自己的问诊记录

**验收标准**：
- [ ] AC-1.1 输入合法用户名与密码后，提交注册请求返回成功，并自动登录进入问诊页面
- [ ] AC-1.2 用户名重复时注册失败，提示「用户名已被占用」
- [ ] AC-1.3 用户名或密码不符合格式要求时，在前端即给出明确的字段级错误提示
- [ ] AC-1.4 注册成功后，数据库 `patients` 表中存在对应记录，且 `password` 列为 BCrypt 哈希而非明文

**关联需求**：REQ-001、REQ-002、REQ-003

---

### US-2 患者登录

**作为** 已有账号的患者
**我希望** 用用户名和密码登录
**以便** 进入问诊页面并继续我之前的问诊

**验收标准**：
- [ ] AC-2.1 用户名与密码正确时登录成功，返回登录态并跳转到问诊页面
- [ ] AC-2.2 用户名不存在与密码错误时，返回**相同**的错误提示
- [ ] AC-2.3 连续多次登录失败时触发限流，返回明确的「尝试过于频繁」提示
- [ ] AC-2.4 刷新页面后登录态保持，用户无需重新登录

**关联需求**：REQ-004、REQ-005、REQ-006

---

### US-3 登出

**作为** 已登录的患者
**我希望** 退出当前账号
**以便** 在共用设备上不留下我的问诊记录

**验收标准**：
- [ ] AC-3.1 点击登出后登录态被清除，页面回到未登录状态
- [ ] AC-3.2 登出后再次访问问诊页，需重新登录才能提交问题
- [ ] AC-3.3 登出后其他用户无法看到上一位患者的历史问题

**关联需求**：REQ-007

---

### US-4 未登录拦截

**作为** 未登录的访客
**我希望** 在未登录时无法提交问诊问题
**以便** 平台能保证每条问诊都有真实的责任主体

**验收标准**：
- [ ] AC-4.1 未登录状态下访问问诊页时，页面展示注册/登录表单而非直接展示问题列表
- [ ] AC-4.2 未登录时不存在任何可提交问题的入口
- [ ] AC-4.3 已登录时，页面展示「我的问题」列表与「提交问题」按钮

**关联需求**：REQ-008

---

## 3. 功能需求

### 3.1 需求清单

| 需求 ID | 摘要 | 优先级 | 关联故事 | 状态 |
|---|---|---|---|---|
| REQ-001 | 患者注册接口 | P0 | US-1 | 待开发 |
| REQ-002 | 用户名唯一性约束 | P0 | US-1 | 待开发 |
| REQ-003 | 患者注册页前端表单 | P0 | US-1 | 待开发 |
| REQ-004 | 患者登录接口 | P0 | US-2 | 待开发 |
| REQ-005 | 登录失败限流 | P1 | US-2 | 待开发 |
| REQ-006 | 登录态保持与登出 | P0 | US-2, US-3 | 待开发 |
| REQ-007 | 登出接口与前端登出 | P0 | US-3 | 待开发 |
| REQ-008 | 未登录拦截问诊提交 | P0 | US-4 | 待开发 |
| REQ-009 | 患者数据初始化迁移 | P1 | US-1, US-2 | 待开发 |
| REQ-010 | 患者账号前端请求层 | P1 | US-1, US-2 | 待开发 |

### 3.2 需求详述

#### REQ-001 患者注册接口

- **描述**：提供 `POST /api/patients/register`，创建患者账号并在成功后返回登录态。
- **优先级**：P0
- **关联故事**：US-1
- **输入**：
  - `username`（必填）：字符串
  - `password`（必填）：字符串
  - `name`（必填）：字符串，患者姓名/昵称
  - `birthday`（选填）：`YYYY-MM-DD`
  - `phone`（选填）：字符串
  - `gender`（选填）：`男` / `女` / `其他`
- **处理逻辑**：
  1. 服务端执行与前端一致的格式校验（不信任前端）
  2. 校验用户名唯一性；冲突返回 `409`
  3. 使用 BCrypt 对密码哈希后存储，**明文密码不得落库、不得入日志**
  4. 生成会话标识并返回
- **输出**：见第 5.3 节接口定义
- **验收标准**：
  - [ ] 合法请求返回 `201`，且响应体不含 `password` 字段
  - [ ] 重复用户名返回 `409`，错误码 `PATIENT_USERNAME_TAKEN`
  - [ ] 数据库中 `password` 列以 `$2a$` / `$2b$` 开头（BCrypt 特征）

---

#### REQ-002 用户名唯一性约束

- **描述**：用户名是患者的唯一标识。
- **优先级**：P0
- **关联故事**：US-1
- **输入/输出**：同 REQ-001
- **处理逻辑**：
  - 数据库层：`username` 列建唯一索引（`UNIQUE`），作为并发下的最终防线
  - 应用层：注册前先查重，给出友好提示
- **验收标准**：
  - [ ] 数据库 `SHOW CREATE TABLE patients` 显示 `username` 带唯一键
  - [ ] 并发注册同一用户名时，仅一个请求成功，另一个返回 `409`

---

#### REQ-003 患者注册页前端表单

- **描述**：在问诊页未登录区域提供注册表单（与登录表单切换）。
- **优先级**：P0
- **关联故事**：US-1
- **输入**：与 REQ-001 相同
- **处理逻辑**：
  - 表单字段级校验，校验通过才允许提交
  - 提交期间按钮进入 loading，禁用重复提交
  - 注册成功后自动进入登录态并展示问诊页面
- **输出**：注册成功提示 + 页面状态切换
- **验收标准**：
  - [ ] 未通过前端校验时，字段下方展示具体错误文案
  - [ ] 注册过程中按钮为 loading 且不可重复点击
  - [ ] 注册成功后页面直接展示「我的问题」与「提交问题」

---

#### REQ-004 患者登录接口

- **描述**：提供 `POST /api/patients/login`，校验凭证并返回登录态。
- **优先级**：P0
- **关联故事**：US-2
- **输入**：
  - `username`（必填）：字符串
  - `password`（必填）：字符串
- **处理逻辑**：
  1. 按用户名查患者；**查不到时也执行一次 BCrypt 比对**（对不存在的用户用固定假哈希比对，抹平时间差）
  2. 用 `BCryptPasswordEncoder.matches()` 校验密码
  3. 校验失败 → 统一返回 `401` + `PATIENT_LOGIN_FAILED`
  4. 校验成功 → 重置该用户的失败计数，返回登录态
- **输出**：见第 5.3 节
- **验收标准**：
  - [ ] 正确凭证返回 `200` 与登录态
  - [ ] 用户名不存在返回 `401`，错误码与「密码错误」完全一致
  - [ ] 响应体与日志中均不出现密码

---

#### REQ-005 登录失败限流

- **描述**：防止针对单个账号的暴力破解。
- **优先级**：P1
- **关联故事**：US-2
- **输入**：同 REQ-004
- **处理逻辑**：
  - 记录每个用户名的连续失败次数与最后失败时间
  - 失败达到阈值（建议 5 次）后锁定一段时间（建议 5 分钟）
  - 锁定期间直接返回 `429` + `PATIENT_LOGIN_LOCKED`
  - 登录成功或锁定过期后计数清零
- **输出**：`429` 响应
- **验收标准**：
  - [ ] 连续 5 次错误后，第 6 次请求返回 `429`
  - [ ] 锁定期间即使密码正确也返回 `429`
  - [ ] 正确登录一次后失败计数被清零

---

#### REQ-006 登录态保持与登出

- **描述**：登录成功后，用户在页面刷新后仍处于登录态。
- **优先级**：P0
- **关联故事**：US-2、US-3
- **输入**：注册/登录接口返回的 `token`
- **处理逻辑**：
  - 前端将 `token` 持久化到 `localStorage`
  - 应用启动时读取 `localStorage` 中的 `token`，调用 `GET /api/patients/me` 校验其有效性
  - 校验通过则恢复登录态；失败（过期/非法）则清除并回到未登录态
- **输出**：登录态恢复或清除
- **验收标准**：
  - [ ] 登录后刷新页面，`GET /api/patients/me` 返回当前患者信息，页面保持登录态
  - [ ] 手工把 `localStorage` 中的 token 改成非法值后刷新，页面回到未登录态
  - [ ] 问诊页面在未登录时不展示任何历史问题列表

---

#### REQ-007 登出接口与前端登出

- **描述**：提供 `POST /api/patients/logout` 使当前会话失效。
- **优先级**：P0
- **关联故事**：US-3
- **输入**：`Authorization: Bearer <token>`
- **处理逻辑**：
  1. 服务端将 token 加入失效列表（或直接采用无状态 token + 短有效期，见第 6.2 节）
  2. 前端清除 `localStorage` 中的 token，重置患者状态
  3. 前端清空 `store` 中的患者相关数据
- **输出**：`204 No Content`
- **验收标准**：
  - [ ] 登出后 `localStorage` 中的 token 被清除
  - [ ] 登出后使用旧 token 调用 `/api/patients/me` 返回 `401`
  - [ ] 登出后页面回到注册/登录表单

---

#### REQ-008 未登录拦截问诊提交

- **描述**：登录是问诊的前置条件。
- **优先级**：P0
- **关联故事**：US-4
- **输入**：问诊页访问
- **处理逻辑**：
  - 未登录时，`Consultation.vue` 渲染注册/登录表单，不渲染「提交问题」按钮与「我的问题」列表
  - 登录态由 `store.currentPatient` 统一持有，其他组件通过它判断
- **输出**：受保护的问诊能力
- **验收标准**：
  - [ ] 未登录访问 `/consultation` 时不渲染「提交问题」按钮
  - [ ] 未登录访问 `/consultation` 时不渲染任何问题条目
  - [ ] 登录后上述两项均正常渲染

---

#### REQ-009 患者账号初始化迁移

- **描述**：把存量患者数据从 `src/data/patient-user.json` 迁移到 MySQL `patients` 表。
- **优先级**：P1
- **关联故事**：US-1、US-2
- **输入**：`web/qa-web/src/data/patient-user.json` 的 5 条记录
- **处理逻辑**：
  - 参照医生数据的既有做法（`DoctorDataInitializer`），在 `qa-service-user` 中新增 `PatientDataInitializer`
  - 启动时读取 JSON 种子文件，在 `patients` 表为空时写入
  - 存量记录无密码字段，需生成可用的初始密码（建议 `username` + 统一默认密码），并提示用户后续修改
- **输出**：`patients` 表的 5 条种子数据
- **验收标准**：
  - [ ] 服务启动后 `patients` 表存在 5 条记录，字段与 JSON 一致
  - [ ] 种子数据幂等：重复启动不产生重复数据
  - [ ] 种子数据的密码为 BCrypt 哈希，明文不落库

---

#### REQ-010 患者账号前端请求层

- **描述**：前端调用患者相关接口的统一封装。
- **优先级**：P1
- **关联故事**：US-1、US-2
- **输入**：注册/登录/登出/查询当前患者的请求
- **处理逻辑**：
  - 复用已有的 `web/qa-web/src/api/client.ts`，在其基础上补充 `POST` 与带 `Authorization` 头的请求能力
  - 新增 `web/qa-web/src/api/patient.ts` 封装患者接口
  - 统一处理 `401`：清除本地 token 并回到未登录态
- **输出**：类型化的接口调用函数
- **验收标准**：
  - [ ] 所有患者请求统一走 `src/api/` 下的封装，不在组件内裸写 `fetch`
  - [ ] 请求失败时复用 `AsyncState.vue` 的错误态与重试按钮
  - [ ] `npx vue-tsc -b` 通过（exit 0）

---

## 4. 校验规则与异常提示

### 4.1 校验规则

| 编号 | 字段/场景 | 规则 | 触发时机 | 错误码 |
|---|---|---|---|---|
| V-01 | `username` | 必填；长度 4–32；仅允许字母、数字、下划线、连字符；**大小写不敏感唯一** | 前端 + 后端 | `PATIENT_USERNAME_INVALID` |
| V-02 | `password` | 必填；长度 8–32；必须同时包含字母和数字 | 前端 + 后端 | `PATIENT_PASSWORD_INVALID` |
| V-03 | `name` | 必填；长度 1–32 | 前端 + 后端 | `PATIENT_NAME_INVALID` |
| V-04 | `birthday` | 选填；格式 `YYYY-MM-DD`；不得晚于当天 | 前端 + 后端 | `PATIENT_BIRTHDAY_INVALID` |
| V-05 | `phone` | 选填；11 位中国大陆手机号（`^1[3-9]\d{9}$`） | 前端 + 后端 | `PATIENT_PHONE_INVALID` |
| V-06 | `gender` | 选填；枚举 `男`/`女`/`其他` | 前端 + 后端 | `PATIENT_GENDER_INVALID` |
| V-07 | `username` 唯一 | 注册时不可与已有用户名重复（忽略大小写） | 后端 | `PATIENT_USERNAME_TAKEN` |
| V-08 | 登录凭证 | 用户名或密码不匹配 | 后端 | `PATIENT_LOGIN_FAILED` |
| V-09 | 登录频次 | 同用户名连续失败 5 次锁定 5 分钟 | 后端 | `PATIENT_LOGIN_LOCKED` |
| V-10 | 请求体 | 注册/登录为 JSON 对象，字段缺失或类型错误 | 后端 | `PATIENT_BAD_REQUEST` |

> 约定：**服务端必须独立执行全部校验**，前端校验仅为体验优化，不可作为安全边界。

### 4.2 异常提示文案

| 场景 | 用户可见文案 | 错误码 | HTTP 状态码 |
|---|---|---|---|
| 用户名为空 | 请输入用户名 | `PATIENT_USERNAME_INVALID` | 400 |
| 用户名格式非法 | 用户名需为 4-32 位，仅支持字母、数字、下划线和连字符 | `PATIENT_USERNAME_INVALID` | 400 |
| 密码格式非法 | 密码需为 8-32 位，且必须同时包含字母和数字 | `PATIENT_PASSWORD_INVALID` | 400 |
| 姓名为空 | 请输入姓名 | `PATIENT_NAME_INVALID` | 400 |
| 生日格式非法 | 生日格式应为 YYYY-MM-DD | `PATIENT_BIRTHDAY_INVALID` | 400 |
| 手机号格式非法 | 请输入有效的手机号 | `PATIENT_PHONE_INVALID` | 400 |
| 性别取值非法 | 性别取值不合法 | `PATIENT_GENDER_INVALID` | 400 |
| 用户名已存在 | 该用户名已被占用，请更换 | `PATIENT_USERNAME_TAKEN` | 409 |
| **登录失败（用户名不存在）** | **用户名或密码错误** | `PATIENT_LOGIN_FAILED` | 401 |
| **登录失败（密码错误）** | **用户名或密码错误** | `PATIENT_LOGIN_FAILED` | 401 |
| 登录过于频繁 | 登录尝试过于频繁，请 5 分钟后再试 | `PATIENT_LOGIN_LOCKED` | 429 |
| 未登录访问受保护接口 | 请先登录 | `PATIENT_UNAUTHORIZED` | 401 |
| token 失效或非法 | 登录已过期，请重新登录 | `PATIENT_UNAUTHORIZED` | 401 |
| 服务端异常 | 服务异常，请稍后重试 | `PATIENT_INTERNAL_ERROR` | 500 |

> **安全要求**：登录失败必须统一为「用户名或密码错误」，禁止区分「用户不存在」与「密码错误」，
> 以避免账号枚举攻击（SEC-03）。

---

## 5. 接口定义

### 5.1 通用约定

| 项 | 值 |
|---|---|
| 服务 | `qa-service-user`（Spring Boot 3.5.7 / Java 17） |
| 基础路径 | `http://localhost:8080`（前端通过 `.env` 的 `VITE_API_BASE_URL` 配置） |
| 前缀 | `/api/patients` |
| 认证方式 | `Authorization: Bearer <token>`（受保护接口） |
| 请求/响应格式 | `application/json; charset=UTF-8` |
| 跨域 | 复用现有 `CorsConfig`（`allowedOriginPatterns("*")`），无需改动 |
| 字符集 | UTF-8（数据库 `utf8mb4_unicode_ci`） |

### 5.2 接口清单

| 接口 | 方法 | 路径 | 认证 | 说明 |
|---|---|---|---|---|
| 患者注册 | POST | `/api/patients/register` | 否 | 创建账号并返回登录态 |
| 患者登录 | POST | `/api/patients/login` | 否 | 校验凭证并返回登录态 |
| 患者登出 | POST | `/api/patients/logout` | 是 | 使当前会话失效 |
| 当前患者 | GET | `/api/patients/me` | 是 | 查询当前登录患者信息（用于刷新后恢复登录态） |

### 5.3 接口详述

#### 5.3.1 患者注册

- **方法与路径**：`POST /api/patients/register`
- **认证**：无
- **请求体**：
```json
{
  "username": "zhangsan",
  "password": "qa123456",
  "name": "张三",
  "birthday": "1990-01-01",
  "phone": "13800138000",
  "gender": "男"
}
```
- **成功响应**（`201`）：
```json
{
  "code": "OK",
  "message": "注册成功",
  "data": {
    "token": "8f14e45fceea167a5a36dedd4bea2543",
    "patient": {
      "id": "patient006",
      "username": "zhangsan",
      "name": "张三",
      "birthday": "1990-01-01",
      "phone": "13800138000",
      "gender": "男"
    }
  }
}
```
- **失败响应**（`400` / `409`）：
```json
{
  "code": "PATIENT_USERNAME_TAKEN",
  "message": "该用户名已被占用，请更换",
  "data": null
}
```
- **说明**：
  - 响应中的 `patient` 对象**不含 `id` 之外的敏感字段，且绝不含 `password`**
  - `token` 为随机不透明串（建议 32 位十六进制/Base64），服务端保存会话映射
  - `id` 由服务端生成（形如 `patient006`），不使用前端传入值

#### 5.3.2 患者登录

- **方法与路径**：`POST /api/patients/login`
- **认证**：无
- **请求体**：
```json
{
  "username": "zhangsan",
  "password": "qa123456"
}
```
- **成功响应**（`200`）：结构同注册接口（`message` 为「登录成功」）
- **失败响应**（`401` / `429`）：
```json
{
  "code": "PATIENT_LOGIN_FAILED",
  "message": "用户名或密码错误",
  "data": null
}
```
- **说明**：
  - 用户名不存在时，服务端仍执行一次 BCrypt 比对（使用固定假哈希），使响应时间与「密码错误」接近

#### 5.3.3 患者登出

- **方法与路径**：`POST /api/patients/logout`
- **认证**：是（`Authorization: Bearer <token>`）
- **请求体**：无
- **成功响应**（`204`）：无响应体
- **失败响应**（`401`）：token 缺失或已失效
- **说明**：登出后该 token 立即失效

#### 5.3.4 当前患者

- **方法与路径**：`GET /api/patients/me`
- **认证**：是
- **请求体**：无
- **成功响应**（`200`）：
```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "id": "patient006",
    "username": "zhangsan",
    "name": "张三",
    "birthday": "1990-01-01",
    "phone": "13800138000",
    "gender": "男"
  }
}
```
- **失败响应**（`401`）：
```json
{
  "code": "PATIENT_UNAUTHORIZED",
  "message": "登录已过期，请重新登录",
  "data": null
}
```
- **说明**：前端刷新页面时调用此接口校验本地 token 是否仍然有效

---

## 6. 数据模型

### 6.1 实体定义

**表名**：`patients`

| 属性 | 类型 | 约束 | 说明 |
|---|---|---|---|
| `id` | `varchar(64)` | `NOT NULL`，主键 | 患者 ID，形如 `patient001` |
| `username` | `varchar(64)` | `NOT NULL`，`UNIQUE` | 登录用户名，大小写不敏感唯一 |
| `password` | `varchar(100)` | `NOT NULL` | **BCrypt 哈希**（含盐，长度 60），非明文 |
| `name` | `varchar(64)` | `NOT NULL` | 患者姓名/昵称 |
| `birthday` | `varchar(10)` | 可空 | `YYYY-MM-DD` |
| `phone` | `varchar(20)` | 可空 | 手机号 |
| `gender` | `varchar(10)` | 可空 | `男`/`女`/`其他` |
| `created_at` | `datetime` | `NOT NULL`，默认当前时间 | 账号创建时间 |
| `updated_at` | `datetime` | `NOT NULL`，自动更新 | 账号最后更新时间 |

- **主键**：`id`（字符串，由应用生成）
- **索引**：`username` 唯一索引；`created_at` 普通索引（为后续按时间查询预留）
- **关系**：本特性内 `patients` 与医生表 `doctors` 无外键关系；`questions`（问诊记录）当前仍在前端 store，暂无外键
- **表引擎/字符集**：`InnoDB` / `utf8mb4_unicode_ci`（与 `doctors` 表保持一致，由 JPA `ddl-auto=update` 自动创建）

### 6.2 字段约束与安全要求

**密码存储**

- 使用 Spring Security Crypto 的 `BCryptPasswordEncoder`（强度因子 10）
- 数据库中仅存储形如 `$2a$10$...` 的哈希串，**永不存储明文、永不使用可逆加密**
- 校验使用 `matches(raw, hash)`，禁止用 `equals` 比较

**敏感字段**

| 字段 | 是否可出现在响应中 | 说明 |
|---|---|---|
| `password` | ❌ 绝不 | 仅以 BCrypt 哈希形式存库，任何接口均不返回 |
| `id` | ✅ | 患者自身标识 |
| `username` / `name` / `birthday` / `phone` / `gender` | ✅ | 供本人查看 |

**迁移要求**

- 存量 `src/data/patient-user.json` 的 5 条记录无密码字段，迁移时统一生成初始密码
  （建议规则：`username` 为 `patient001`–`patient005`，初始密码为 `qa123456`）
- 初始密码同样以 BCrypt 存储
- 迁移完成后 `src/data/patient-user.json` 可保留作为后端种子数据来源
  （与医生数据保持一致的做法），但前端 `store` 不再直接引用它作为运行时数据源

**会话令牌**

- 采用**服务端会话**方案：随机 `token` → 内存/持久化映射 → 患者 ID
- 登出时从映射中移除，实现立即失效
- 备选：无状态 token（HMAC 签名）+ 短有效期 + 失效名单；本特性优先采用会话方案以保证登出立即生效

---

## 7. 非功能要求

### 7.1 性能

- 注册/登录接口 P95 响应时间 < 200ms（本地环境，BCrypt 哈希计算约 50–100ms 属正常）
- `GET /api/patients/me` P95 < 50ms
- 前端登录态恢复（调用 `/me`）不阻塞页面首屏渲染

### 7.2 安全

| 编号 | 要求 | 验证方式 |
|---|---|---|
| SEC-01 | 密码必须使用 BCrypt 等单向哈希存储，禁止明文或可逆加密 | 代码审查 + 库表直查确认 `$2a$` 前缀 |
| SEC-02 | 密码不得出现在任何接口响应、日志或错误信息中 | 接口响应检查 + 日志审查 |
| SEC-03 | 登录失败提示不得区分「用户不存在」与「密码错误」 | 两个场景对比接口响应 |
| SEC-04 | 连续登录失败应有限流/锁定策略 | 连续 5 次错误后验证第 6 次返回 429 |
| SEC-05 | token 不得使用可预测的序列（如自增 ID 或时间戳） | 多次登录对比 token 无规律 |
| SEC-06 | 登出后旧 token 必须立即失效 | 登出后用旧 token 调 `/me` 返回 401 |
| SEC-07 | 所有患者接口不得受 SQL 注入影响（必须使用参数化查询） | 代码审查：仅用 JPA/参数化查询，无字符串拼接 SQL |
| SEC-08 | 注册与登录接口应限制请求体大小 | 使用 Spring Boot 默认限制或显式配置 |
| SEC-09 | 服务端日志不得输出请求体中的密码字段 | 日志审查 |

### 7.3 可用性与兼容性

- 后端：`qa-service-user` 现有 8080 端口与 CORS 配置不变，无需额外配置
- 前端：沿用 Vite 5 + Vue 3 + Ant Design Vue 4 技术栈，不引入新的大型依赖
- BCrypt 通过 `spring-security-crypto` 单库引入（**不引入完整 Spring Security**，避免其默认拦截所有接口带来额外配置成本）
- 数据迁移后需保证 `qa_live` 库中 `doctors` 表既有数据不受影响

### 7.4 可维护性与可观测性

- 沿用现有分层结构：`entity` / `repository` / `service` / `controller` / `dto`
- 错误码统一走 `code` + `message` 的响应结构，与既有接口风格保持一致
- 登录成功/失败事件建议记录日志（含用户名与来源 IP，**不含密码**），便于后续风控扩展
- 后续可扩展点：密码修改、密码找回、登录历史

### 7.5 国际化

- **需要接入 i18n**：问诊页的新增文案（登录/注册标题、字段标签、按钮、错误提示）建议接入既有
  `web/qa-web/src/locales/zh-CN.json` 与 `en-US.json`
- 新增语言 key 前缀建议：`patient.*`（如 `patient.login.title`、`patient.register.username`）
- **注意**：后端返回的 `message` 为中文，暂不做多语言；若前端需要多语言错误提示，
  应改为由前端根据 `code` 映射本地文案（推荐做法，可与第 4.2 节的错误码表对应）
- 本特性**不涉及**首页，故不改动首页已有的 i18n 结构

---

## 8. 验收标准

### 8.1 功能验收

| 编号 | 验收标准 | 验证方式 | 关联需求 |
|---|---|---|---|
| AC-01 | 注册成功后返回 201，响应体不含 password，库中 password 为 BCrypt 哈希 | curl + 库表直查 | REQ-001、REQ-002 |
| AC-02 | 重复用户名注册返回 409 与 `PATIENT_USERNAME_TAKEN` | curl | REQ-002 |
| AC-03 | 用户名/密码格式非法时返回 400 与对应错误码 | curl | REQ-001 |
| AC-04 | 正确凭证登录返回 200 与 token | curl | REQ-004 |
| AC-05 | 用户名不存在与密码错误返回完全相同的 401 响应体 | curl 对比 | REQ-004 |
| AC-06 | 连续 5 次错误登录后，第 6 次返回 429 | curl | REQ-005 |
| AC-07 | `GET /api/patients/me` 带有效 token 返回 200 与当前患者信息 | curl | REQ-006 |
| AC-08 | `GET /api/patients/me` 不带/带非法 token 返回 401 | curl | REQ-006 |
| AC-09 | `POST /api/patients/logout` 返回 204，随后旧 token 调 `/me` 返回 401 | curl | REQ-007 |
| AC-10 | 登录后刷新问诊页，页面保持登录态并展示「我的问题」 | 浏览器人工验收 | REQ-006 |
| AC-11 | 未登录访问问诊页不渲染「提交问题」按钮与问题列表 | 浏览器人工验收 | REQ-008 |
| AC-12 | 登出后页面回到登录表单，且不再显示上一位患者的问题 | 浏览器人工验收 | REQ-007、REQ-008 |
| AC-13 | 存量 5 条患者数据已迁移到 `patients` 表且为 BCrypt 密码 | 库表直查 | REQ-009 |
| AC-14 | 医生登录、医生列表接口功能不受影响（回归） | curl 回归 | G-6 |

### 8.2 质量门禁

| 项 | 命令 | 通过标准 |
|---|---|---|
| 后端编译 | `cd server/qa-service-user && ./mvnw -q clean compile` | exit 0 |
| 后端测试 | `cd server/qa-service-user && ./mvnw test` | 全部通过 |
| 前端类型检查 | `cd web/qa-web && npx vue-tsc -b` | exit 0 |
| 前端构建 | `cd web/qa-web && npm run build` | exit 0 |
| 接口测试 | 见 `docs/API_TEST.md`（本特性测试用例待补充） | 全部符合预期 |

### 8.3 人工验收

1. 未登录访问 `http://localhost:5173/consultation`，页面显示「患者身份验证」区的登录/注册切换表单
2. 注册新账号 `zhangsan / qa123456`，成功后页面直接展示「我的问题」与「提交问题」按钮
3. 刷新页面，仍处于登录态，页面顶部显示当前患者姓名
4. 提交一条问题后登出，再次登录同一账号，问题列表中可见该问题
5. 登出后刷新页面，回到未登录状态
6. 切换到「注册」标签，输入非法用户名（如 `ab`）与非法密码（如 `12345678`），提交后在字段下方看到明确错误提示
7. 医生登录页（`/doctor/login`）仍可正常登录并进入诊室

---

## 9. 任务拆分

### 9.1 拆分顺序

| 顺序 | 任务 ID | 任务名称 | 依赖 | 可并行 | 产出 |
|---|---|---|---|---|---|
| 1 | T-01 | 后端患者实体与数据层（实体、Repository、密码编码器、数据迁移初始化） | 无 | 与 T-02 并行 | `patients` 表自动建表 + 5 条种子数据 |
| 2 | T-02 | 后端患者认证接口（注册/登录/登出/me + 校验 + 限流） | 无 | 与 T-01 并行 | 4 个 REST 接口 |
| 3 | T-03 | 后端接口单元/集成测试 | T-01, T-02 | 否 | 测试用例与报告 |
| 4 | T-04 | 前端 API 请求层扩展（POST + Authorization + patient.ts） | 无 | 与 T-01/T-02 并行 | `src/api/` 封装 |
| 5 | T-05 | 前端 store 接入登录态（currentPatient、token 持久化、登出、401 处理） | T-02, T-04 | 否 | store 改造 |
| 6 | T-06 | 前端问诊页登录/注册表单与状态切换 | T-05 | 否 | Consultation.vue 改造 |
| 7 | T-07 | 前端 i18n 文案补充（zh-CN / en-US） | T-06 | 否 | 语言包更新 |
| 8 | T-08 | 前后端联调与接口测试文档 | T-03, T-06 | 否 | `docs/API_TEST.md` 更新 + 测试报告 |

> **并行说明**：T-01/T-02/T-04 三者无相互依赖，可并行推进。
> **合并说明**：T-01 与 T-02 若由单人顺序执行，可合并为一个"后端认证实现"任务（工作量约 2-3 小时），
> 但保留拆分有助于并行与独立验收。

> **总任务数：8**（≤ 10，符合拆分原则，无需再拆分子特性）

### 9.2 任务明细

#### T-01 后端患者实体与数据层

- **目标**：在 `qa-service-user` 中建立患者账号的持久化能力。
- **依赖**：无
- **涉及文件**：
  - `server/qa-service-user/pom.xml`（新增 `spring-security-crypto` 依赖）
  - `server/qa-service-user/src/main/java/.../entity/Patient.java`
  - `server/qa-service-user/src/main/java/.../repository/PatientRepository.java`
  - `server/qa-service-user/src/main/java/.../config/PasswordEncoderConfig.java`
  - `server/qa-service-user/src/main/java/.../init/PatientDataInitializer.java`
- **实现要点**：
  - 实体字段与第 6.1 节一致；`password` 列长 100 容纳 BCrypt 哈希
  - `username` 建唯一索引；大小写不敏感的唯一性在服务层用 `findByUsernameIgnoreCase` 处理
  - `PatientDataInitializer` 参照现有 `DoctorDataInitializer` 的写法，读取
    `../../web/qa-web/src/data/patient-user.json`，在表为空时写入，并为无密码的存量记录生成初始密码
  - 密码列在日志与 `toString()` 中必须被排除
- **验收标准**：
  - [ ] `./mvnw -q clean compile` exit 0
  - [ ] 服务启动后 `patients` 表存在且含 5 条种子数据
  - [ ] `password` 列值以 `$2a$` 开头
  - [ ] 重复启动不产生重复数据
- **验证命令**：
  ```bash
  cd server/qa-service-user
  ./mvnw -q clean compile
  ./mvnw spring-boot:run
  docker exec qa-live-mysql mysql -uroot -proot qa_live -e "SELECT id,username,name FROM patients;"
  ```

#### T-02 后端患者认证接口

- **目标**：实现注册、登录、登出、查询当前患者 4 个接口及配套校验与限流。
- **依赖**：无（可与 T-01 并行开发，完成后联调）
- **涉及文件**：
  - `.../controller/PatientController.java`
  - `.../service/PatientAuthService.java`
  - `.../dto/*`（RegisterRequest / LoginRequest / PatientResponse / ApiResponse）
  - `.../service/LoginAttemptService.java`（失败计数与锁定）
  - `.../exception/*`（全局异常处理器，统一错误码与状态码）
- **实现要点**：
  - 严格按第 4.1 节校验表实现服务端校验，不信任前端
  - 登录失败时对不存在的用户也执行一次 `matches()` 比对，消除时间差
  - 限流以「用户名」为维度：失败计数 + 最后失败时间，5 次失败锁定 5 分钟
  - token 为 `UUID`/安全随机数生成，存入服务端会话映射；登出即移除
  - 全局异常处理器把校验异常、唯一约束冲突、限流分别映射为 400 / 409 / 429
- **验收标准**：
  - [ ] 4 个接口按第 5.3 节定义返回，响应结构为 `{code, message, data}`
  - [ ] 所有响应体均不含 `password`
  - [ ] 用户名不存在与密码错误的 401 响应体**逐字节相同**
  - [ ] `./mvnw -q clean compile` exit 0
- **验证命令**：
  ```bash
  cd server/qa-service-user
  ./mvnw -q clean compile
  curl -i -X POST http://localhost:8080/api/patients/login \
    -H "Content-Type: application/json" \
    -d '{"username":"zhangsan","password":"qa123456"}'
  ```

#### T-03 后端接口测试

- **目标**：为核心认证逻辑编写自动化测试。
- **依赖**：T-01、T-02
- **涉及文件**：`server/qa-service-user/src/test/java/.../PatientAuthServiceTests.java` 等
- **实现要点**：
  - 覆盖：注册成功、用户名重复、密码哈希存储、登录成功、密码错误、用户不存在（响应一致）、
    限流触发与解除、登出后 token 失效
  - 使用 `@SpringBootTest` + MockMvc；若引入 Testcontainers 需复用
    `qa-service-question` 中已有的 `TestcontainersConfiguration` 模式
- **验收标准**：
  - [ ] `./mvnw test` 全部通过（exit 0）
  - [ ] 覆盖上述全部场景
- **验证命令**：
  ```bash
  cd server/qa-service-user
  ./mvnw test
  ```

#### T-04 前端 API 请求层扩展

- **目标**：让前端具备发起带认证头的 POST 请求的能力。
- **依赖**：无
- **涉及文件**：
  - `web/qa-web/src/api/client.ts`（扩展 `post`、`auth` 选项、统一错误结构）
  - `web/qa-web/src/api/patient.ts`（新增）
- **实现要点**：
  - 保持既有 `get()` 的签名向后兼容，不破坏医生接口调用
  - 统一解析 `{code, message, data}`，非 `OK` 时抛出带 `code` 的错误对象
  - `patient.ts` 导出 `registerPatient`、`loginPatient`、`logoutPatient`、`fetchCurrentPatient`
  - 响应结构与后端第 5.3 节严格一致
- **验收标准**：
  - [ ] `npx vue-tsc -b` exit 0
  - [ ] 医生接口调用未受影响（`/api/doctors` 仍正常返回）
- **验证命令**：
  ```bash
  cd web/qa-web
  npx vue-tsc -b
  npm run build
  ```

#### T-05 前端 store 接入登录态

- **目标**：让前端 store 持有登录态，并打通注册/登录/登出流程。
- **依赖**：T-02、T-04
- **涉及文件**：`web/qa-web/src/store/index.ts`
- **实现要点**：
  - `currentPatient` 继续作为登录态判据；新增 `token` 存储与持久化（`localStorage`）
  - 新增 `loginPatient`、`registerPatient`、`logoutPatient`、`restoreSession` 动作
  - `restoreSession` 在应用启动时调用 `/me` 校验本地 token
  - `logoutPatient` 需同时清空 `state.currentPatient` 与本地 token，并调用登出接口
  - 移除 `store.verifyPatient()` 的「自动建号」行为（该行为是本特性要解决的问题）
- **验收标准**：
  - [ ] 登录后刷新页面，`restoreSession` 能恢复登录态
  - [ ] 登出后 token 被清除且 `currentPatient` 为 null
  - [ ] `npx vue-tsc -b` exit 0
- **验证命令**：
  ```bash
  cd web/qa-web
  npx vue-tsc -b
  ```

#### T-06 前端问诊页登录/注册表单

- **目标**：在问诊页实现登录/注册表单与状态切换，并拦截未登录问诊。
- **依赖**：T-05
- **涉及文件**：`web/qa-web/src/views/Consultation.vue`
- **实现要点**：
  - 未登录时渲染登录/注册切换表单（`a-tabs` 或两个 `a-button` 切换）
  - 表单字段级校验，规则与第 4.1 节 V-01~V-06 一致
  - 提交时按钮 loading，禁用重复提交
  - 加载失败时复用 `AsyncState.vue` 的错误态与重试按钮
  - 登录态下才渲染「我的问题」列表与「提交问题」按钮
- **验收标准**：
  - [ ] 未登录时页面无「提交问题」按钮、无问题条目
  - [ ] 非法输入在字段下方显示具体错误文案
  - [ ] 注册成功后自动进入登录态
  - [ ] `npx vue-tsc -b` exit 0
- **验证命令**：
  ```bash
  cd web/qa-web
  npx vue-tsc -b
  npm run build
  ```

#### T-07 前端 i18n 文案补充

- **目标**：为新增的登录/注册文案补齐中英文资源。
- **依赖**：T-06
- **涉及文件**：
  - `web/qa-web/src/locales/zh-CN.json`
  - `web/qa-web/src/locales/en-US.json`
- **实现要点**：
  - 新增 `patient.*` 命名空间：登录/注册标题、字段标签、按钮、校验提示
  - 后端返回的 `message` 建议按 `code` 映射为本地文案，实现多语言错误提示
  - 两种语言的 key 集合必须完全一致
- **验收标准**：
  - [ ] 两个语言文件的 `patient` key 集合完全一致（可写脚本比对）
  - [ ] 切换语言后新增文案随之变化
  - [ ] `npx vue-tsc -b` exit 0
- **验证命令**：
  ```bash
  cd web/qa-web
  node -e "const z=require('./src/locales/zh-CN.json').patient,k=require('./src/locales/en-US.json').patient;const a=Object.keys(z),b=Object.keys(k);console.log(JSON.stringify(a)===JSON.stringify(b)?'KEYS_MATCH':'KEYS_MISMATCH')"
  npx vue-tsc -b
  ```

#### T-08 前后端联调与接口测试文档

- **目标**：完成前后端联调，补充 `docs/API_TEST.md` 中的本特性用例并产出测试报告。
- **依赖**：T-03、T-06
- **涉及文件**：`docs/API_TEST.md`、`docs/TEST_REPORT.md`（或既有测试报告文件）
- **实现要点**：
  - 用 `curl` 覆盖第 8.1 节 AC-01 ~ AC-09 全部接口级验收项
  - 记录请求/响应示例与实际输出
  - 补充 AC-10 ~ AC-14 的人工验收结果
- **验收标准**：
  - [ ] `docs/API_TEST.md` 含本特性全部接口用例与预期结果
  - [ ] 每条用例有实际执行结果记录
  - [ ] 回归用例（医生接口）同样通过
- **验证命令**：
  ```bash
  # 按 docs/API_TEST.md 逐条执行 curl 用例，全部符合预期
  ```

---

## 10. 风险与应对

| 编号 | 风险 | 影响 | 应对措施 |
|---|---|---|---|
| R-1 | BCrypt 哈希导致登录接口响应变慢（约 50–100ms） | 低 | 可接受；必要时后续将强度因子下调至 8 或引入异步预校验 |
| R-2 | 前后端校验规则不一致导致「前端通过、后端 400」 | 中 | 校验规则集中定义在第 4.1 节，两端严格对齐；T-03 用测试固化后端规则 |
| R-3 | 问诊记录（questions）仍存于前端 store，登录后「我的问题」可能读不到历史数据 | **高** | 本特性仅迁移账号。问诊记录的持久化不在本特性范围，但**需在开发前确认**：若要求登录后可见跨设备历史问题，须将 `questions` 一并迁移到 MySQL（会显著扩大范围，需重新评估任务拆分） |
| R-4 | `patient-user.json` 存量数据无密码，初始密码需公开给测试者 | 中 | 采用统一弱初始密码并在测试文档中明示；PRD 已说明待补充「修改密码」能力 |
| R-5 | 引入 `spring-security-crypto` 被误用为完整 Spring Security，导致接口被默认拦截 | 中 | 仅引入 crypto 单库，不引入 `spring-boot-starter-security`；在 T-02 验收中确认既有 `/api/doctors` 接口不受影响 |
| R-6 | 后端错误信息为中文，与前端 i18n 体系冲突 | 低 | 错误码为稳定契约，前端按 `code` 映射本地文案（见 T-07） |
| R-7 | 内存态 token 在服务重启后全部失效 | 低 | 当前为本地开发环境，可接受；生产化需改为持久化会话或无状态 token（见第 6.2 节备选方案） |
| R-8 | 前端 store 的 `verifyPatient` 自动建号逻辑被多处依赖，移除后产生回归 | 中 | T-05 中全局检索 `verifyPatient` 调用点并一并改造；T-08 做回归验证 |

---

## 11. 附录

### 11.1 参考资料

- 项目需求来源：`README.md`（需求列表第 3 条）
- PRD 模版：`docs/PRD_TEMPLATE.md`
- 医生数据迁移的既有实现（可作为参照模式）：
  - `server/qa-service-user/src/main/java/com/leansofx/qaserviceuser/init/DoctorDataInitializer.java`
  - `server/qa-service-user/src/main/java/com/leansofx/qaserviceuser/entity/Doctor.java`
  - `server/qa-service-user/src/main/java/com/leansofx/qaserviceuser/controller/DoctorController.java`
- 前端 API 封装既有实现：`web/qa-web/src/api/client.ts`、`web/qa-web/src/api/doctor.ts`
- 前端异步状态组件：`web/qa-web/src/components/AsyncState.vue`
- 前端 store：`web/qa-web/src/store/index.ts`
- 问诊页：`web/qa-web/src/views/Consultation.vue`
- 数据可视化模板：`.asdm/toolsets/prd-builder/spec/feature-prd-spec.md`

### 11.2 待确认问题

| 编号 | 问题 | 影响范围 | 决策人 | 状态 |
|---|---|---|---|---|
| Q-1 | **问诊记录（questions）是否需要同时迁移到 MySQL？** 若需要，登录后应能跨设备看到历史问题 | 决定范围与任务拆分（当前 PRD 假设**不迁移**） | 需求方 | **待确认（阻塞 T-05/T-06 的范围）** |
| Q-2 | 存量 5 个患者的初始密码是否采用统一值 `qa123456`？是否需要「首次登录强制改密」？ | T-01 实现方式 | 需求方 | 待确认 |
| Q-3 | 登录失败锁定阈值（5 次 / 5 分钟）是否符合预期？ | T-02 限流参数 | 需求方 | 待确认 |
| Q-4 | token 是否需要设置有效期（如 2 小时）？还是仅靠登出失效？ | T-02 会话策略 | 需求方 | 待确认 |
| Q-5 | 是否需要「记住我」/ 自动续期功能？ | T-02、T-05 范围 | 需求方 | 待确认 |
| Q-6 | 登录/注册表单是否需要纳入首页 i18n 体系（当前首页已支持中英切换）？ | T-07 范围 | 需求方 | 待确认 |

> **开发前置条件**：Q-1 必须在开发启动前给出结论，因为它直接决定 T-05/T-06 的实现范围与工作量。
> 本 PRD 经确认后方可进入开发阶段。
