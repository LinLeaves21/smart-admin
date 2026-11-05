# AGENTS.md — SmartAdmin Web（TypeScript）

本文件面向在此项目中工作的 AI 编码代理。修改代码前先阅读本文件；跨项目契约见仓库根目录 `AGENTS.md`。

## 1. 项目定位

SmartAdmin v3 的 PC 管理端，Vue 3 + TypeScript + Ant Design Vue。业务页面、路由和按钮权限都由后端菜单数据驱动。仓库同时只维护这一套前端，原来的 `smart-admin-web-javascript` 已删除。

## 2. 技术栈（版本锁定）

| 组件 | 版本 | 说明 |
|---|---|---|
| Vue | 3.4.27 | `<script setup lang="ts">` |
| Vite | 5.2.12 | 开发端口 8081 |
| TypeScript | 5.6.3 | `strict`，但构建不做类型检查 |
| Ant Design Vue | 4.2.5 | 弹窗使用 `:open`，不是 `:visible` |
| Pinia | 2.1.7 | 无持久化插件，手写 localStorage |
| Vue Router | 4.3.2 | `createWebHashHistory` |
| Axios | 1.6.8 | 统一封装在 `src/lib/axios.ts` |
| ECharts | 5.4.3 | 仅在首页 Demo 使用 |
| wangEditor | `@wangeditor-next/*` | 不是旧版 `wangeditor` |
| 样式 | Less ~4.2.0 | 没有 Sass/SCSS |
| 加密 | sm-crypto + crypto-js | 默认 SM4 |
| Node | >= 18 | 当前环境 Node 24 可运行 |

## 3. 启动、构建与代理

```bash
cd smart-admin-web-typescript
npm install

npm run localhost     # 本地模式，端口 8081
npm run dev           # 开发模式，端口 8081
npm run build:prod    # 生产构建
```

### 3.1 开发代理（重要）

当前开发环境的请求链路：

```text
浏览器  /api/login/getCaptcha
  -> Vite proxy（/api 去掉前缀）
后端    http://127.0.0.1:1024/login/getCaptcha
```

- `src/lib/axios.ts` 的 `baseURL`：开发环境固定 `/api`，生产环境用 `VITE_APP_API_URL`。
- `vite.config.ts` 配置了 `/api` 和 `/upload` 两个代理。
- `API_PROXY_TARGET` 没有 `VITE_` 前缀，只在 Node 侧使用，不会暴露给浏览器。
- `loadEnv(mode, process.cwd(), '')` 的第三个参数不能删，否则读不到 `API_PROXY_TARGET`。

**API 文件里的路径不要写 `/api`。** 例如写 `/login`、`/support/dict/query`；写成 `/api/login` 会变成 `/api/api/login`。

### 3.2 环境文件

| 文件 | 用途 | 关键变量 |
|---|---|---|
| `.env.localhost` | `npm run localhost` | `API_PROXY_TARGET=http://127.0.0.1:1024` |
| `.env.development` | `npm run dev` | `API_PROXY_TARGET=http://127.0.0.1:1024` |
| `.env.test` | `build:test` | `VITE_APP_API_URL=http://127.0.0.1:1024` |
| `.env.pre` / `.env.production` | 预发/生产构建 | 同源反向代理路径 `/smart-admin-api` |

- 只有 `VITE_` 前缀变量会进入浏览器产物，不要把密钥写进 `VITE_*`。
- `.env.production` / `.env.pre` 默认使用同源路径，真实部署前需按网关或反向代理配置调整。
- `build:test` 使用 `--base=/admin/`，部署到子路径时注意资源路径。

## 4. 目录职责

```text
src/
  api/            # 接口封装，按 system/support/business 分目录
  views/          # 页面，xxx-list.vue / xxx-form-modal.vue / xxx-detail.vue
  components/     # framework/system/support/business 可复用组件
  layout/         # 四种布局、菜单、面包屑、标签页、用户区、帮助文档、水印
  router/         # 仅静态路由 + 动态菜单路由构建
  store/          # Pinia modules
  directives/     # v-privilege
  lib/            # axios、SM4/AES、sentry、水印、表格高度
  utils/          # localStorage、字符串、polyfill
  constants/      # 枚举、常量、tableId、localStorage key
  i18n/           # 仅设置抽屉使用
  theme/          # Less 变量、全局样式、颜色盘
  plugins/        # $smartEnumPlugin、$dictPlugin、$privilege
  config/         # 应用默认配置
  types/          # 类型声明
```

