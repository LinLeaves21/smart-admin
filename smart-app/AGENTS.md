# AGENTS.md — SmartApp（uni-app 移动端）

本文件面向在此项目中工作的 AI 编码代理。修改代码前先阅读本文件；跨项目契约见仓库根目录 `AGENTS.md`。

## 1. 项目定位

SmartAdmin v3 的移动端，uni-app + Vue 3 + uni-ui，支持 H5、微信小程序、App 等多端。功能包括登录、首页、消息、通知公告、企业/客户、商品、意见反馈、版本更新等，同时保留了一批纯静态的 UI 演示页。

## 2. 技术栈（版本锁定）

| 组件 | 版本 | 说明 |
|---|---|---|
| uni-app（`@dcloudio/*`） | `3.0.0-3090920231225001` | 所有 `@dcloudio/*` 必须保持一致 |
| Vue | 3.2.47 | 不要升级到 3.3+/3.4+ |
| Pinia | 2.0.36 | 不要用 Pinia 3 或 setup store 新语法 |
| uni-ui（npm） | 1.5.0 | 另有本地 `src/uni_modules` 组件 |
| Vite | 4.0.3 | 默认 H5 端口 5173 |
| sass | 1.69.7 | 大量 `@import`，不要升级到 Sass 2 |
| sm-crypto / crypto-js | 0.3.13 / 4.1.1 | SM4 加密 |
| lodash | 4.17.21 | `$lodash` 全局属性 |
| dayjs | 1.11.10 | 仅演示页使用 |

- 纯 JavaScript，不是 TypeScript。`shims-uni.d.ts` / `shime-uni.d.ts` 是残留，不参与构建。
- 没有 `package-lock.json`、没有 `engines`、没有测试、没有 CI。
- 推荐 Node 18.x LTS，至少 18；不要用最新的 Node 直接跑这套 2023 年底的 uni-app 版本。

## 3. 启动与构建

```bash
cd smart-app
npm install

npm run dev:h5            # H5 开发，Vite 默认 5173
npm run build:h5          # H5 构建
npm run dev:mp-weixin     # 微信小程序开发
npm run build:mp-weixin   # 微信小程序构建
npm run dev:app           # 只生成 App 资源
```

- 必须在 `smart-app` 目录执行 npm 脚本；`vite.config.js` 的 `root` 是 `process.cwd()`。
- H5 开发**没有 Vite 代理**，直接请求 `VITE_APP_API_URL`（默认 `http://127.0.0.1:1024`），依赖后端 CORS。后端 CORS 只在 dev/test 注册，访问 pre/prod 后端会跨域失败。
- App 原生打包（APK/IPA）需要 HBuilderX；`npm run build:app` 只产出 App 资源。`src/manifest.json` 的 DCloud `appid` 当前为空，云打包前必须填写。
- 产物目录：CLI 为 `dist/dev|build/<platform>`，HBuilderX 为 `unpackage/dist/...`；不要提交 `dist`、`node_modules`、`unpackage`。
- `build.minify` 是 `terser`，但 `terser` 未在依赖中声明；如果构建报 `terser not found`，补 `npm i -D terser` 或改 `minify: 'esbuild'`。

## 4. 目录与页面

```text
src/
  pages/          # 页面；列表页/详情页/表单页 + 模块内 components
  api/            # 接口封装，按 system/support/business 分目录
  store/          # Pinia；仅 user store 真正使用
  lib/            # smart-request、encrypt、sentry、toast/loading
  components/     # 全局复用组件，必须显式 import
  constants/      # 枚举、文件目录、localStorage key、正则
  plugins/        # $smartEnumPlugin
  utils/          # 字符串工具（大部分未使用）
  uni_modules/    # uni-mescroll、y-tabs、uni-scss 等
  static/         # 图片资源
  theme/          # 全局 SCSS 工具类
  uni.scss        # uni-app 自动注入的全局变量
  pages.json      # 路由/窗口/tabBar/easycom
  manifest.json   # 平台配置（JSONC，含注释）
  main.js         # createSSRApp、Pinia、枚举插件、lodash
  App.vue         # 应用生命周期、全局样式
```

### 4.1 pages.json 是唯一路由表

- 新增页面必须在 `src/pages.json` 的 `pages` 数组注册，`path` 不带 `.vue`。
- tab 页必须同时在 `tabBar.list` 注册，跳转必须用 `uni.switchTab`，不能用 `navigateTo`。
- 当前 tabBar 5 项：`home`、`list`、`list2`、`message`、`mine`。其中 `list` / `list2` 是演示页。
- 没有 `subPackages`，18 个页面全在主包；微信小程序主包 2MB 限制需要关注。
- 普通页面跳转 `uni.navigateTo`，替换当前页 `uni.redirectTo`；`redirectTo` 不能跳 tab 页。
- 页面参数用 `onLoad(options)` 读取；现有参数名是硬编码的：`noticeId`、`enterpriseId`、`changeLogId`。
- 页面生命周期必须从 `@dcloudio/uni-app` 引入（`onLoad/onShow/onReady/onPageScroll/onReachBottom`），不要从 `vue` 引入。

