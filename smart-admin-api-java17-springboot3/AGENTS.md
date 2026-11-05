# AGENTS.md — SmartAdmin 后端（Java 17 / Spring Boot 3）

本文件面向在此项目中工作的 AI 编码代理。修改代码前先阅读本文件；跨项目契约见仓库根目录 `AGENTS.md`。

## 1. 项目定位

SmartAdmin v3 的 Java 17 + Spring Boot 3 后端，提供登录认证、员工/部门/角色/菜单/权限、字典、文件、消息、通知公告、代码生成、定时任务、数据变更记录、三级等保等能力。仓库同时只维护这一套后端，原来的 `smart-admin-api-java8-springboot2` 已删除。

## 2. 技术栈（版本锁定）

| 组件 | 版本 | 说明 |
|---|---|---|
| Java | 17 | 编译参数 `-parameters` |
| Spring Boot | 3.5.4 | 使用 `jakarta.*`，不要引入 `javax.servlet/validation/annotation` |
| MyBatis-Plus | 3.5.12 | 分页、BaseMapper、自动填充 |
| Sa-Token | 1.44.0 | 登录态与权限注解 |
| Druid | 1.2.25 | 手工创建数据源 |
| p6spy | 3.9.1 | dev/test 的 JDBC URL 前缀 |
| MySQL Connector/J | 9.3.0 | 仅 MySQL |
| Redisson | 3.50.0 | 定时任务锁、发布订阅 |
| Knife4j / springdoc | 4.6.0 / 2.7.0 | 接口文档 |
| Hutool | 5.8.39 | 工具库 |
| Lombok | Boot BOM 管理 | `@Data`、`@Slf4j` |

## 3. 模块与包结构

Maven 两个模块，依赖方向只能是 `sa-admin -> sa-base`：

- `sa-base`：公共基础库。统一响应/异常/分页/工具、数据源与 MyBatis 配置、全局异常、自动填充、启动监听器，以及可复用的 support 业务域（文件、字典、消息、定时任务、代码生成、三级等保等）。没有启动类。
- `sa-admin`：可执行应用。登录、员工、部门、角色、菜单、职务、数据权限、admin 侧 support Controller，以及示例业务（分类/商品/OA）。

启动类：`sa-admin/src/main/java/com/smartadmin/admin/AdminApplication.java`
- `@ComponentScan("com.smartadmin")`
- `@MapperScan(annotationClass = Mapper.class)`：DAO 必须显式加 `@Mapper`
- 排除了 `UserDetailsServiceAutoConfiguration`，项目不用 Spring Security 登录，只用 `spring-security-crypto` 做 Argon2 密码哈希

业务分层：

```text
module/<system|support|business>/<domain>/
  controller/XxxController.java
  service/XxxService.java
  manager/XxxManager.java          # 多表写、事务边界
  dao/XxxDao.java                  # @Mapper + BaseMapper<Entity>
  domain/entity/XxxEntity.java
  domain/form/XxxAddForm.java      # 新增入参
  domain/form/XxxUpdateForm.java
  domain/form/XxxQueryForm.java    # 继承 PageParam
  domain/vo/XxxVO.java
```

命名后缀固定用 `Entity/Form/VO/DTO/BO`，不要自造 `PO/DO/Req/Resp`。历史 OA 模块有把实体/表单直接放 `domain/` 的情况，新代码按标准结构。

路由约定：
- support 接口统一 `/support` 前缀（`SupportBaseController`）。
- system/business 接口直接在根路径，例如 `/employee/query`、`/oa/notice/query`、`/category/tree`。
- 后端没有 `/api` 前缀；前端开发环境的 `/api` 是 Vite 代理前缀，转发时会去掉。

## 4. 启动、构建与配置

### 4.1 前置依赖

