# 接口测试文档（API_TEST.md）

> 特性：问诊患者用户名密码登录
> 关联 PRD：`docs/PRD-patient-login.md`
> 关联任务：T-08 前后端联调与接口测试文档
> 适用服务：`qa-service-user`（Spring Boot 3.5.7 / Java 17）
> 文档版本：v1.0

---

## 1. 测试范围

本文档使用 `curl` 覆盖本特性涉及的**全部接口**，包含医生接口（回归）与患者认证接口（新增）。

| 编号 | 方法 | 路径 | 认证 | 覆盖场景 |
|---|---|---|---|---|
| API-D1 | GET | `/api/doctors` | 否 | 正常 / 回归 |
| API-D2 | GET | `/api/doctors/active` | 否 | 正常 / 回归 |
| API-P1 | POST | `/api/patients/register` | 否 | 正常 / 参数缺失 / 格式非法 / 用户名重复 |
| API-P2 | POST | `/api/patients/login` | 否 | 正常 / 密码错误 / 用户不存在 / 参数缺失 / 限流锁定 |
| API-P3 | POST | `/api/patients/logout` | 是 | 正常（204） |
| API-P4 | GET | `/api/patients/me` | 是 | 有效 token / 缺失 token / 非法 token / 已登出 token |

同时覆盖**异常路径**：

| 编号 | 场景 | 期望状态码 |
|---|---|---|
| ERR-1 | 未注册的路径 | 404 |
| ERR-2 | 空请求体 | 400 |
| ERR-3 | 非法 JSON 请求体 | 400 |

---

## 2. 环境准备

### 2.1 前置条件

| 项 | 要求 | 校验命令 |
|---|---|---|
| JDK | 17+ | `java -version` |
| Node.js | 22（18+ 即可） | `node -v` |
| Docker | 已在运行 | `docker ps` |
| MySQL | 容器 `qa-live-mysql` 运行中，端口 3306 | `docker ps --filter name=qa-live-mysql` |

### 2.2 启动数据库

```bash
# 在仓库根目录执行
docker compose up -d
docker ps --filter name=qa-live-mysql
```

### 2.3 启动后端

```bash
cd server/qa-service-user
./mvnw spring-boot:run
```

等待日志出现 `Started QaServiceUserApplication` 后确认端口：

```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/doctors
# 期望：200
```

### 2.4 启动前端（联调/人工验收需要）

```bash
cd web/qa-web
npm run dev
# 默认 http://localhost:5173
```

### 2.5 种子数据说明

后端启动时若 `patients` / `doctors` 表为空，会自动写入种子数据：

| 表 | 记录数 | 用户名 | 初始密码 |
|---|---|---|---|
| `patients` | 5 | `patient001` ~ `patient005` | `qa123456`（BCrypt 哈希存储，明文不落库） |
| `doctors` | 5 | `doc001` ~ `doc005` | `123456`（历史遗留，明文存储，仅前端登录校验用） |

> 医生数据中 `doc004` 的 `isActive=false`，因此 `/api/doctors` 返回 5 条，`/api/doctors/active` 返回 4 条。

### 2.6 通用约定

- **基础地址**：`http://localhost:8080`
- **请求头**：`Content-Type: application/json; charset=UTF-8`
- **认证方式**：`Authorization: Bearer <token>`
- **统一响应结构**（患者接口）：

```json
{ "code": "OK", "message": "success", "data": { } }
```

- **医生接口**直接返回裸数组（历史设计，未套统一响应结构），前端 `client.ts` 已做兼容处理。

### 2.7 PowerShell / CMD 注意事项

Windows 下直接用 `curl -d '{...}'` 会因控制台编码问题破坏中文与引号。
**统一做法**：把 JSON 写入 UTF-8 文件后用 `--data-binary "@文件"` 传入。

```bash
# PowerShell 中创建请求体文件（UTF-8 无 BOM）
@'
{"username":"zhangsan","password":"qa123456","name":"张三"}
'@ | Out-File -Encoding utf8 req.json
```