### 4.2 页面清单与功能

| 路由 | 功能 | 说明 |
|---|---|---|
| `pages/home/index` | 首页 | tab；统计/轮播是静态数据，商品/公告有接口 |
| `pages/login/login` | 登录 | 账号密码 + 图形验证码 + 可选邮箱验证码 |
| `pages/mine/mine` | 我的 | tab；用户信息、消息角标、退出 |
| `pages/enterprise/enterprise-list` | 企业/客户列表 | 分页、搜索、拨号、新增 |
| `pages/enterprise/enterprise-form` | 企业新增/编辑 | 表单校验 |
| `pages/enterprise/enterprise-detail` | 企业详情 | 图片预览、删除、修改 |
| `pages/notice/notice-index` | 公告列表 | 分页、搜索、筛选 |
| `pages/notice/notice-detail` | 公告详情 | rich-text |
| `pages/goods/goods-index` | 商品列表 | 分页、搜索、筛选；字典组件当前有契约问题 |
| `pages/support/change-log/change-log-list` | 版本更新列表 | 字段名有已知偏差 |
| `pages/support/change-log/change-log-detail` | 版本更新详情 | 同上 |
| `pages/message/message` | 消息 | tab；分页、未读 |
| `pages/support/feedback/feedback-form` | 意见反馈 | 图片上传 |
| `pages/form/form` | 表单演示 | 纯静态假数据 |
| `pages/select-people/select-people` | 人员选择演示 | 纯静态 |
| `pages/list/list`、`pages/list2/list` | 列表样式演示 | tab；纯静态 |
| `pages/order-detail/order-detail` | 运单详情演示 | 纯静态 |

演示页不能当业务实现修改；它们没有接口、没有真实数据。

## 5. 请求层

文件：`src/lib/smart-request.js`

```js
const baseUrl = import.meta.env.VITE_APP_API_URL;
header: { Authorization: 'Bearer ' + getUserToken() }
```

- token 存在 `uni.getStorageSync('smart_h5_token')`，与 PC 端 `smart_admin_user_token` 不共享。
- 每次请求都会带 `Authorization: Bearer `，没有 token 时尾部是空格。
- 成功判断：

```js
if (res.code && res.code !== 1) { ... } else { resolve(res); }
```

后端成功码是 `0`。**不要改成 `res.code !== 1`**，否则所有成功响应都会被当失败。

- `30007/30008/30012` 会清 token 并 `navigateTo('/pages/login/login')`；当前错误码分支会重复 toast。
- 没有 HTTP status 判断、没有 timeout、没有重试、没有取消、没有刷新 token。
- 页面 catch 里不要重复 toast；请求层已经提示业务错误，页面只做 `smartSentry.captureError(e)`。
- Loading 用 `SmartLoading.show/hide`，必须成对；`await SmartLoading.show()` 是无效写法。
- 分页参数：`pageNum`（从 1 开始）、`pageSize`（<=500）、可选 `searchCount`；响应取 `res.data.list`、`res.data.pages`。
- 上传固定 `POST /support/file/upload`，multipart 字段 `file`，`folder` 是数字枚举（通用 1、公告 2、帮助中心 3、意见反馈 4）。

### 5.1 环境变量

| 文件 | API 地址 | 何时生效 |
|---|---|---|
| `.env.development` | `http://127.0.0.1:1024` | `dev:h5` 默认 |
| `.env.localhost` | `http://127.0.0.1:1024` | 需手动 `--mode localhost`，无脚本 |
| `.env.test` | `http://127.0.0.1:1024` | 需 `--mode test`；后端 test 端口是 11024，不一致 |
| `.env.pre` | `https://app.smartadmin.vip/smart-app-api` | 需 `--mode pre` |
| `.env.production` | `https://app.smartadmin.vip/smart-app-api` | `build:h5` 默认 |

`VITE_APP_TITLE` 当前没有被使用，`index.html` 的 title 为空。

### 5.2 加密

- `src/lib/encrypt.js` 默认 `EncryptObject = SM4`，密钥 `1024lab__1024lab`。
- 登录密码单独加密：`password: encryptData(loginForm.password)`。
- 响应 `dataType === 10` 时自动解密 `data`。
- 改 key/算法必须同时改后端；后端当前只有 SM4 实现是 `@Service`。
- `postEncryptRequest` 已实现但全项目未使用。

## 6. 登录与权限

登录入口：`src/pages/login/login.vue`

