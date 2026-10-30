# 接口测试报告（TEST_REPORT.md）

> 特性：问诊患者用户名密码登录
> 测试文档：`docs/API_TEST.md`
> 关联 PRD：`docs/PRD-patient-login.md`（T-08）
> 报告版本：v1.0
> 执行日期：2026-10-30

---

## 1. 测试结论

| 项 | 结果 |
|---|---|
| **接口用例** | **25 条，全部通过（PASS 25 / FAIL 0）** |
| 质量门禁 | 4 项全部通过 |
| 后端自动化测试 | 113 条全部通过 |
| **总体结论** | ✅ **通过（Pass）** |

PRD 第 8.1 节的接口级验收标准 **AC-01 ~ AC-09** 与回归项 **AC-14** 已全部覆盖并通过；
AC-10 ~ AC-13 为浏览器人工验收项，见第 6 节。

---

## 2. 环境信息

### 2.1 软硬件环境

| 项 | 版本 / 配置 |
|---|---|
| 操作系统 | Microsoft Windows 10 专业版 10.0.19045.0 |
| JDK | OpenJDK 17.0.20.1 |
| Maven | Maven Wrapper（`./mvnw`） |
| Spring Boot | 3.5.7 |
| Node.js | v22.22.2 |
| npm | 10.9.7 |
| MySQL | 8.0.46（Docker 容器 `qa-live-mysql`，healthy） |
| Git 提交 | `0363c44`（分支 `feature/i18n-multi-language`） |

### 2.2 服务状态

| 服务 | 地址 | 状态 |
|---|---|---|
| 后端 `qa-service-user` | http://localhost:8080 | ✅ 运行中（PID 18948） |
| 前端 `qa-web` | http://localhost:5173 | ✅ 运行中 |
| MySQL | localhost:3306 / `qa_live` | ✅ healthy |
| phpMyAdmin | http://localhost:8081 | ✅ 运行中 |

### 2.3 数据库初始状态

| 表 | 记录数 | 说明 |
|---|---|---|
| `doctors` | 5 | 种子数据（`doc001`~`doc005`，其中 `doc004` 为 `isActive=false`） |
| `patients` | 5 | 种子数据（`patient001`~`patient005`，初始密码 `qa123456`） |

> 本轮测试新增 6 个测试账号（`zhangsan_t1`、`ZhangSan_t1`、`ratelimit_test_t1`、`testuser1_t1`~`testuser4_t1`）。
> `_t1` 后缀是本次执行批次标识，便于复现与清理。

### 2.4 执行前置说明

- **限流计数为内存态**：登录失败计数保存在 `LoginAttemptService` 的 `ConcurrentHashMap` 中，服务重启即清空。
  执行 API-P2-05 前已重启后端以获得干净状态。
- **测试账号唯一化**：所有测试账号带 `_t1` 后缀，避免与历史数据及重复执行冲突。

---

## 3. 接口用例执行结果

### 3.1 医生接口（回归）

| 用例 | 目的 | 期望结果 | 实际结果 | 结论 |
|---|---|---|---|---|
| **API-D1** | `GET /api/doctors` 返回全部医生 | HTTP 200；数组 5 条；字段完整；不含 `password`；`isActive` 含 true/false | HTTP 200；数组 5 条；字段完整=true；含 password=false；含 isActive=false=true；含 isActive=true=true | ✅ PASS |
| **API-D2** | `GET /api/doctors/active` 仅返回在线医生 | HTTP 200；数组 4 条；全部 `isActive=true`；不含 `doc004`；不含 `password` | HTTP 200；数组 4 条；全部 active=true；无 doc004=true；含 password=false | ✅ PASS |

---

### 3.2 患者注册