---

## 3. 执行顺序

用例之间存在依赖，**必须按下列顺序执行**：

```
阶段一：医生接口回归（无状态，可任意顺序）
  API-D1 → API-D2

阶段二：注册（API-P1-01 会创建 zhangsan，后续用例依赖它）
  API-P1-01 → API-P1-02 → API-P1-03 → API-P1-04 → API-P1-05
  → API-P1-06 → API-P1-07 → API-P1-08

阶段三：登录（依赖 zhangsan 已存在）
  API-P2-01 → API-P2-02 → API-P2-03 → API-P2-04

阶段四：会话（依赖 API-P2-01 返回的 token）
  API-P4-01 → API-P4-02 → API-P4-03 → API-P3-01 → API-P4-04

阶段五：限流（用独立账号，不影响前面的用例）
  API-P2-05 → API-P2-06

阶段六：异常路径（无状态）
  ERR-1 → ERR-2 → ERR-3
```

> **顺序敏感点**
> - API-P1-02（用户名重复）依赖 API-P1-01 已成功创建 `zhangsan`
> - API-P2-02（密码错误）会把 `zhangsan` 的失败计数 +1，因此 **API-P2-05 限流用例必须使用另一个账号**（`ratelimit_test`）
> - API-P4-04（登出后 token 失效）必须在 API-P3-01 登出之后执行
> - API-P4-03（非法 token）使用伪造 token，不影响真实会话

---

## 4. 医生接口用例（回归）

### API-D1 获取全部医生

**目的**：验证医生列表接口功能正常，数据来自 MySQL 且结构完整（AC-14）。

**执行**：

```bash
curl -s -i http://localhost:8080/api/doctors
```

**期望结果**：

- 状态码 `200`
- 响应体为 JSON 数组，元素数 `5`
- 每个元素含 `id`、`username`、`name`、`title`、`department`、`avatar`、`experience`、`specialties`、`isActive`
- **不含** `password` 字段
- `isActive` 的 `false` 与 `true` 均有（`doc004` 为 `false`）

---

### API-D2 获取在线医生

**目的**：验证在线医生过滤接口正常，仅返回 `isActive=true` 的记录（AC-14）。

**执行**：

```bash
curl -s -i http://localhost:8080/api/doctors/active
```

**期望结果**：

- 状态码 `200`
- 响应体为 JSON 数组，元素数 `4`
- 所有元素 `isActive` 均为 `true`
- **不含** `doc004`
- **不含** `password` 字段

---

## 5. 患者注册用例

### API-P1-01 注册成功

**目的**：验证注册成功返回 201 与登录态，响应体不含 password（AC-01）。

**准备**：

```bash
@'
{"username":"zhangsan","password":"qa123456","name":"张三","birthday":"1990-01-01","phone":"13800138000","gender":"男"}
'@ | Out-File -Encoding utf8 req_register.json
```

**执行**：

```bash
curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_register.json"
```

**期望结果**：

- 状态码 `201`
- `code` 为 `OK`，`message` 为「注册成功」
- `data.token` 为非空字符串（64 位十六进制）
- `data.patient.username` 为 `zhangsan`，`data.patient.name` 为「张三」
- `data.patient.id` 由服务端生成（形如 `patient006`），**不使用请求体中的值**
- 响应体中**不出现** `password` 字段，也**不出现**明文 `qa123456` 或 `$2a$` 开头的哈希

**库表校验**（可选，确认落库为 BCrypt 哈希）：

```bash
docker exec qa-live-mysql mysql -uroot -proot qa_live \
  -e "SELECT id, username, LEFT(password, 4) AS pwd_prefix FROM patients WHERE username='zhangsan';"
```

期望 `pwd_prefix` 为 `$2a$`。

---

### API-P1-02 用户名重复

**目的**：验证重复用户名返回 409 与 `PATIENT_USERNAME_TAKEN`（AC-02）。

**执行**（复用 API-P1-01 的请求体文件）：

