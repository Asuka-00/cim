# CIM

半导体通用 MES（制造执行系统）。一个租户对应一个厂区，作业以操作员录入为主，不对接 EAP / ERP / WMS。

启用 MES 后只支持 **MySQL 8**。

## 目录

| 目录 | 内容 |
|------|------|
| `backend-db` | Java 21 / Spring Boot 3.5 后端，Flyway 迁移，纯关系库（Redis 只存登录会话） |
| `soybean-admin` | Vue 3 / Vite / TypeScript / Naive UI 管理端 |
| `docs` | 设计文档（M0–M13）。文内仍标为草案，以代码为准 |

## 环境

- JDK 21
- Node.js ≥ 20.19，pnpm ≥ 10.5
- MySQL 8
- Redis 7

## 本地启动

1. 建空库（例如 `scaffold`），并启动 Redis。
2. 修改 `backend-db/app-boot/src/main/resources/application-dev.properties` 里的 MySQL 与 Redis 地址、账号。
3. 启动后端（约 1–2 分钟，首次会跑 Flyway）：

```bash
cd backend-db
./mvnw -pl app-boot -am spring-boot:run
```

4. 启动前端：

```bash
cd soybean-admin
pnpm install
pnpm dev
```

| 入口 | 地址 |
|------|------|
| 管理端 | http://localhost:9527 |
| API | http://localhost:18080 |
| Swagger | http://localhost:18080/swagger-ui.html |

种子账号：`admin` / `admin123`。上线前请改掉。

## 后端模块

| 模块 | 职责 |
|------|------|
| `app-common` | 公共模型、异常、仓储；MES 跨模块契约在 `share.integration.mes` |
| `app-system` | 登录、RBAC、租户、系统配置、日志 |
| `app-mes-core` | 编号、对象锁、统一事务、领域事件、导入、打印 |
| `app-mes-base` | 产品、工艺路线、站点、设备、物料、BOM、Recipe |
| `app-mes-warehouse` | 库房、库位、库存、收发料 |
| `app-mes-exec` | 工单、Lot、Wafer、过站与追溯 |
| `app-boot` | 启动入口与数据库迁移 |

质量（NCR）与维保模块在设计文档的 M13，当前工程未装配。

## 单机部署

见 [backend-db/docker/single/README.md](backend-db/docker/single/README.md)。默认 UI 端口 `8848`，容器内同时提供 API。

更细的后端约定见 [backend-db/README.md](backend-db/README.md) 与 [backend-db/DEVELOPMENT_GUIDELINE.md](backend-db/DEVELOPMENT_GUIDELINE.md)。
