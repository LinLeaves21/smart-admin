# SmartAdmin

SmartAdmin 是一个前后端分离的后台管理系统，仓库包含 Java 后端、PC 管理端和 uni-app 移动端三个部分。

## 项目结构

```text
.
├── smart-admin-api-java17-springboot3/   # Java 17 / Spring Boot 3 后端
├── smart-admin-web-typescript/           # Vue 3 + TypeScript PC 管理端
├── smart-app/                            # uni-app 移动端
├── 数据库SQL脚本/                        # MySQL 初始化与增量脚本
└── AGENTS.md                             # 仓库级开发说明
```

各子项目的详细开发约定见对应目录下的 `AGENTS.md`。

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis-Plus、Sa-Token、Redis、MySQL 8
- PC 管理端：Vue 3、TypeScript、Vite、Pinia、Ant Design Vue
- 移动端：uni-app、Vue 3、uni-ui（支持 H5、微信小程序、App）
- 数据库：MySQL 8（当前仅提供 MySQL 脚本）

## 环境要求

| 组件 | 版本 |
|---|---|
| JDK | 17 |
| Maven | 3.9+ |
| Node.js | 18+（PC 端与移动端） |
| MySQL | 8.x |
| Redis | 6.x / 7.x |

后端默认连接：

- MySQL：`127.0.0.1:3306/smart_admin_v3`
- Redis：`127.0.0.1:6379`，database `1`

连接信息在 `smart-admin-api-java17-springboot3/sa-base/src/main/resources/dev/sa-base.yaml` 中配置。

## 初始化数据库

```bash
mysql -uroot -p smart_admin_v3 < 数据库SQL脚本/mysql/smart_admin_v3.sql
```

版本升级时，按版本号从小到大执行 `数据库SQL脚本/mysql/sql-update-log/` 下的增量脚本。

## 启动后端

```bash
cd smart-admin-api-java17-springboot3
mvn -B -ntp -DskipTests clean package

java -Xms256m -Xmx512m -jar sa-admin/target/sa-admin-dev-3.0.0.jar \
  --spring.profiles.active=dev
```

后端默认端口 `1024`。启动前请确保日志目录和上传目录可写，具体路径见后端 `AGENTS.md`。

## 启动 PC 管理端

```bash
cd smart-admin-web-typescript
npm install
npm run localhost
```

访问 `http://127.0.0.1:8081`。开发环境下浏览器请求 `/api/...`，由 Vite 代理到后端，代理目标通过 `API_PROXY_TARGET` 配置。

默认账号：`admin / 123456`。

## 启动移动端

```bash
cd smart-app
npm install
npm run dev:h5
```

H5 默认端口 `5173`，接口地址通过 `.env.development` 中的 `VITE_APP_API_URL` 配置。

## 构建

```bash
# PC 管理端
cd smart-admin-web-typescript
npm run build:prod

# 移动端 H5
cd smart-app
npm run build:h5
```

## 许可

本项目基于 MIT 许可证发布，详见 `LICENSE`。