```bash
curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_register.json"
```

**期望结果**：

- 状态码 `409`
- `code` 为 `PATIENT_USERNAME_TAKEN`
- `message` 为「该用户名已被占用，请更换」
- `data` 为 `null`

---

### API-P1-03 用户名大小写不敏感

**目的**：验证 `ZhangSan` 与 `zhangsan` 被视为同一账号（V-01 / PRD 5.3.1）。

**执行**：

```bash
@'
{"username":"ZhangSan","password":"qa123456","name":"张三"}
'@ | Out-File -Encoding utf8 req_dup_case.json

curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_dup_case.json"
```

**期望结果**：

- 状态码 `409`
- `code` 为 `PATIENT_USERNAME_TAKEN`

---

### API-P1-04 用户名格式非法（过短）

**目的**：验证用户名长度校验（AC-03 / V-01）。

**执行**：

```bash
@'
{"username":"ab","password":"qa123456","name":"张三"}
'@ | Out-File -Encoding utf8 req_bad_user.json

curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_bad_user.json"
```

**期望结果**：

- 状态码 `400`
- `code` 为 `PATIENT_USERNAME_INVALID`
- `message` 为「用户名需为 4-32 位，仅支持字母、数字、下划线和连字符」

---

### API-P1-05 密码格式非法（纯数字）

**目的**：验证密码必须同时含字母和数字（AC-03 / V-02）。

**执行**：

```bash
@'
{"username":"testuser1","password":"12345678","name":"张三"}
'@ | Out-File -Encoding utf8 req_bad_pwd.json

curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_bad_pwd.json"
```

**期望结果**：

- 状态码 `400`
- `code` 为 `PATIENT_PASSWORD_INVALID`
- `message` 为「密码需为 8-32 位，且必须同时包含字母和数字」

---

### API-P1-06 姓名为空

**目的**：验证姓名必填校验（V-03）。

**执行**：

```bash
@'
{"username":"testuser2","password":"qa123456","name":""}
'@ | Out-File -Encoding utf8 req_empty_name.json

curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_empty_name.json"
```

**期望结果**：

- 状态码 `400`
- `code` 为 `PATIENT_NAME_INVALID`

---

### API-P1-07 手机号格式非法

**目的**：验证手机号必须匹配 `^1[3-9]\d{9}$`（V-05）。

**执行**：

```bash
@'
{"username":"testuser3","password":"qa123456","name":"张三","phone":"123"}
'@ | Out-File -Encoding utf8 req_bad_phone.json

curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_bad_phone.json"
```

**期望结果**：

- 状态码 `400`
- `code` 为 `PATIENT_PHONE_INVALID`

---

### API-P1-08 生日格式非法

**目的**：验证生日必须为 `YYYY-MM-DD` 且不晚于当天（V-04）。

**执行**：

```bash
@'
{"username":"testuser4","password":"qa123456","name":"张三","birthday":"1990/01/01"}
'@ | Out-File -Encoding utf8 req_bad_birthday.json

curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_bad_birthday.json"
```

**期望结果**：

- 状态码 `400`
- `code` 为 `PATIENT_BIRTHDAY_INVALID`

---

## 6. 患者登录用例

### API-P2-01 登录成功

**目的**：验证正确凭证登录返回 200 与 token（AC-04）。

**执行**：

```bash
@'
{"username":"zhangsan","password":"qa123456"}
'@ | Out-File -Encoding utf8 req_login.json

curl -s -i -X POST http://localhost:8080/api/patients/login \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_login.json"
```

**期望结果**：

- 状态码 `200`
- `code` 为 `OK`，`message` 为「登录成功」
- `data.token` 为非空字符串
- `data.patient.username` 为 `zhangsan`
- 响应体**不含** `password`

**提取 token 供后续用例使用**：

```bash
# Git Bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/patients/login \
  -H "Content-Type: application/json" \
  --data-binary "@req_login.json" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
echo "$TOKEN"
```