业务划分：
- `system`：登录、首页、员工、部门、角色、菜单、职位、个人中心。
- `support`：字典、参数、文件、定时任务、缓存、日志、帮助文档、消息、代码生成、加解密 Demo。
- `business`：ERP（商品、分类）和 OA（企业、银行、发票、通知公告），属于示例业务。

**没有 `SmartTable` / `SmartForm` 组件。** 表格是原生 `a-table`，表单是原生 `a-form`；通用表格工具是 `src/components/support/table-operator`。

## 5. 路由与权限

### 5.1 路由来源

- 静态路由只有 `/login`、`/home`、`/account`、`/help-doc`、`/403`、404 catch-all。
- 业务路由全部来自登录接口返回的 `menuList`，由 `buildRoutes()` 动态 `addRoute`。
- **不要往 `src/router/routers.ts` 加业务路由。** 静态路由会绕过菜单权限。
- 路由 `name` 和 `meta.componentName` 都是后端 `menuId` 字符串，不要手工改。
- `component` 字段是相对 `src/views` 的路径，必须与真实文件完全一致；找不到时页面空白且无显式报错。
- 隐藏详情页的正确做法：新增 `menu_type=2`、`visible_flag=0` 的菜单，`path` 指向详情路由，`component` 指向详情页。
- 登录页实际使用的是 `src/views/system/login3/login.vue`；旧版 `login/`、`login2/` 已删除。
- 路由守卫只校验 token，没有页面级权限判断；能访问哪些页面取决于后端返回的菜单。

### 5.2 按钮权限

```vue
<a-button v-privilege="'system:department:add'">添加</a-button>
<a-button v-if="$privilege('oa:enterprise:detail')">详情</a-button>
```

- `v-privilege` 只在 `mounted` 执行一次，无权限直接删除 DOM，不会响应式恢复；动态场景用 `$privilege` + `v-if`。
- 一个指令只接受一个权限字符串，不支持数组/OR/AND。
- `administratorFlag` 默认值是 `true`，用户信息加载前权限判断会放行；它不是安全边界。
- 权限字符串必须同时核对：
  - 前端 `t_menu.web_perms`
  - 后端 `t_menu.api_perms` 与 `@SaCheckPermission`
- 现有代码已经存在前后端权限字符串不一致的情况，新增权限时不要只写前端。

## 6. 请求层

文件：`src/lib/axios.ts`

- 请求拦截器从 localStorage 读取 token（key：`smart_admin_user_token`），设置 `Authorization: Bearer <token>`。
- 响应拦截器返回的是**完整响应体**，页面里用 `res.data`、`res.data.list`。
- 成功判断是：

```ts
if (res.code && res.code !== 1) { ... }
```

后端成功码是 `0`。**不要把它改成 `res.code !== 1`**，否则所有成功响应都会被当成错误。`src/api/base-model/response-model.ts` 里的 `success` 字段是过期的，后端实际字段是 `ok`。

- 特殊错误码：
  - `30007` / `30008`：登录失效，自动退出。
  - `30010` / `30011`：等保提醒，弹窗但不退出。
  - `30012`：长时间未操作，3 秒后强制退出。
- 响应 `dataType === 10` 时，拦截器会自动 SM4 解密 `data`；业务代码不需要手动解密。
- Axios 没有配置 timeout；刷新页面时 `main.ts` 会等 `getLoginInfo`，网络挂起可能白屏。

### 6.1 加密

- `src/lib/encrypt.ts` 默认 `EncryptObject = SM4`，密钥 `DefaultCryptoKey`。
- 登录密码单独加密：`password: encryptData(loginForm.password)`。
- 整包加密请求用 `postEncryptRequest`，对应后端 `@ApiDecrypt`；当前业务很少使用。
- 切换 AES 必须同时改后端 Bean，当前后端只有 SM4 实现带 `@Service`。

### 6.2 上传/下载