- JDK 17、Maven 3.9+
- MySQL 8.x，库名 `smart_admin_v3`，已导入 `数据库SQL脚本/mysql/smart_admin_v3.sql`
- Redis 6/7，默认 `127.0.0.1:6379`、database `1`、无密码
- 可写目录：日志目录（默认 `/home/logs/smart_admin_v3/sa-admin/dev`）和上传目录（默认 `/home/smart_admin_v3/upload/`）。`Ip2RegionListener` 启动时会往日志目录复制 `ip2region.xdb`，目录不可写会直接启动失败。
- `t_config` 必须存在 `super_password` 和 `level3_protect_config` 两行，否则登录 NPE 或启动失败。

### 4.2 构建与启动

```bash
cd smart-admin-api-java17-springboot3
mvn -B -ntp -DskipTests clean package

java -Xms256m -Xmx512m -jar sa-admin/target/sa-admin-dev-3.0.0.jar \
  --spring.profiles.active=dev
```

- 构建产物名：`sa-admin-<profile>-3.0.0.jar`。
- 端口：dev/pre/prod 为 `1024`，test 为 `11024`，context-path 都是 `/`。
- Maven profile 决定打进 jar 的配置内容，Spring profile 只是运行标签。**两者必须一致**：用 `-Pdev` 打的包不要用 `--spring.profiles.active=prod` 启动，否则连的是 dev 库却按 prod 逻辑运行。

### 4.3 当前已知构建问题（必读）

`pom.xml` 的 resources 配置里，同一个 profile 目录被复制两次：

1. `*.yaml` 使用 `filtering=true`
2. `*.*` 使用 `filtering=false`，包含 yaml

后者覆盖前者，导致 `application.yaml` 中的 `spring.profiles.active: '@profiles.active@'` 没有被替换。直接 `java -jar` 会因 profile 非法而启动失败。

**在修复 pom 之前，启动必须显式带 `--spring.profiles.active=dev`（或 test/pre/prod）。** 如果要修，推荐把第三个 resource 的 includes 改成只包含非 yaml 文件，或把 yaml 从第三个 resource 排除。

IDE 直接运行也可能找不到 `application.yaml`，因为配置在 `src/main/resources/<profile>/` 而不是根目录。先执行 `mvn -Pdev process-resources`，或把对应 profile 目录标记为资源根。

### 4.4 配置加载

- `application.yaml` 负责端口、日志目录、`spring.profiles.active`。
- `sa-base.yaml` 负责数据源、Redis、邮件、文件存储、Sa-Token、定时任务等通用配置，由 `YamlProcessor`（`EnvironmentPostProcessor`）以 `addLast` 加载；`application.yaml` 优先级更高，可覆盖。
- 四个 profile 的配置必须同步维护：`dev/test/pre/prod`。
- dev/test 的 JDBC URL 是 `jdbc:p6spy:mysql://...` + `P6SpyDriver`；prod 是 `jdbc:mysql://...` + `com.mysql.cj.jdbc.Driver`，不要混用。

## 5. 请求处理链路

请求依次经过：

1. CORS Filter（仅 dev/test 注册）
2. `DecryptRequestAdvice`（仅 `@ApiDecrypt`）
3. `AdminInterceptor`（登录态、`@NoNeedLogin`、`@SaCheckPermission`）
4. Controller 参数校验
5. `@RepeatSubmit` 防重切面
6. `@OperateLog` 操作日志切面
7. `EncryptResponseAdvice`（仅 `@ApiEncrypt`）
8. `GlobalExceptionHandler`

关键约定：
- **所有响应 HTTP 状态码都是 200**，业务成功/失败看 `ResponseDTO.code`。成功码是 `0`。
- `ResponseDTO` 字段：`code / level / msg / ok / data / dataType`。
- 错误码范围：
  - `10001-20000`：`SystemErrorCode`
  - `20001-30000`：`UnexpectedErrorCode`
  - `30001-40000`：`UserErrorCode`
  - 新增错误码必须落在已注册范围内；新增独立枚举必须在 `ErrorCodeRegister` 注册不重叠范围，否则启动时报错。