- 登录字段：`loginName`、SM4 加密后的 `password`、`captchaCode`、`captchaUuid`、`loginDevice`、`emailCode`。
- 默认账号密码在代码里硬编码为 `admin / 123456`，仅开发演示。
- 成功后 `setUserLoginInfo` 写 Pinia + storage，`uni.switchTab('/pages/home/index')`。
- 登录页每次 `onShow` 都重新拉验证码和双因子开关；验证码定时器离开页面不会清理，存在泄漏。
- 邮箱验证码按钮使用了未定义的 `message.success`（`login.vue:209`），点击会抛 `ReferenceError`，倒计时不会启动。
- 登录成功分支没有 `uni.hideLoading()`。

权限现状：
- **没有全局路由守卫，也没有页面级 token 校验。** 未登录靠接口返回 30007 后被动跳登录；断网时可能停留在空首页。
- 登录返回的 `menuList`、`needUpdatePwdFlag` 被完全忽略；没有按钮/页面权限体系。
- `userStore.administratorFlag` 默认 `true`，用户信息缺失时会当成管理员。
- 后端 `sa-token.is-concurrent=false`：同账号新登录会踢掉旧 token，App 和 PC 会互踢，这是后端策略，不是 bug。
- 退出登录只清本地，不调用 `/login/logout`，服务端 token 仍有效。

## 7. 已知契约偏差（修改前必须核对）

以下问题在当前代码中真实存在，新增功能不要复制这些写法：

| 问题 | 当前代码 | 后端实际 |
|---|---|---|
| 登录设备 H5 | `LOGIN_DEVICE_ENUM.H5.value = 3`，与 APPLE 重复 | `LoginDeviceEnum.H5 = 4` |
| 字典接口 | `GET /support/dict/value/list/{keyCode}` | `GET /support/dict/getAllDictData` |
| 字典字段 | `valueCode / valueName` | `dataValue / dataLabel` |
| 更新日志字段 | `item.version` / `res.data.version` | `updateVersion` |
| 企业 logo/营业执照 | 当数组取 `[0].fileUrl` | `String` URL |
| 商品产地 | 当对象数组取 `[0].valueName` | `String` |
| 反馈附件 | 传数组 | `FeedbackAddForm.feedbackAttachment` 是 `String` |
| 未注册路由 | `goods-detail`、`phone-login`、`protocol` | `pages.json` 中没有 |
| 未定义函数 | `enterprise-form` / `feedback-form` 的取消调用 `close()` | 页面没有该函数 |

处理原则：先核对后端 DTO 和接口，再改前端；不要只改一边。字典组件当前不可用，除非先修接口和字段。

## 8. 状态管理

- `src/store/index.js` 只有 `createPinia()`，没有持久化插件。
- `src/store/modules/system/user.js` 是唯一使用中的业务 store：token、用户信息、未读数。
- `src/store/modules/system/app-config.js` 是死代码：引用了不存在的 `@/config/app-config`、`@/utils/local-util`，且 `local-storage-key-const` 没有默认导出；不要 import 它。
- store 用 Pinia Options API（`state/getters/actions`）。
- `useUserStore()` 在请求层非组件上下文调用，依赖 `app.use(store)` 设置的 activePinia；不要在应用初始化前调用请求层。
- `setUserLoginInfo` 没有回填 `avatar`、`needUpdatePwdFlag`；"我的"页使用远程硬编码头像。

## 9. 组件与表单约定

- `src/components/<name>/index.vue` **不会被 easycom 自动识别**，页面必须显式 import。
- easycom 只覆盖：
  - `^uni-(.*)` → npm `@dcloudio/uni-ui`
  - `^y-(.*)` → 本地 `src/uni_modules/y-tabs`
- `mescroll-body` / `mescroll-empty` 来自本地 `uni_modules/uni-mescroll`，hook 必须显式 import：

```js
import useMescroll from '@/uni_modules/uni-mescroll/hooks/useMescroll';
```

- 列表页标准模式：

```vue
<mescroll-body @init="mescrollInit" :down="{ auto: false }" @down="onDown" @up="onUp">
```

```js
const { mescrollInit, getMescroll } = useMescroll(onPageScroll, onReachBottom);
// query 中：mescroll.endSuccess(list.length, res.data.pages > res.data.pageNum)
// 异常：mescroll.endErr()
```

- `enablePullDownRefresh` 保持 `false`，否则和 mescroll 手势冲突。
- 不要在 `onShow` 里再调一次查询，容易和 mescroll 首次 `up` 重复请求；现有 `enterprise-list`、`change-log-list` 有这种风险。
- 表单：`uni-forms` + `uni-forms-item` + `uni-easyinput`，`formRef.value.validate()`。
- 弹窗：子组件 `<uni-popup ref="popupRef">`，`defineExpose({ show })`，父组件 ref 调用；关闭时 emit `close`，`null` 表示取消。
- 枚举统一形状：