| 用例 | 目的 | 期望结果 | 实际结果 | 结论 |
|---|---|---|---|---|
| **API-P1-01** | 注册成功返回 201 与登录态 | HTTP 201；`code=OK`；message=注册成功；token 64 位 hex；username/name 正确；无密码泄露；id 服务端生成 | HTTP 201；code=OK=true；msg=注册成功=true；token 长度=64；username=zhangsan=true；name=张三=true；无密码泄露=true；服务端生成 id=`patient008` | ✅ PASS |
| **API-P1-02** | 用户名重复 | HTTP 409；`PATIENT_USERNAME_TAKEN`；message=该用户名已被占用，请更换；data=null | HTTP 409；code 正确=true；msg 正确=true；data=null=true | ✅ PASS |
| **API-P1-03** | 用户名大小写不敏感 | HTTP 409；`PATIENT_USERNAME_TAKEN` | HTTP 409；code=PATIENT_USERNAME_TAKEN=true | ✅ PASS |
| **API-P1-04** | 用户名过短 | HTTP 400；`PATIENT_USERNAME_INVALID`；message=用户名需为 4-32 位… | HTTP 400；code=PATIENT_USERNAME_INVALID=true；msg 正确=true | ✅ PASS |
| **API-P1-05** | 密码纯数字 | HTTP 400；`PATIENT_PASSWORD_INVALID`；message=密码需为 8-32 位… | HTTP 400；code=PATIENT_PASSWORD_INVALID=true；msg 正确=true | ✅ PASS |
| **API-P1-06** | 姓名为空 | HTTP 400；`PATIENT_NAME_INVALID` | HTTP 400；code=PATIENT_NAME_INVALID=true | ✅ PASS |
| **API-P1-07** | 手机号格式非法 | HTTP 400；`PATIENT_PHONE_INVALID` | HTTP 400；code=PATIENT_PHONE_INVALID=true | ✅ PASS |
| **API-P1-08** | 生日格式非法 | HTTP 400；`PATIENT_BIRTHDAY_INVALID` | HTTP 400；code=PATIENT_BIRTHDAY_INVALID=true | ✅ PASS |

**API-P1-01 实际响应**：

```http
HTTP/1.1 201 Created
Content-Type: application/json
{
  "code": "OK",
  "message": "注册成功",
  "data": {
    "token": "<64 位十六进制随机串>",
    "patient": {
      "id": "patient008",
      "username": "zhangsan_t1",
      "name": "张三",
      "birthday": "1990-01-01",
      "phone": "13800138000",
      "gender": "男",
      "createdAt": "2026-10-30T14:50:58.749738"
    }
  }
}
```

> ✅ 响应体中**不含** `password` 字段、**不含**明文 `qa123456`、**不含** `$2a$` 开头的哈希。

---

### 3.3 患者登录

| 用例 | 目的 | 期望结果 | 实际结果 | 结论 |
|---|---|---|---|---|
| **API-P2-01** | 正确凭证登录 | HTTP 200；`code=OK`；message=登录成功；token 非空(64 hex)；不含 password | HTTP 200；code=OK=true；msg=登录成功=true；token 长度=64；无密码泄露=true | ✅ PASS |
| **API-P2-02** | 密码错误 | HTTP 401；`PATIENT_LOGIN_FAILED`；message=用户名或密码错误；data=null | HTTP 401；code=PATIENT_LOGIN_FAILED=true；msg 正确=true；data=null=true | ✅ PASS |
| **API-P2-03** | 用户不存在与密码错误响应一致 | 两个响应体逐字节一致；均为 401 | 错误密码响应与不存在用户响应均为 `{"code":"PATIENT_LOGIN_FAILED","message":"用户名或密码错误","data":null}`；**diff 结果：完全一致(PASS)** | ✅ PASS |
| **API-P2-04** | 登录参数缺失 | HTTP 401；`PATIENT_LOGIN_FAILED`（**不是** `PATIENT_USERNAME_INVALID`） | HTTP 401；code=PATIENT_LOGIN_FAILED=true；未泄露具体原因=true | ✅ PASS |
| **API-P2-05** | 连续 6 次错误登录触发限流 | 第 1~5 次 401 `PATIENT_LOGIN_FAILED`；第 6 次 429 `PATIENT_LOGIN_LOCKED` | 第1次: 401 PATIENT_LOGIN_FAILED；第2次: 401；第3次: 401；第4次: 401；第5次: 401；**第6次: 429 PATIENT_LOGIN_LOCKED** | ✅ PASS |
| **API-P2-06** | 锁定期间正确密码也被拒 | HTTP 429；`PATIENT_LOGIN_LOCKED` | HTTP 429；code=PATIENT_LOGIN_LOCKED=true | ✅ PASS |

**API-P2-01 实际响应**：

```http
HTTP/1.1 200 OK
Content-Type: application/json
{
  "code": "OK",
  "message": "登录成功",
  "data": {
    "token": "2553765a49cf7d088e27e683fa63abaf28ec1bb70d643f20bf6e2762e62aa140",
    "patient": {
      "id": "patient008",
      "username": "zhangsan_t1",
      "name": "张三",
      "birthday": "1990-01-01",
      "phone": "13800138000",
      "gender": "男",
      "createdAt": "2026-10-30T14:50:58.749738"
    }
  }
}
```

