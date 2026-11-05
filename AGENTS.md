# AGENTS.md — SmartAdmin 仓库总览

本文件是仓库级 agent 指令，说明三个子项目的关系、运行环境、跨项目契约和硬约束。进入具体项目前，先读对应目录下的 `AGENTS.md`：

- `smart-admin-api-java17-springboot3/AGENTS.md` — Java 17 / Spring Boot 3 后端
- `smart-admin-web-typescript/AGENTS.md` — Vue 3 + TypeScript PC 管理端
- `smart-app/AGENTS.md` — uni-app 移动端

## 1. 仓库定位与组成

SmartAdmin v3（当前代码版本 v3.31）是 1024 创新实验室开源的快速开发平台，包含 PC 管理端、移动端、Java 后端和 MySQL 初始化脚本。

```text
smart-admin/
  smart-admin-api-java17-springboot3/   # 后端（唯一维护版本）
  smart-admin-web-typescript/           # PC 管理端（唯一维护版本）
  smart-app/                            # uni-app 移动端
  数据库SQL脚本/                        # MySQL 初始化与版本增量脚本
  AGENTS.md                             # 本文件
```

**已删除、不要恢复或引用：**

- `smart-admin-api-java8-springboot2`
- `smart-admin-web-javascript`

仓库不再维护 Java 8 / Spring Boot 2 和 JavaScript 前端版本。新增功能只落到 Java 17 + TypeScript 两套。

## 2. 三个项目的职责与依赖

```text
smart-admin-web-typescript  ──HTTP──>  smart-admin-api-java17-springboot3  ──JDBC──>  MySQL
smart-app                  ──HTTP──>  smart-admin-api-java17-springboot3  ──Redis──>  Redis
```

- 后端是唯一数据权威，前端和移动端都不直接连数据库。
- PC 端和移动端共用同一套登录、权限、字典、文件、消息等接口。
- PC 端菜单/按钮权限由后端 `t_menu` 驱动；移动端当前没有消费 `menuList`，只做登录和接口调用。
- 数据库脚本是两端的共同基础设施，新增表/字段必须同步增量 SQL。

## 3. 当前开发环境（本机）

以下为本仓库当前这套服务器的实际配置，仅用于开发：

| 组件 | 版本/地址 | 说明 |
|---|---|---|
| OS | Ubuntu 24.04 | |
| JDK | 17.0.20.1 | 后端要求 Java 17 |
| Maven | 3.9.16 | 已配置阿里云镜像 |
| Node / npm | 24.22.1 / 11.19.1 | PC 端要求 Node >=18 |
| Docker | 29.9.2 | 需要 `sudo` 访问 socket |
| MySQL | `127.0.0.1:3306`，容器 `smart-admin-mysql` | mysql:8.0，库 `smart_admin_v3`，`root / SmartAdmin666` |
| Redis | `127.0.0.1:6379`，容器 `smart-admin-redis` | redis:7-alpine，database 1，无密码，AOF 开启 |
| MySQL 数据 | `/home/ubuntu/workspace/docker/mysql` | 宿主机挂载 |
| Redis 数据 | `/home/ubuntu/workspace/docker/redis` | 宿主机挂载 |
| 后端日志 | `/home/logs/smart-admin_v3/sa-admin/dev` | 运行用户必须可写 |
| 上传目录 | `/home/smart_admin_v3/upload/` | 运行用户必须可写 |
| 后端端口 | `1024` | context-path `/` |
| PC 前端端口 | `8081` | Vite，`0.0.0.0` |
| 移动端 H5 | 默认 `5173` | 无代理，直连 API |

默认登录账号：`admin / 123456`（种子数据，仅开发环境）。

## 4. 启动、停止与验证

### 4.1 基础设施

```bash
sudo docker ps
sudo docker start smart-admin-mysql smart-admin-redis
sudo docker logs -f smart-admin-mysql
```

MySQL/Redis 容器设置 `restart=unless-stopped`，Docker 开机自启，服务器重启后会自动恢复。

### 4.2 后端

```bash
cd smart-admin-api-java17-springboot3
mvn -B -ntp -DskipTests clean package

java -Xms256m -Xmx512m -jar sa-admin/target/sa-admin-dev-3.0.0.jar \
  --spring.profiles.active=dev
```