- 业务校验优先返回 `ResponseDTO.userErrorParam(...)`，不要抛异常。`BusinessException` 的 code 是系统错误 10001。
- 未登录/无权限不会返回 401/403，而是 200 + 错误码。前端依赖 `code` 判断。
- `@OperateLog` 会原样记录请求参数 JSON，含密码/身份证/银行卡的接口不要加；响应的 `data` 会被清空。
- `@ApiEncrypt` 只加密 `ResponseDTO.data`，异常响应不加密。前端按 `dataType=10` 判断是否解密。

## 6. 认证、权限与登录

- Token：请求头 `Authorization: Bearer <token>`（`sa-token.token-name` / `token-prefix`）。
- `@NoNeedLogin`：只支持方法级，跳过登录校验。新增匿名接口必须显式加，并确认 `UrlConfig` 会把它加入无需登录清单。
- `@SaCheckPermission("module:action")`：功能权限。**项目没有默认拒绝机制**，方法不加注解时任何登录用户都能访问；新增 admin 接口必须加注解，并同步在 `t_menu.api_perms` 配置相同字符串。
- `administratorFlag=true` 的用户直接跳过所有权限校验。
- loginId 格式是硬约定：普通登录 `userType + ":" + employeeId`，万能密码 `S:<uuid>:<employeeId>`。不要修改用户类型位数或拼接格式。
- 登录密码必须 SM4 加密后传输，后端 `LoginService` 会无条件 `apiEncryptService.decrypt(password)`。密钥 `DefaultCryptoKey`，前后端必须一致。
- 当前 `@Service` 注册的是 SM4 实现；AES 实现不是 Bean。切换算法必须同时改前端 `EncryptObject` 和后端 Bean。
- 密码存储：Argon2id，盐格式 `明文密码_UID大写_UID小写`。修改 `employeeUid` 会导致该员工密码失效。
- 默认万能密码是 `t_config.super_password = ChangeMe_2026!`，生产必须修改或移除。
- 默认登录账号：`admin / 123456`（种子数据，仅开发环境）。

## 7. 数据层约定

- DAO 必须加 `@Mapper` 并继承 `BaseMapper<Entity>`。
- 自定义 SQL 只能写在 `src/main/resources/mapper/**/*.xml`，namespace 用 DAO 全限定名；项目没有注解 SQL。`DataSourceConfig` 会加载 `classpath*:/mapper/**/*.xml`。
- 所有表统一 `t_` 前缀；实体用 `@TableName("t_xxx")`；主键默认 `IdType.AUTO`。
- **没有启用 MyBatis-Plus 逻辑删除**：不要依赖 `@TableLogic`。各表是否逻辑删除不一致，查询/更新/删除必须显式处理 `deletedFlag`；`selectById` 会返回已删除记录。
- 自动填充只按字段名 `createTime` / `updateTime`，且只对 MyBatis-Plus 的 insert/updateById/updateBatchById 生效。自定义 XML update 要自己设置 `updateTime`。
- 分页：
  - 查询 Form 继承 `PageParam`，不要重复定义 `pageNum/pageSize`。
  - `pageNum` 从 1 开始，`pageSize` 最大 500。
  - 用 `SmartPageUtil.convert2PageQuery` / `convert2PageResult`。
  - 返回 `ResponseDTO<PageResult<VO>>`。
  - 排序字段会做 SQL 注入检查，但不要接受任意用户输入作为列名。
- 事务加在 Manager 层，`@Transactional(rollbackFor = ...)`。**返回 `ResponseDTO.error` 不会回滚**；需要回滚必须抛异常。
- 不要返回 Entity，尤其是含 `loginPwd` / `employeeUid` 的 `EmployeeEntity`；统一用 VO。
- Jackson 全局约定：
  - 日期 `yyyy-MM-dd HH:mm:ss`，时区 GMT+8；使用 `LocalDate/LocalDateTime`。
  - `BigDecimal` 序列化为字符串，前端拿到的金额可能是字符串。
  - `Long` 超出 JS 安全整数范围时序列化为字符串，ID 可能是 number 或 string。
  - 枚举使用 `BaseEnum` + `@SchemaEnum`，校验用 `@CheckEnum`。