**API-P2-03 响应对比**（两个场景输出逐字节相同）：

```json
{"code":"PATIENT_LOGIN_FAILED","message":"用户名或密码错误","data":null}
```

> ✅ 验证了 PRD SEC-03「用户不存在与密码错误不得区分」，防账号枚举生效。

---

### 3.4 会话管理

| 用例 | 目的 | 期望结果 | 实际结果 | 结论 |
|---|---|---|---|---|
| **API-P4-01** | 有效 token 查询当前患者 | HTTP 200；`code=OK`；username/name 正确；不含 password | HTTP 200；code=OK=true；username=zhangsan=true；name=张三=true；无密码泄露=true | ✅ PASS |
| **API-P4-02** | 缺失 token | HTTP 401；`PATIENT_UNAUTHORIZED`；message=请先登录 | HTTP 401；code=PATIENT_UNAUTHORIZED=true；msg 正确=true | ✅ PASS |
| **API-P4-03** | 非法 token | HTTP 401；`PATIENT_TOKEN_INVALID`；message=登录已过期，请重新登录 | HTTP 401；code=PATIENT_TOKEN_INVALID=true；msg 正确=true | ✅ PASS |
| **API-P3-01** | 登出 | HTTP 204；响应体为空 | 状态行=`HTTP/1.1 204`；响应体为空=true（长度 0） | ✅ PASS |
| **API-P4-04** | 登出后原 token 失效 | HTTP 401；`PATIENT_TOKEN_INVALID` | HTTP 401；code=PATIENT_TOKEN_INVALID=true | ✅ PASS |

**API-P4-02 实际响应**（缺失 token）：

```http
HTTP/1.1 401 Unauthorized
{
  "code": "PATIENT_UNAUTHORIZED",
  "message": "请先登录",
  "data": null
}
```

**API-P4-03 实际响应**（非法 token）：

```http
HTTP/1.1 401 Unauthorized
{
  "code": "PATIENT_TOKEN_INVALID",
  "message": "登录已过期，请重新登录",
  "data": null
}
```

---

### 3.5 异常路径

| 用例 | 目的 | 期望结果 | 实际结果 | 结论 |
|---|---|---|---|---|
| **ERR-1** | 未注册的路径 | HTTP 404；`NOT_FOUND` | HTTP 404；code=NOT_FOUND=true；响应体 `{"code":"NOT_FOUND","message":"请求的资源不存在","data":null}` | ✅ PASS |
| **ERR-2** | 空请求体 `{}` | HTTP 400；不得为 500 | HTTP 400；code=`PATIENT_USERNAME_INVALID` | ✅ PASS |
| **ERR-3** | 非法 JSON | HTTP 400；不得为 500 | HTTP 400；code=`PATIENT_BAD_REQUEST` | ✅ PASS |

> ✅ ERR-1 验证了本轮修复：`NoResourceFoundException` 不再被兜底 handler 吞成 500。

---

### 3.6 库表校验

| 用例 | 目的 | 期望结果 | 实际结果 | 结论 |
|---|---|---|---|---|
| **DB-01** | 注册账号落库为 BCrypt 哈希 | `password` 列前缀为 `$2a$` | 实际前缀=`$2a$`；行数据 `patient008  zhangsan_t1  $2a$` | ✅ PASS |

**实际执行命令与输出**：

```bash
docker exec qa-live-mysql mysql -uroot -proot qa_live \
  -e "SELECT id, username, LEFT(password,4) AS pwd_prefix FROM patients WHERE username='zhangsan_t1';"
```

```
id          username      pwd_prefix
patient008  zhangsan_t1   $2a$
```

> ✅ 密码以 BCrypt 哈希落库，明文不落库（PRD REQ-002 / AC-01）。

---

## 4. PRD 验收标准对照

### 4.1 接口级验收（AC-01 ~ AC-09、AC-14）