**必须带 `--spring.profiles.active=dev`。** 当前 `pom.xml` 的资源过滤有重叠，`@profiles.active@` 不会在打包时替换，直接 `java -jar` 会启动失败。详见后端 `AGENTS.md`。

### 4.3 PC 前端

```bash
cd smart-admin-web-typescript
npm install
npm run localhost
```

访问 `http://<服务器地址>:8081`。开发环境所有接口走 Vite 代理：

```text
浏览器 /api/login/...  ->  Vite  ->  后端 /login/...
```

浏览器 F12 里不会出现后端 1024 端口；后端地址只存在于 Vite 的 Node 侧配置 `API_PROXY_TARGET`。

### 4.4 移动端

```bash
cd smart-app
npm install
npm run dev:h5            # http://localhost:5173
npm run dev:mp-weixin     # 产物 dist/dev/mp-weixin
```

App 原生打包需要 HBuilderX，并先在 `src/manifest.json` 填写 DCloud `appid`。

### 4.5 验证

```bash
# 后端存活
curl -s http://127.0.0.1:1024/login/getCaptcha

# 前端页面
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/

# 前端代理（应返回后端 JSON）
curl -s http://127.0.0.1:8081/api/login/getCaptcha

# 端口
ss -ltnp | grep -E ':(1024|8081)\b'
```

## 5. 跨项目契约（不要单方面修改）

### 5.1 统一响应

```json
{
  "code": 0,
  "level": null,
  "msg": "操作成功",
  "ok": true,
  "data": {},
  "dataType": 1
}
```

- 成功码是 `0`，不是 `1`。
- 所有业务错误也返回 HTTP 200，由 `code` 区分。
- `dataType=10` 表示 `data` 是 SM4 加密字符串，前端需要解密。
- 错误码范围：`10001-20000` 系统，`20001-30000` 未预期，`30001-40000` 用户。
- PC 前端和移动端都用 `if (res.code && res.code !== 1)` 兼容成功码 0；不要改成 `code === 1`。

### 5.2 认证

- 请求头：`Authorization: Bearer <token>`。
- 后端 Sa-Token：`token-name=Authorization`、`token-prefix=Bearer`。
- PC token 存储 key：`smart_admin_user_token`。
- 移动端 token 存储 key：`smart_h5_token`。
- 两端登录态不共享；`sa-token.is-concurrent=false`，同账号新登录会踢掉旧 token。
- 登录密码必须 SM4 加密后传输，密钥 `1024lab__1024lab`；改一端会导致登录失败。

### 5.3 分页

请求：

```json
{ "pageNum": 1, "pageSize": 10, "searchCount": true }
```

- `pageNum` 从 1 开始，`pageSize` 最大 500。
- 响应字段：`pageNum / pageSize / total / pages / list / emptyFlag`。
- 后端用 `PageParam` / `PageResult`，PC 端用 `PageParamModel` / `PageResultModel`，移动端直接读 `res.data.list` / `res.data.pages`。

### 5.4 数据格式

- 日期：`yyyy-MM-dd HH:mm:ss`，时区 GMT+8。
- `BigDecimal` 序列化为字符串；金额不要直接做 JS 数值运算。
- `Long` 超出 JS 安全整数时序列化为字符串；ID 可能是 number 或 string。
- 文件 URL 在 dev 环境是相对路径 `/upload/...`，通过前端同源代理访问。

## 6. 数据库与脚本

- 数据库：MySQL 8，库名 `smart_admin_v3`，字符集 `utf8mb4`，表统一 `t_` 前缀。
- 首次初始化：`数据库SQL脚本/mysql/smart_admin_v3.sql`。
- 版本升级：`数据库SQL脚本/mysql/sql-update-log/` 下的增量脚本，按版本从小到大执行。
- 新增表/字段必须补增量脚本；不要只改初始 SQL。
- 后端仅支持 MySQL：分页方言、Druid `DbType.MYSQL`、`DATE_FORMAT`、代码生成器 `information_schema` 都是 MySQL 专用。PostgreSQL 属于官方商业版能力，开源仓库没有现成适配。
- 当前 SQL 共 44 张表。

## 7. 已知问题与当前决策