```powershell
# PowerShell
$TOKEN = (curl.exe -s -X POST http://localhost:8080/api/patients/login `
  -H "Content-Type: application/json" --data-binary "@req_login.json" |
  ConvertFrom-Json).data.token
$TOKEN
```

---

### API-P2-02 密码错误

**目的**：验证密码错误返回 401 与 `PATIENT_LOGIN_FAILED`（AC-04）。

**执行**：

```bash
@'
{"username":"zhangsan","password":"wrongpassword"}
'@ | Out-File -Encoding utf8 req_login_wrong.json

curl -s -i -X POST http://localhost:8080/api/patients/login \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_login_wrong.json"
```

**期望结果**：

- 状态码 `401`
- `code` 为 `PATIENT_LOGIN_FAILED`
- `message` 为「用户名或密码错误」
- `data` 为 `null`

---

### API-P2-03 用户不存在（响应必须与密码错误完全一致）

**目的**：验证不存在用户与密码错误返回**逐字节相同**的响应体，防止账号枚举（AC-05 / SEC-03）。

**执行**：

```bash
@'
{"username":"nonexistent_user","password":"qa123456"}
'@ | Out-File -Encoding utf8 req_login_noexist.json

# 分别抓取两个场景的响应体（去掉状态行与响应头）
curl -s -X POST http://localhost:8080/api/patients/login \
  -H "Content-Type: application/json" --data-binary "@req_login_wrong.json" > resp_wrong.json

curl -s -X POST http://localhost:8080/api/patients/login \
  -H "Content-Type: application/json" --data-binary "@req_login_noexist.json" > resp_noexist.json

# 对比两个响应体
diff resp_wrong.json resp_noexist.json && echo "PASS: 响应体完全一致"
```

**期望结果**：

- 两个响应体 `diff` 无差异，输出 `PASS: 响应体完全一致`
- 两者 `code` 均为 `PATIENT_LOGIN_FAILED`，`message` 均为「用户名或密码错误」

---

### API-P2-04 登录参数缺失

**目的**：验证用户名为空时统一返回登录失败，不泄露具体原因（SEC-03）。

**执行**：

```bash
@'
{"username":"","password":"qa123456"}
'@ | Out-File -Encoding utf8 req_login_empty.json

curl -s -i -X POST http://localhost:8080/api/patients/login \
  -H "Content-Type: application/json; charset=UTF-8" \
  --data-binary "@req_login_empty.json"
```

**期望结果**：

- 状态码 `401`
- `code` 为 `PATIENT_LOGIN_FAILED`（**不是** `PATIENT_USERNAME_INVALID`）
- `message` 为「用户名或密码错误」

---

### API-P2-05 连续错误登录触发限流

**目的**：验证连续 5 次失败后，第 6 次返回 429（AC-06 / V-09）。

> 使用**独立账号** `ratelimit_test`，避免影响 `zhangsan` 的失败计数。

**准备**：先注册该账号（正常路径）：

```bash
@'
{"username":"ratelimit_test","password":"qa123456","name":"限流测试"}
'@ | Out-File -Encoding utf8 req_rl_register.json

curl -s -o /dev/null -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json" --data-binary "@req_rl_register.json"
```

**执行**（连续 6 次错误登录）：

**执行脚本**（Git Bash / Linux / macOS）：

```bash
for i in 1 2 3 4 5 6; do
  printf "第 %d 次: " "$i"
  curl -s -X POST http://localhost:8080/api/patients/login \
    -H "Content-Type: application/json" \
    -d '{"username":"ratelimit_test","password":"wrongpassword"}' \
    -w " [HTTP %{http_code}]\n"
