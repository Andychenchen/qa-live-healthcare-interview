#提示词

## P0 · 分支与文档骨架

```
先快速梳理一下这个项目目录结构和技术栈，并验证一下环境是否可用，不需要修改任何代码
##已有环境
##本机是 Windows 10 + WSL2，当前工作分支 `feature/i18n-multi-language
`。以下环境均已安装并验证通过，直接使用即可。
## 工具链

| 工具 | 版本 | 位置 / 说明 |
|---|---|---|
| JDK | 17.0.20.1 (Temurin) | `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`，系统 `JAVA_HOME` 已指向它 |
| Maven | 3.9.11 | 用各服务目录下的 `mvnw`（wrapper），**不需要**全局 mvn |
| Node | v22.22.2 | 全局可用 |
| npm | 10.9.7 | 全局可用 |
| Git | 2.56.0.windows.3 | `C:\Program Files\Git\cmd\git.exe`
| Docker | Engine 29.9.2 / Desktop 4.97.0 | WSL2 后端，`docker` 与 `docker compose` 可用 |
## 已做的加速/镜像配置（已生效，不要重复配置）
- Maven 中央仓库 → 阿里云镜像：`C:\Users\Andychen\.m2\settings.xml`
- 本地镜像已就绪：`mysql:8.0`、`phpmyadmin:latest`（docker-compose 直接写这两个名字即可，无需再 pull）
```

## P1 · 需求1：首页中英文切换

```
需求：为首页添加中英文切换能力。
1. 安装 vue-i18n@9，在 src/main.ts 注册；
2. 在 src/locales/ 目录新建 zh-CN.json 和 en-US.json，把 Home.vue 中所有中文文案（hero 区、统计卡片、开放诊室区）提取为语言 key，中英文都要写好；
3. 在页面右上角添加语言切换下拉菜单（选项：中文/English），切换后页面文案立即生效，选择结果存入 localStorage，刷新后保持上次选择；
4. 只处理首页本身，不要改其他页面。
完成后启动 npm run dev，自测中英切换和刷新保持。
```

## P2 · 需求2-A：数据库环境

```
需求：为项目添加数据库环境。在仓库根目录创建 docker-compose.yml：
- MySQL 8.0：库名 qa_live，端口 3306，root 密码 root，数据卷持久化，启动时自动建库；
- phpMyAdmin：端口 8081，连接上述 MySQL。
镜像直接使用 mysql:8.0 和 phpmyadmin:latest。执行 docker compose up -d 启动，验证 phpMyAdmin 可通过 http://localhost:8081 访问并能看到 qa_live 库。
```

## P3 · 需求2-B：医生数据迁移与 API

```
需求：把医生数据从前端 JSON 迁移到 MySQL，并在 qa-service-user 中提供 API。数据源：web/qa-web/src/data/doctor-user-list.json。
1. 在 server/qa-service-user 引入 spring-boot-starter-data-jpa 和 mysql-connector-j，application.properties 配置连接 docker-compose 中的 qa_live 库；
2. 创建 Doctor 实体和 Repository，字段与 JSON 完全一致（id、username、password、name、title、department、avatar、experience、specialties、isActive），specialties 用 JSON 字符串列存储；
3. 提供建表和种子数据初始化（data.sql 或启动时 Runner，数据来自上述 JSON 的全部记录）；
4. 实现 REST API：GET /api/doctors 返回全部医生列表；GET /api/doctors/active 返回 isActive=true 的医生。复用现有 CORS 配置；
5. 用 ./mvnw spring-boot:run 启动，用 curl 验证两个接口返回正确 JSON。
```

## P4 · 需求2-C：前端联调

```
需求：前端医生数据改为来自后端 API。
1. 在 web/qa-web 增加统一的 API 请求封装（原生 fetch 即可，base url 读取 .env 的 VITE_API_BASE_URL，默认 http://localhost:8080，.env.example 同步更新）；
2. 把 store/index.ts 中 doctors 的来源从本地 JSON 改为调用 GET /api/doctors 异步加载（保留 Doctor 类型；登录、getStatistics、首页开放诊室、医生页继续走 store，避免大改）；
3. 医生页与首页在数据加载中显示 loading 态，加载失败时显示错误提示和重试按钮；
4. 同时启动后端与前端完成联调自测：医生页列表、首页开放诊室、首页统计的医生数量均正确。
```

## P5 · 需求3-A：PRD 模版与登录 PRD

```
需求：为"问诊患者用户名密码登录"特性编写产品文档。
1. 在 /docs 创建 PRD 模版（PRD_TEMPLATE.md），包含：背景与目标、用户故事、功能需求（含注册/登录/登出、校验规则、异常提示）、接口定义、数据模型、非功能要求（密码安全等）、验收标准、任务拆分章节；
2. 严格按模版撰写该特性的 PRD（docs/PRD-patient-login.md）：患者使用用户名+密码注册和登录，登录后方可问诊；如需拆分，在 PRD 中列出实现步骤顺序；
3. PRD 产出后暂停，等我确认后再开始开发。
```

## P6 · 需求3-B：按 PRD 实现登录

```
按 /docs/PRD-patient-login.md 完成特性开发，严格按其中列出的拆分步骤逐个执行：
1. 后端：在 qa-service-user 中创建患者表（MySQL）、注册和登录接口，密码加密存储，登录成功返回用户信息；入参校验和错误码与 PRD 一致；
2. 前端：实现患者注册/登录页面，并在问诊入口集成登录态（未登录引导登录，登录后进入问诊），样式与现有页面一致；
3. 完成后用 curl 自测注册、登录（含错误密码场景）。
每完成一个拆分步骤暂停汇报，我确认后继续。
```

## P7 · 测试文档与测试报告

```
需求：编写接口测试文档并执行生成测试报告。
1. 创建 /docs/API_TEST.md：用 curl 覆盖以上需求中全部接口（GET /api/doctors、GET /api/doctors/active、患者注册、患者登录），每个用例含：目的、curl 命令、期望结果，并覆盖正常与异常场景（404、参数缺失、密码错误等），说明执行顺序和环境准备；
2. 严格按 API_TEST.md 逐条执行，将实际结果记录为 /docs/TEST_REPORT.md（含环境信息、每条用例通过/失败结论、问题清单）。
```