1. **后端 profile 占位符未过滤**：`application.yaml` 中的 `@profiles.active@` 打包后仍是字面量；启动必须带 `--spring.profiles.active=dev`。优先修 `pom.xml` 资源规则，不要删启动参数了事。
2. **PC 前端代理已修正**：原来的 `server.server.proxy` 是错误层级，现已改为 `server.proxy`，并让 axios 在开发环境使用 `/api`。不要改回直连 `http://127.0.0.1:1024`。
3. **文件 URL 已改为相对路径**：`file.storage.local.url-prefix: /upload/`，避免浏览器暴露后端端口。
4. **种子头像文件缺失**：`t_employee.avatar` 有值，但图片文件没有随仓库提供，登录后头像可能 404；不是代理问题。
5. **生产环境 API 地址**：PC 端 `.env.production` / `.env.pre` 默认指向演示环境 `https://preview.smartadmin.vip/smart-admin-api`；移动端指向 `https://app.smartadmin.vip/smart-app-api`。真实部署前必须改。
6. **移动端与后端存在契约偏差**：字典接口、更新日志字段、企业 logo、商品产地、反馈附件、H5 登录设备值等；详见 `smart-app/AGENTS.md`。
7. **数据权限未启用**：后端 `@DataScope` 是预埋能力，当前没有业务方法使用，不要假设数据已被隔离。
8. **万能密码默认开启**：`t_config.super_password = 1024ok`，生产必须修改或移除。
9. **本地文件 private 目录不私有**：本地存储的 `/upload/private/**` 可通过 URL 直接访问；真正私有需要云存储预签名。
10. **测试很少**：后端只有一个没有 `@Test` 的测试类；PC 端和移动端没有测试。构建成功不等于业务正确。

## 8. 跨项目硬约束

1. 不要恢复已删除的 Java 8 / JavaScript 版本，也不要把它们的写法带回来。
2. 后端新代码使用 `jakarta.*`、Java 17 语法；不要引入 `javax.servlet/validation`。
3. 数据库只按 MySQL 维护；不要在未做系统性移植的情况下声称支持 PostgreSQL。
4. 新增接口必须考虑 PC 端和移动端契约：响应包装、分页字段、token 头、加密字段、日期/金额格式。
5. PC 端业务路由由后端菜单生成，不要写进前端静态路由。
6. 移动端新页面必须注册 `pages.json`，tab 页必须用 `switchTab`。
7. 权限字符串必须前后端同时配置：后端 `@SaCheckPermission`、`t_menu.api_perms`、前端 `t_menu.web_perms` / `v-privilege`。
8. 不要提交 `target/`、`node_modules/`、`dist/`、`unpackage/`、日志、上传文件、IDE 配置。
9. 不要修改 SM4 密钥、密码盐格式、loginId 格式、token 头名；它们是跨端硬约定。
10. 不要用构建成功代替验证；涉及登录、权限、分页、文件、加密的改动必须实际调用接口验证。

## 9. 常见任务入口

| 任务 | 去看 |
|---|---|
| 新增后端业务模块 | `smart-admin-api-java17-springboot3/AGENTS.md` §10 |
| 新增 PC CRUD 页面 | `smart-admin-web-typescript/AGENTS.md` §8.4 |
| 新增移动端页面 | `smart-app/AGENTS.md` §4、§7 |
| 修改登录/权限 | 后端 §6 + PC §5 + app §6 |
| 修改接口契约 | 根 §5 + 各项目对应章节 |
| 修改数据库 | 根 §6；同步初始化脚本和 `sql-update-log` |
| 排查启动失败 | 后端 §4.3；先看 profile 参数、日志目录、`t_config` |
| 排查前端接口 404 | PC §3.1；确认 `/api` 前缀和 Vite 代理 |

## 10. 验证总清单

- `sudo docker ps`：MySQL、Redis 均为 Up。
- `curl http://127.0.0.1:1024/login/getCaptcha`：返回 `code=0` 和验证码 JSON。
- `curl http://127.0.0.1:8081/api/login/getCaptcha`：同样返回后端 JSON，说明代理正常。
- 浏览器打开 8081，用 `admin / 123456` 登录，能看到菜单和首页。
- F12 Network 中接口地址是 `http://<前端地址>:8081/api/...`，没有 1024 端口。
- 后端日志无启动异常；上传目录和日志目录可写。
- 移动端 `npm run dev:h5` 能打开 5173，登录接口可通（依赖后端 CORS dev 配置）。