done
```

**执行脚本**（PowerShell）：

```powershell
1..6 | ForEach-Object {
  $body = '{"username":"ratelimit_test","password":"wrongpassword"}'
  $r = curl.exe -s -X POST http://localhost:8080/api/patients/login `
    -H "Content-Type: application/json" -d $body -w "`nHTTP %{http_code}"
  Write-Output "第 $_ 次: $r"
}
```

**期望结果**：

| 次序 | 状态码 | `code` |
|---|---|---|
| 第 1 ~ 5 次 | `401` | `PATIENT_LOGIN_FAILED` |
| 第 6 次 | `429` | `PATIENT_LOGIN_LOCKED` |

第 6 次的 `message` 应为「登录尝试过于频繁，请 5 分钟后再试」。

---

### API-P2-06 锁定期间即使密码正确也被拒绝

**目的**：验证账号锁定期间正确密码同样被拒绝（AC-06）。

**执行**（紧接 API-P2-05，此时 `ratelimit_test` 处于锁定状态）：

```powershell
$body = '{"username":"ratelimit_test","password":"qa123456"}'
curl.exe -s -i -X POST http://localhost:8080/api/patients/login `
  -H "Content-Type: application/json" -d $body
```

**期望结果**：

- 状态码 `429`
- `code` 为 `PATIENT_LOGIN_LOCKED`

> **注意**：锁定期为 5 分钟。如需重新执行该用例，请重启后端（内存态计数会清空）或等待 5 分钟。

---

## 7. 会话管理用例

### API-P4-01 当前患者（有效 token）

**目的**：验证带有效 token 查询当前患者成功（AC-07）。

**执行**：

```bash
curl -s -i http://localhost:8080/api/patients/me \
  -H "Authorization: Bearer $TOKEN"
```

**期望结果**：

- 状态码 `200`
- `code` 为 `OK`
- `data.username` 为 `zhangsan`，`data.name` 为「张三」
- `data` 中**不含** `password` 字段

---

### API-P4-02 当前患者（缺失 token）

**目的**：验证未携带认证头返回 401（AC-08）。

**执行**：

```bash
curl -s -i http://localhost:8080/api/patients/me
```

**期望结果**：

- 状态码 `401`
- `code` 为 `PATIENT_UNAUTHORIZED`
- `message` 为「请先登录」

---

### API-P4-03 当前患者（非法 token）

**目的**：验证伪造 token 返回 401（AC-08）。

**执行**：

```bash
curl -s -i http://localhost:8080/api/patients/me \
  -H "Authorization: Bearer invalid-token-abc123"
```

**期望结果**：

- 状态码 `401`
- `code` 为 `PATIENT_TOKEN_INVALID`
- `message` 为「登录已过期，请重新登录」

---

### API-P3-01 登出

**目的**：验证登出返回 204 且令牌立即失效（AC-09）。

**执行**：

```bash
curl -s -i -X POST http://localhost:8080/api/patients/logout \
  -H "Authorization: Bearer $TOKEN"
```

**期望结果**：

- 状态码 `204`
- 响应体为空

---

### API-P4-04 登出后原 token 失效

**目的**：验证登出后旧 token 调用 `/me` 返回 401（AC-09 / SEC-05）。

**执行**（紧接 API-P3-01）：

```bash
curl -s -i http://localhost:8080/api/patients/me \
  -H "Authorization: Bearer $TOKEN"
```

**期望结果**：

- 状态码 `401`
- `code` 为 `PATIENT_TOKEN_INVALID`

---

## 8. 异常路径用例

### ERR-1 未注册的路径返回 404

**目的**：验证未匹配到任何接口的请求返回 404 而非 500。

**执行**：

```bash
curl -s -i http://localhost:8080/api/nonexistent-endpoint
```

**期望结果**：

- 状态码 `404`
- `code` 为 `NOT_FOUND`
- `message` 为「请求的资源不存在」

---

### ERR-2 空请求体返回 400

**目的**：验证注册/登录接口在请求体为空时返回 400 而非 500。

**执行**：

```bash
curl -s -i -X POST http://localhost:8080/api/patients/register \
  -H "Content-Type: application/json" \
  --data-binary "{}"