- 上传：`src/api/support/file-api.ts` → `POST /support/file/upload?folder=...`，组件是 `src/components/support/file-upload/index.vue`。
- `FileUpload` 只 emit `change`，不 emit `update:value`，**不要用 `v-model`**；用 `:defaultFileList` + `@change`。
- 后端文件 URL 是相对路径 `/upload/...`，开发环境由 Vite `/upload` 代理，生产环境需要同源反向代理。
- 下载：`getDownload` / `postDownload` 目前没有返回 Promise，不能 `await` 下载完成；错误响应是 Blob 时会解析 JSON 提示。
- 后端 `BigDecimal` 和大 `Long` 可能以字符串返回，金额/ID 不要直接做数值运算或盲目 `===`。

## 7. 状态管理

Pinia 实例：`src/store/index.ts`，无持久化插件。

| Store | 文件 | 职责 |
|---|---|---|
| `userStore` | `src/store/modules/system/user.ts` | token、用户信息、菜单树、路由菜单、功能点、标签页、keep-alive、未读数 |
| `appConfig` | `src/store/modules/system/app-config.ts` | 语言、布局、主题、暗黑/紧凑、标签页、水印、帮助文档 |
| `dictStore` | `src/store/modules/system/dict.ts` | 全量字典、`dictCode -> dictData` 映射、值转标签 |
| `roleStore` | `src/store/modules/system/role.ts` | 角色权限树编辑状态（不参与全局权限判断） |
| `spinStore` | `src/store/modules/system/spin.ts` | 全屏 loading |

注意：
- 退出登录会删除 `APP_CONFIG`，主题/布局/语言会恢复默认。
- `appConfig` 从 localStorage 读取时不与默认值合并；新增配置字段后老用户可能拿到 `undefined`。
- 新增持久化 key 时，记得在 `userStore.logout()` 中清理。

## 8. 页面与组件约定

### 8.1 典型 CRUD 页面

参考最干净的实现：
- `src/views/support/config/config-list.vue`
- `src/views/support/config/config-form-modal.vue`

列表页结构：
1. `a-form.smart-query-form` 查询区
2. `a-card size="small" :bordered="false"`
3. `smart-table-btn-block` + `TableOperator`
4. `a-table :pagination="false"` + `#bodyCell`
5. 独立 `a-pagination`
6. 表单弹窗组件

表单弹窗：
- `defineEmits(['reloadList'])`
- `formDefault` + `reactive`
- `showModal(rowData)` 重置并回填
- `onClose()` 重置并关闭
- `defineExpose({ showModal })`
- 提交用 `formRef.value.validate()`
- 成功 `message.success` → `emit('reloadList')` → `onClose()`
- `SmartLoading.show()/hide()` 必须成对

### 8.2 枚举

```ts
export const GOODS_STATUS_ENUM = {
  APPOINTMENT: { value: 1, desc: '预约中' },
  SELL: { value: 2, desc: '售卖中' },
};
```

- 新枚举必须注册到 `src/constants/index.ts`，否则 `$smartEnumPlugin` / `<SmartEnumSelect>` 找不到。
- 枚举值必须与后端 `BaseEnum` 一致。

### 8.3 表格列配置

- `TableOperator` 的 `tableId` 必须是全局唯一，来自 `src/constants/support/table-id-const.ts`。
- 现有 `position-list.vue` 错误复用了 `SYSTEM.EMPLOYEE`，会覆盖员工列配置；新增表格不要照抄。
- 不需要服务端列配置时传 `:tableId="null"`。

### 8.4 新增一个 CRUD 页面

必须完成以下几件事：

1. API：`src/api/{system|support|business}/<module>/<name>-api.ts`
2. 列表页：`src/views/.../<name>-list.vue`
3. 表单弹窗：同目录 `components/<name>-form-modal.vue`
4. 枚举/常量（可选）：`src/constants/...` 并注册到 `src/constants/index.ts`
5. 表格列 ID（可选）：`src/constants/support/table-id-const.ts`
6. 后端菜单记录（必须）：
   - `menu_type=2`
   - `path` 是前端路由
   - `component` 是相对 `src/views` 的 `.vue` 路径
   - 详情页用 `visible_flag=0` 的隐藏菜单
7. 按钮功能点（按需）：`menu_type=3`，同时填 `web_perms` 和 `api_perms`
8. 不需要改 `src/router`

## 9. 样式与 i18n