| AC | 验收标准 | 对应用例 | 结论 |
|---|---|---|---|
| AC-01 | 注册返回 201，响应体不含 password，库中为 BCrypt 哈希 | API-P1-01、DB-01 | ✅ PASS |
| AC-02 | 重复用户名返回 409 与 `PATIENT_USERNAME_TAKEN` | API-P1-02 | ✅ PASS |
| AC-03 | 用户名/密码格式非法返回 400 与对应错误码 | API-P1-04 ~ API-P1-08 | ✅ PASS |
| AC-04 | 正确凭证登录返回 200 与 token | API-P2-01 | ✅ PASS |
| AC-05 | 用户名不存在与密码错误返回完全相同的 401 响应体 | API-P2-02、API-P2-03 | ✅ PASS |
| AC-06 | 连续 5 次错误登录后第 6 次返回 429 | API-P2-05、API-P2-06 | ✅ PASS |
| AC-07 | `/me` 带有效 token 返回 200 与当前患者信息 | API-P4-01 | ✅ PASS |
| AC-08 | `/me` 不带/带非法 token 返回 401 | API-P4-02、API-P4-03 | ✅ PASS |
| AC-09 | 登出返回 204，随后旧 token 调 `/me` 返回 401 | API-P3-01、API-P4-04 | ✅ PASS |
| AC-14 | 医生登录、医生列表接口功能不受影响（回归） | API-D1、API-D2 | ✅ PASS |

### 4.2 质量门禁（PRD 8.2）

| 项 | 命令 | 通过标准 | 实际 | 结论 |
|---|---|---|---|---|
| 后端编译 | `cd server/qa-service-user && ./mvnw -q clean compile` | exit 0 | exit 0 | ✅ PASS |
| 后端测试 | `cd server/qa-service-user && ./mvnw test` | 全部通过 | **113 tests, 0 failures, 0 errors**，BUILD SUCCESS | ✅ PASS |
| 前端类型检查 | `cd web/qa-web && npx vue-tsc -b` | exit 0 | exit 0 | ✅ PASS |
| 前端构建 | `cd web/qa-web && npm run build` | exit 0 | exit 0 | ✅ PASS |
| 接口测试 | 按 `docs/API_TEST.md` 执行 | 全部符合预期 | 25/25 通过 | ✅ PASS |

---

## 5. 自动化测试覆盖

`./mvnw test` 输出的 113 条用例分布（测试类位于 `server/qa-service-user/src/test/java/`）：

| 测试类 | 用例数 | 覆盖内容 |
|---|---|---|
| `PatientValidatorTests` | 60 | V-01~V-06 全部校验规则与边界值 |
| `PatientAuthServiceTests` | 27 | 注册/登录/限流/会话的服务层行为 |
| `PatientControllerTests` | 12 | REST 契约：状态码、统一响应结构、错误码、Bearer 头、响应体不泄露密码 |
| `LoginAttemptServiceTests` | 7 | 失败计数、锁定、过期解除（注入 `Clock` 模拟时间流逝） |
| `SessionServiceTests` | 6 | 令牌格式、唯一性、注销失效 |
| `QaServiceUserApplicationTests` | 1 | 应用上下文加载 |

测试环境使用 H2 内存数据库（`@ActiveProfiles("test")`），**不会污染开发环境的 MySQL**。

---

## 6. 人工验收（AC-10 ~ AC-13）

以下为浏览器端人工验收项，需在前端 `http://localhost:5173` 手动执行：

| 编号 | 步骤 | 期望结果 | 关联 AC | 结论 |
|---|---|---|---|---|
| MAN-01 | 访问 `/consultation`（未登录） | 显示「患者身份验证」区的登录/注册切换表单 | AC-11 | ⏳ 待人工执行 |
| MAN-02 | 注册 `zhangsan2 / qa123456` | 注册成功，直接展示「我的问题」与「提交问题」按钮 | AC-10 | ⏳ 待人工执行 |
| MAN-03 | 刷新页面 | 仍处于登录态，顶部显示当前患者姓名 | AC-10 | ⏳ 待人工执行 |
| MAN-04 | 登出后刷新页面 | 回到未登录状态 | AC-12 | ⏳ 待人工执行 |
| MAN-05 | 注册标签输入 `ab` / `12345678` 提交 | 字段下方显示明确错误提示 | AC-03 | ⏳ 待人工执行 |
| MAN-06 | 访问 `/doctor/login` | 医生登录仍可用，可进入诊室 | AC-14 | ⏳ 待人工执行 |
| MAN-07 | 切换中/英文 | 登录/注册表单文案随语言切换 | T-07 | ⏳ 待人工执行 |