```

**期望结果**：

- 状态码 `400`
- `code` 为 `PATIENT_BAD_REQUEST` 或 `PATIENT_USERNAME_INVALID`（取决于校验顺序）
- **不得**为 500

---

### ERR-3 非法 JSON 返回 400

**目的**：验证请求体不是合法 JSON 时返回 400 而非 500。

**执行**：

```bash
curl -s -i -X POST http://localhost:8080/api/patients/login \
  -H "Content-Type: application/json" \
  --data-binary "{invalid json}"
```

**期望结果**：

- 状态码 `400`
- **不得**为 500

---

## 9. 人工验收（浏览器）

以下用例需在浏览器中执行，记录于 `docs/TEST_REPORT.md` 的「人工验收」章节。

| 编号 | 步骤 | 期望结果 | 关联 AC |
|---|---|---|---|
| MAN-01 | 访问 `http://localhost:5173/consultation`（未登录） | 显示「患者身份验证」区的登录/注册切换表单 | AC-11 |
| MAN-02 | 注册 `zhangsan2 / qa123456` | 注册成功，直接展示「我的问题」与「提交问题」按钮 | AC-10 |
| MAN-03 | 刷新页面 | 仍处于登录态，顶部显示当前患者姓名 | AC-10 |
| MAN-04 | 登出后刷新页面 | 回到未登录状态 | AC-12 |
| MAN-05 | 切到「注册」标签，输入用户名 `ab`、密码 `12345678` 提交 | 字段下方显示明确错误提示 | AC-03 |
| MAN-06 | 访问 `http://localhost:5173/doctor/login` | 医生登录仍可用，可进入诊室 | AC-14 |
| MAN-07 | 切换页面语言（中/英） | 登录/注册表单文案随语言切换 | T-07 |

---

## 10. 质量门禁

| 项 | 命令 | 通过标准 |
|---|---|---|
| 后端编译 | `cd server/qa-service-user && ./mvnw -q clean compile` | exit 0 |
| 后端测试 | `cd server/qa-service-user && ./mvnw test` | 全部通过 |
| 前端类型检查 | `cd web/qa-web && npx vue-tsc -b` | exit 0 |
| 前端构建 | `cd web/qa-web && npm run build` | exit 0 |
| 接口测试 | 按本文档第 3 节顺序执行 | 全部符合预期 |

---

## 11. 清理

测试产生的临时账号与文件：

```bash
# 删除测试账号
docker exec qa-live-mysql mysql -uroot -proot qa_live \
  -e "DELETE FROM patients WHERE username IN ('zhangsan','ZhangSan','ratelimit_test','testuser1','testuser2','testuser3','testuser4');"

# 删除临时请求体文件
rm -f req_*.json resp_*.json
```

---

## 12. 附录：错误码速查

| 错误码 | 状态码 | 含义 |
|---|---|---|
| `OK` | 200 / 201 | 成功 |
| `PATIENT_BAD_REQUEST` | 400 | 请求参数不合法 |
| `PATIENT_USERNAME_INVALID` | 400 | 用户名格式不合法 |
| `PATIENT_PASSWORD_INVALID` | 400 | 密码格式不合法 |
| `PATIENT_NAME_INVALID` | 400 | 姓名不合法 |
| `PATIENT_BIRTHDAY_INVALID` | 400 | 生日格式不合法 |
| `PATIENT_PHONE_INVALID` | 400 | 手机号格式不合法 |
| `PATIENT_GENDER_INVALID` | 400 | 性别取值不合法 |
| `PATIENT_USERNAME_TAKEN` | 409 | 用户名已被占用 |
| `PATIENT_LOGIN_FAILED` | 401 | 用户名或密码错误（含用户不存在） |
| `PATIENT_UNAUTHORIZED` | 401 | 未携带认证信息 |
| `PATIENT_TOKEN_INVALID` | 401 | 令牌无效或已失效 |
| `PATIENT_LOGIN_LOCKED` | 429 | 登录尝试过于频繁，已锁定 |
| `NOT_FOUND` | 404 | 请求的资源不存在 |
| `PATIENT_INTERNAL_ERROR` | 500 | 服务端异常 |