## 8. 缓存、Redis 与并发

- 业务 Redis key 必须用 `RedisKeyConst`，并通过 `RedisService.generateRedisKey` 生成，格式 `project:env:prefix:key`。
- Spring Cache 名称使用 `CacheKeyConst` / `AdminCacheConst`；默认 Redis，key 前缀 `cache:<cacheName>:`，默认 TTL 7 天；支持 `cacheName#ttl`。
- 字典、角色、菜单、员工权限都有缓存。修改角色/菜单/字典后不会自动清所有相关缓存；必要时显式 `@CacheEvict` 或清缓存，否则最长 7 天后才生效。
- 重要并发不要用 `synchronized`（多实例无效），使用数据库唯一索引或 Redisson。
- `RedisService.getLock/unLock` 是简单 SETNX，不是 owner-safe 分布式锁；重要场景用 Redisson。

## 9. 支撑能力要点

- 文件存储：
  - `file.storage.mode=local|cloud`。
  - 本地模式映射 `/upload/**` 到 `upload-path`。dev 配置的 `url-prefix: /upload/` 返回相对 URL，前端通过同源代理访问。
  - 本地模式的 `private` 目录并没有访问控制，知道 URL 就能下载；真正私有需要云存储预签名。
  - Spring 的 `max-request-size=10MB` 会先于 `max-file-size=20MB` 生效，实际上传上限约 10MB。
  - 文件类型由 `SecurityFileService` 用 Tika 检测，禁止 SVG。
- 代码生成器强依赖 MySQL：`information_schema` + `select database()`，修改数据库类型时不可用。
- 数据变更记录（`t_data_tracer`）需要业务代码手动调用 `DataTracerService`，不是自动切面；配置 `@DataTracerFieldLabel` 等注解。
- 定时任务实现 `SmartJob` 接口并注册 Bean，使用 Redisson 锁；`smart.job.enabled=false` 时相关 Bean 不注册。
- 热加载：`@SmartReload("tag")` 标注 public 方法，`SmartReloadManager` 扫描；通过修改 `t_reload_item.identification` 触发。
- 三级等保：登录失败锁定、密码复杂度/历史、双因子、文件检测，配置存 `t_config.level3_protect_config`。
- 数据权限 `@DataScope` 目前是预埋能力，没有任何业务方法使用；不要假设它已经生效。
- `@Async` 未启用（启动类没有 `@EnableAsync`），新增异步方法前先加注解并评估线程池。
- 单号生成默认是单机内存实现；多实例部署需要改用 Redis/MySQL 实现。

## 10. 新增业务模块的标准流程

1. 在 `sa-admin/src/main/java/com/smartadmin/admin/module/business/<domain>` 建包；可复用能力放 `sa-base/module/support`。
2. 按 `controller/service/manager/dao/domain` 分层；多表写操作放 Manager 并加事务。
3. Entity 加 `@TableName("t_xxx")`；DAO 加 `@Mapper`；复杂 SQL 写 XML。
4. 查询 Form 继承 `PageParam`；Service 用 `SmartPageUtil`。
5. Controller 返回 `ResponseDTO`；分页返回 `ResponseDTO<PageResult<VO>>`。
6. 每个接口加 `@SaCheckPermission("<module>:<action>")`；同步在后端菜单的 `api_perms` / `web_perms` 配置相同字符串。
7. 需要审计的写操作调用 `DataTracerService`；需要操作日志的接口按需加 `@OperateLog`（注意敏感字段）。
8. 新增错误码放进现有枚举范围；新增缓存 key 放进常量类。
9. 更新 `数据库SQL脚本/mysql/sql-update-log/`：新增表或字段要提供对应版本的增量 SQL，不要只改初始脚本。
10. 构建、启动并验证接口；至少覆盖登录、权限、分页、增删改。

## 11. 硬约束与禁止事项

