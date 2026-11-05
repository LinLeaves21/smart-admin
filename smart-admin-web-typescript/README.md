# SmartAdmin Web

Vue 3 + TypeScript + Vite + Ant Design Vue 的 PC 管理端。

## 启动

```bash
npm install
npm run localhost
```

访问 `http://127.0.0.1:8081`。开发环境通过 Vite 将 `/api` 代理到后端，代理目标由 `.env.localhost` 中的 `API_PROXY_TARGET` 配置。

## 构建

```bash
npm run build:prod
```

生产部署时请将 `.env.production` 中的 `VITE_APP_API_URL` 改为实际接口地址或同源反向代理路径。