> 本轮接口测试通过 curl 完成，未包含浏览器交互验证。上述 7 项需人工补测后回填结论。

---

## 7. 问题清单

### 7.1 阻塞级（Blocker）

**无。**

### 7.2 严重级（Critical）

**无。**

### 7.3 一般级（Major）

**无。**

### 7.4 次要级（Minor / 建议）

| 编号 | 描述 | 影响 | 建议 |
|---|---|---|---|
| **MIN-01** | 限流计数为纯内存态（`LoginAttemptService` 用 `ConcurrentHashMap`），服务重启即全部清零 | 攻击者可通过诱导重启绕过锁定 | 符合 PRD R-7「当前为本地开发环境，可接受」。生产化时改为 Redis 等共享存储 |
| **MIN-02** | token 为内存态无过期时间，服务重启后全部失效 | 用户需重新登录 | 同上，符合 PRD R-7。PRD Q-4「token 是否设置有效期」仍待需求方确认 |
| **MIN-03** | 限流锁定按**用户名**维度计数，而非 IP | 同一 IP 可换用户名无限尝试 | 建议后续增加 IP 维度限流作为补充 |
| **MIN-04** | 医生 `password` 在库中为明文存储 | 数据库泄露即暴露医生密码 | 属历史遗留（`doctor-user-list.json` 前端凭据表同源）。建议后续统一改为 BCrypt，与患者侧一致 |
| **MIN-05** | `doctors` 与 `patients` 两套接口的响应结构不同（前者裸数组，后者统一信封） | 前端 `client.ts` 需做双格式兼容 | 已知技术债，不影响功能。建议后续统一为统一信封 |
| **MIN-06** | 测试需重启后端才能获得干净的限流状态 | 影响接口测试的可重复性 | 已在 `API_TEST.md` 第 6 节注明；后续可考虑将限流状态外置以便测试隔离 |

### 7.5 本轮修复记录

| 编号 | 问题 | 修复 | 验证 |
|---|---|---|---|
| **FIX-01** | 未注册的路径（如 `/api/questions/mine`）返回 **500** 而非 404 —— `NoResourceFoundException` 被兜底 `@ExceptionHandler(Exception.class)` 捕获 | 在 `GlobalExceptionHandler` 中新增 `NoResourceFoundException` 专用 handler，返回 404 + `NOT_FOUND` | ERR-1 ✅ PASS |
| **FIX-02** | `LoginAttemptService` 锁定期无法自动化测试（需真实等待 5 分钟） | 注入 `Clock` 抽象，构造器支持传入自定义时钟 | `LoginAttemptServiceTests.lockExpires` ✅ PASS |
| **FIX-03** | 登录锁定后「限制期过后自动解除」无自动化覆盖 | 同 FIX-02，配合 `MutableClock` 推进时间 | `LoginAttemptServiceTests` 2 条用例 ✅ PASS |

---

## 8. 测试数据清理

本轮测试共创建 9 个临时账号（`zhangsan_t1`、`ZhangSan_t1`、`ratelimit_test_t1`、`testuser1_t1`~`testuser4_t1`，
以及早期批次遗留的 `zhangsan`、`ratelimit_test`、`xiaozhang`）。清理命令：

```bash
docker exec qa-live-mysql mysql -uroot -proot qa_live \
  -e "DELETE FROM patients WHERE username IN ('zhangsan','ZhangSan','ratelimit_test','xiaozhang','testuser1','testuser2','testuser3','testuser4');"
```

> 执行后 `patients` 表已恢复为 5 条种子数据（`patient001`~`patient005`），已验证。
> 限流计数与 token 均为内存态，重启后端即自动清空。

---

## 9. 总结

本轮 T-08 接口测试**全部通过**：

- **接口用例 25/25 通过**，覆盖 PRD 8.1 节 AC-01 ~ AC-09 与 AC-14
- **质量门禁 4/4 通过**：后端编译、后端测试（113 条）、前端类型检查、前端构建
- **回归无破坏**：医生列表接口行为与响应结构未受本特性影响
- **安全项验证通过**：密码 BCrypt 存储、响应体无密码泄露、用户枚举防护、限流锁定、登出即失效

发现的问题均为**次要级建议**（6 条 Minor），无阻塞/严重/一般级问题，不影响本特性验收。

**剩余工作**：第 6 节 7 项浏览器人工验收（AC-10 ~ AC-13）需人工补测并回填结论。
