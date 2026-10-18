# Admin Web

Vue 3 · Vite · TypeScript · Naive UI · UnoCSS 管理端脚手架。保留登录、RBAC 和平台配置，业务页面已去掉。

## 环境要求

- Node.js ≥ 20.19
- pnpm ≥ 10.5
- 后端运行于 `VITE_SERVICE_BASE_URL`（默认 `http://localhost:18080`）

## 快速开始

```bash
pnpm install
pnpm dev          # http://localhost:9527
```

## 保留的页面

- 登录（含 OAuth2 回调）
- 用户、角色、权限、租户
- 字典类型、字典项
- 系统配置、版本升级
- 操作日志、日志文件

登录后的菜单来自后端 `GET /api/v1/users/me/permissions`。首页是权限树里的第一个菜单。

## 环境变量

见 `.env`、`.env.test`、`.env.prod`。常用项：`VITE_APP_TITLE`、`VITE_SERVICE_BASE_URL`、`VITE_SERVICE_SUCCESS_CODE`。