```js
export const GOODS_STATUS_ENUM = {
  APPOINTMENT: { value: 1, desc: '预约中' },
};
```

- 新枚举必须注册到 `src/constants/index.js`，否则 `$smartEnumPlugin` 返回空字符串。
- 字典（数据库字典）和枚举是两套机制；当前 `dict-select` 契约已失效，不要新增依赖。

## 10. 平台约束

- 静态资源使用 `@/static/...` 或 `/static/...`；**禁止新增 `/src/static/...`**，它只在 H5 开发服务器可用。
- 不要使用 `window`、`document`、`localStorage`；统一 `uni.*`。
- 平台相关代码必须用条件编译：`#ifdef APP-PLUS`、`#ifdef H5`、`#ifdef MP-WEIXIN`。
- `plus.*` 只能出现在 App 条件编译中；现有 Apple 登录入口在 H5 下会显示但点击报错。
- `uni.makePhoneCall` 在 H5 不支持。
- 小程序页面用 `<view>/<text>/<image>`，不要用 `<div>/<span>/<img>`；现有部分登录/字典组件有 HTML 标签，新代码不要照抄。
- tab 页跳转用 `uni.switchTab`；`navigateTo` 页面栈最多 10 层，登录失败重复跳转可能叠加。
- 自定义导航页（home/login/mine）需要自行处理状态栏高度；当前没有读取 `statusBarHeight`。
- 固定底部按钮没有处理 `env(safe-area-inset-bottom)`，新增页面要注意 iPhone 安全区。
- 小程序生产环境需要配置 request/uploadFile/downloadFile 合法域名；`urlCheck:false` 只对开发工具有效。

## 11. 硬约束与禁止事项

1. 不要单独升级 Vue 或任意 `@dcloudio/*`；所有 uni-app 包版本必须一致。
2. 不要引入 TypeScript、新的 UI 框架、新的状态库或 i18n 库。
3. 新页面必须同时改 `src/pages.json`；tab 页必须改 `tabBar` 并使用 `switchTab`。
4. 所有接口走 `src/lib/smart-request.js`，路径不带 baseURL；成功码是 0。
5. 不要修改 SM4 key 或默认算法；改加密必须前后端同时改。
6. 不要使用未注册路由；新增跳转前先检查 `pages.json`。
7. 不要复制演示页（`form`、`select-people`、`list`、`list2`、`order-detail`、首页统计/轮播）的静态数据写法。
8. 不要 import `store/modules/system/app-config.js`，它是死代码且会编译失败。
9. 不要用 `/src/static/...`；不要用 `window/document/localStorage`。
10. 不要在没有 `appid` 的情况下执行 App 云打包。
11. 不要把 `admin/123456`、硬编码手机号、远程头像、演示数据带到生产。
12. 不要提交 `node_modules`、`dist`、`unpackage`。
13. 不要全量格式化文件；仓库没有统一格式化，全量格式化会产生巨大 diff。
14. 不要修改 `src/uni_modules` 下第三方组件来修业务问题；优先在页面/组件层覆盖。
15. 不要用 `JSON.parse` 读取 `pages.json` / `manifest.json`，它们是 JSONC。

## 12. 验证清单

```bash
# H5
npm run dev:h5
# 打开 http://localhost:5173

# 微信小程序
npm run dev:mp-weixin
# 用微信开发者工具打开 dist/dev/mp-weixin

# 构建
npm run build:h5
npm run build:mp-weixin
```

- 新增/修改页面后，检查 `pages.json` 是否注册、所有 `navigateTo` 目标是否存在。
- 列表页验证首屏只请求一次、下拉刷新、上拉加载、`endErr` 分支。
- 登录后验证 token 写入、接口 401/30007 跳转、退出登录。
- 涉及平台 API 的改动，H5 和微信小程序各跑一遍。
- App 打包前确认 `src/manifest.json` 的 `appid`、版本号、权限和 iOS 隐私描述。

## 13. 关键文件索引

- 入口：`src/main.js`、`src/App.vue`、`src/pages.json`、`src/manifest.json`
- 请求/加密：`src/lib/smart-request.js`、`src/lib/encrypt.js`、`src/lib/smart-sentry.js`
- 登录：`src/pages/login/login.vue`、`src/pages/login/components/*`
- 用户：`src/store/modules/system/user.js`、`src/constants/local-storage-key-const.js`
- 列表模式：`src/pages/notice/notice-index.vue`、`src/pages/support/change-log/change-log-list.vue`
- 表单模式：`src/pages/enterprise/enterprise-form.vue`、`src/components/smart-enum-radio`
- 全局样式：`src/uni.scss`、`src/theme/index.scss`
- 枚举：`src/constants/index.js`、`src/plugins/smart-enums-plugin.js`