1. 新代码一律使用 `jakarta.*`；`javax.sql.DataSource` 除外。
2. 不要从 `sa-base` 反向依赖 `sa-admin`。
3. 不要引入 PostgreSQL 支持“顺手改一下”：代码硬编码 `DbType.MYSQL`、分页方言、37 处 `DATE_FORMAT`、代码生成器 `information_schema`、DDL 都是 MySQL 语法。PG 需要系统性移植。
4. 不要删除 `--spring.profiles.active=...` 启动参数，直到修复资源过滤。
5. 不要改 loginId 格式、密码盐格式、`employeeUid` 语义、SM4 密钥。
6. 不要在未加 `@SaCheckPermission` 的情况下新增 admin 接口；项目没有默认拒绝。
7. 不要依赖 `@TableLogic` 或 `selectById` 自动过滤逻辑删除。
8. 不要在 Service 返回错误 DTO 后仍期望事务回滚。
9. 不要把 SQL 写在注解里；不要漏 `@Mapper`。
10. 不要在 `@OperateLog` 接口中接收密码、身份证等敏感明文。
11. 不要用 `synchronized` 做跨实例互斥。
12. 不要提交 `target/`、IDE 配置、日志、上传文件。
13. 不要新增 `java.util.Date`；日期统一 `LocalDate/LocalDateTime`。
14. 不要把演示配置（数据库密码、邮箱密码、`super_password=ChangeMe_2026!`）带到生产。
15. 不要声称“测试通过”而只跑了构建：当前测试类没有 `@Test`，`mvn test` 不验证业务。

## 12. 验证清单

```bash
# 构建
mvn -B -ntp -DskipTests clean package

# 启动（必须带 profile）
java -Xms256m -Xmx512m -jar sa-admin/target/sa-admin-dev-3.0.0.jar \
  --spring.profiles.active=dev

# 端口与日志
ss -ltnp | grep 1024
tail -f /home/logs/smart-admin_v3/sa-admin/dev/info/*.log

# 登录页验证码（确认服务可用）
curl -s http://127.0.0.1:1024/login/getCaptcha
```

- 确认 MySQL/Redis 可连接、`t_config` 两行配置存在、日志/上传目录可写。
- 涉及权限的改动，用非管理员账号验证 `@SaCheckPermission` 生效。
- 涉及缓存的改动，验证变更后是否立即生效。

## 13. 关键文件索引

- 启动与配置：`sa-admin/src/main/java/com/smartadmin/admin/AdminApplication.java`、`pom.xml`、`sa-admin/src/main/resources/dev/application.yaml`、`sa-base/src/main/resources/dev/sa-base.yaml`
- 请求链路：`sa-admin/.../interceptor/AdminInterceptor.java`、`sa-base/.../handler/GlobalExceptionHandler.java`、`sa-base/.../common/domain/ResponseDTO.java`、`sa-base/.../config/UrlConfig.java`
- 登录与权限：`sa-admin/.../module/system/login/service/LoginService.java`、`sa-admin/.../module/system/login/manager/LoginManager.java`、`sa-base/.../securityprotect/service/SecurityPasswordService.java`
- 数据层：`sa-base/.../config/DataSourceConfig.java`、`sa-base/.../config/MybatisPlusConfig.java`、`sa-base/.../handler/MybatisPlusFillHandler.java`、`sa-base/.../common/util/SmartPageUtil.java`
- 缓存/Redis：`sa-base/.../module/support/redis/RedisService.java`、`sa-base/.../config/CacheConfig.java`
- 文件/等保：`sa-base/.../module/support/file/service/FileStorageLocalServiceImpl.java`、`sa-base/.../module/support/securityprotect/service/Level3ProtectConfigService.java`
- 参考纵向切片：`sa-admin/.../module/business/goods`（单表 CRUD）、`sa-admin/.../module/business/oa/notice`（多表 + 事务 + DataTracer）、`sa-base/.../module/support/feedback`（标准 support 模块）