- 只有 Less；全局入口 `src/theme/index.less` 和 `src/theme/smart-admin.less`。
- 常用全局类：`smart-query-form`、`smart-query-form-row`、`smart-query-form-item`、`smart-table-btn-block`、`smart-table-operate-block`、`smart-query-table-page`。
- 主题变量在 `src/theme/custom-variables.ts`，通过 Vite `less.modifyVars` 注入，页面可直接用 `@primary-color`、`@header-height`。
- AntD 主题在 `src/App.vue` 用 `a-config-provider` 配置；暗黑/紧凑算法也在那里。
- i18n 实际只覆盖设置抽屉（`setting.*`），业务页面全部硬编码中文。新增业务页面不要引入 i18n。
- 修改布局时要同步检查四个布局文件：`side-layout.vue`、`side-expand-layout.vue`、`top-layout.vue`、`top-expand-layout.vue`。

## 10. 质量与格式

- `package.json` **没有** `lint`、`format`、`type-check`、`test` 脚本。
- `vite build` 只做 esbuild 转译，**类型错误不会阻断构建**；不要用“能 build”证明类型正确。
- ESLint 9 + `.eslintrc.cjs` 当前不能直接工作（缺 TS parser、无 lint 脚本）；不要为了修 lint 全仓格式化。
- 格式以 `.prettierrc.cjs` 和现有代码为准：2 空格、单引号、分号、150 列、`<script>`/`<style>` 内容缩进 2 空格。
- 路径别名只支持 `/@/`，不要写 `@/`。
- 新增代码不要引入新的 UI 框架、状态库、格式化配置或测试框架。

## 11. 硬约束与禁止事项

1. 业务路由不要写进 `src/router`，由后端菜单生成。
2. API 路径不要带 `/api`；`/api` 只存在于 axios `baseURL` 和 Vite 代理。
3. 不要修改 `if (res.code && res.code !== 1)` 的成功判断。
4. 不要依赖 `ResponseModel.success` 字段。
5. 不要假设存在 `SmartTable` / `SmartForm`。
6. 不要漏注册新枚举；不要复用其他表格的 `tableId`。
7. 权限字符串必须前后端同时核对，不要只写 `v-privilege`。
8. `v-privilege` 只用于静态按钮；动态权限用 `$privilege` + `v-if`。
9. `FileUpload` 不要用 `v-model`。
10. 不要在前端硬编码后端地址 `http://127.0.0.1:1024`；开发走代理，生产走 `VITE_APP_API_URL`。
11. 不要删除 `package-lock.json`、`node_modules` 之外由你新增的依赖锁文件；不要提交 `node_modules`、`dist`、`.idea`。
12. 不要修改 `.env.production` 指向的演示地址后忘记验证真实部署地址。
13. 不要新增 `java.util.Date` 类似的旧习惯；日期用字符串 + dayjs。
14. 不要把演示页面（首页统计、ECharts 示例）当作真实数据源。
15. 不要声称“类型检查通过”或“lint 通过”，当前没有对应脚本。

## 12. 验证清单

```bash
# 安装与启动
npm install
npm run localhost

# 页面
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8081/

# 代理是否正常（应返回后端 JSON，而不是 index.html）
curl -s http://127.0.0.1:8081/api/login/getCaptcha

# 生产构建
npm run build:prod
```

- 修改请求层后，确认 F12 里请求地址是 `/api/...`，不是 `127.0.0.1:1024`。
- 修改权限后，用非管理员账号验证按钮和接口都受控。
- 修改菜单后，刷新页面重新登录，确认动态路由生成。
- 修改上传后，确认返回的 `/upload/...` 可以通过 8081 访问。

## 13. 关键文件索引

- 入口：`src/main.ts`、`src/App.vue`、`index.html`
- 路由：`src/router/index.ts`、`src/router/routers.ts`、`src/router/system/login.ts`
- 请求/加密：`src/lib/axios.ts`、`src/lib/encrypt.ts`
- 用户与权限：`src/store/modules/system/user.ts`、`src/directives/privilege.ts`、`src/plugins/privilege-plugin.ts`
- 字典：`src/store/modules/system/dict.ts`、`src/components/support/dict-select`
- 表格/表单：`src/components/support/table-operator`、`src/views/support/config/config-list.vue`
- 主题：`src/theme/custom-variables.ts`、`src/App.vue`
- 构建：`vite.config.ts`、`.env.*`、`package.json`、`tsconfig.json`