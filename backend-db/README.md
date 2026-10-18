# Backend Scaffold（纯 DB 模式）

基于 Java 21 与 Spring Boot 的后端基建脚手架（RBAC / 系统配置 / 日志），多模块单体应用。

本副本为**纯 DB 模式**：业务数据只落关系库，无 Redis OM、无 Kafka、无 `app.hot-store` 开关。
Redis 仅用于令牌会话 / OAuth2 一次性 ticket（`StringRedisTemplate`）。热库 + Kafka 异步落库的版本见同级 `backend/`。

## 模块

| 模块 | 职责 |
|------|------|
| `app-common` | 公共模型、异常、MDC/Trace、仓储基类、DB 仓储支撑 |
| `app-system` | 认证、授权、租户、系统配置、操作/文件日志 |
| `app-boot` | 启动入口、环境配置、logback、模块聚合 |

详细约定见 [DEVELOPMENT_GUIDELINE.md](./DEVELOPMENT_GUIDELINE.md)。

## 本地验证

```bash
./mvnw test
./mvnw -DskipTests package
```

## 部署（单机）

部署目录：`docker/single/`。

```bash
# 首次上线：本机构建并推到服务器（密码只输一次；服务器只需 Docker）
./scripts/ship.sh 1.0.0 user@10.10.1.50                    # 默认装到 /opt/scaffold/single
./scripts/ship.sh 1.0.0 user@10.10.1.50 /data/scaffold   # 自定义根目录 → /data/scaffold/single

# 以后应用更版（管理页上传 zip）
./scripts/build-release-zip.sh 1.0.1
# 管理页：系统管理 → 版本升级 → 上传 dist/release/app-1.0.1.zip
# 激活后新进程启动时 Flyway 自动迁移；失败看容器日志，勿手工改 flyway_schema_history。
# 回滚应用不会撤销已加的列/表。若需退回旧 jar：ln -sfn releases/{old} /opt/scaffold/current 后重启。
#
# 换库（同一 jar）：改 spring.datasource.url / username / password 即可。
# 启用 MES（app.mes.enabled=true，默认开）后只支持 MySQL 8，其他库启动会被拒绝。
#   jdbc:mysql://host:3306/scaffold
#   jdbc:postgresql://host:5432/scaffold
#   jdbc:oracle:thin:@//host:1521/orcl
#   jdbc:sqlserver://host:1433;databaseName=scaffold
# 可选 app.db.type=mysql|postgresql|oracle|sqlserver（仅 URL 无法识别时）
# 目标库需为空库或已有对齐的 Flyway history；不能把 MySQL 数据目录改成 PG。

# 无 SSH / 空气隔离：有网机打离线包（含 linux/amd64 镜像）
./scripts/package-offline.sh 1.0.0
# 拷 dist/scaffold-single-1.0.0.tgz 到服务器后：
#   tar -xzf scaffold-single-1.0.0.tgz && cd single && ./install.sh
# 目标机需已装 Docker + compose，linux/amd64；install 不拉网、不 build。
# 应用更版仍用管理页上传 release/app-x.y.z.zip；中间件升级才需重新打离线镜像包。
```

详情见 [docker/single/README.md](docker/single/README.md)。

## 配置文件结构

三层配置，**功能域编号对齐**，便于 `diff` 与对照：

| 文件 | 角色 |
|------|------|
| `app-boot/src/main/resources/application.properties` | 全环境**默认值**（可运行的公共参数） |
| `application-dev.properties` | 仅覆盖开发差异（连接串、宽松 JWT、SQL 日志等） |
| `application-prod.properties` | 仅覆盖生产/容器差异（服务名、日志路径、连接池规模） |

激活：`spring.profiles.active=dev|prod`，或环境变量 `SPRING_PROFILES_ACTIVE`。

### 功能域一览（与文件内章节号一致）

| # | 域 | 公共文件主要内容 | dev / prod 典型覆盖 |
|---|----|------------------|---------------------|
| 1 | 应用基础 | `spring.application.name`、默认 profile | 一般无覆盖 |
| 2 | HTTP / Tomcat / 虚拟线程 | 端口、线程、HTTP/2 | 一般无覆盖 |
| 3 | Spring MVC 异步 | `async.request-timeout`（SSE） | 一般无覆盖 |
| 4 | OpenAPI | swagger / api-docs 路径 | 生产可关文档 |
| 5 | 监控 | Actuator、Prometheus、健康详情策略 | dev：`health.show-details=always` |
| 6 | 认证 / JWT | secret、令牌有效期、测试 token | dev 放宽有效期；prod 用环境变量覆盖 secret |
| 7 | MyBatis-Plus | 逻辑删除 | dev：开启 `SqlSingleLineLog` |
| 8 | 日志 | 路径、通道文件名、异步队列、脱敏、console-threshold | dev：DEBUG + 短保留；prod：`/app/logs` + WARN + 大队列 |
| 9 | 短雪花 ID | `app.id-generator.*` | 多实例改 `worker-id` |
| 10 | 透明查询缓存 | `app.cache.*` | 一般无覆盖 |
| 11 | Flyway / 更版 | `db/migration/{vendor}` + `app.release.*` | 本地 `./release-dev` |
| 12 | MySQL 连接 | （连接信息只写在 profile） | host、库名、连接池 |

未在 profile 中写出的项**继承公共默认**；profile 内「（无覆盖）」注释表示该域完全沿用公共配置。

OAuth2 开关与前端回跳等写在 **系统配置种子**（`db/migration/*/R__system_data.sql`），不在 `application*.properties` 重复声明。

## 运维 / 部署约定

| 项 | 建议 |
|----|------|
| 日志目录 | 生产 `logging.file-path=/app/logs/`（挂载卷）；本地默认 `logs/` |
| 日志通道 | `app.log` / `sql.log` / `audit.log` / `error.log` / `performance.log` |
| 控制台 | dev：`logging.console-threshold=DEBUG`（文本）；prod：`WARN`（JSON） |
| 异步队列 | 生产可调大 `logging.async-queue-size`（默认脚手架 prod=2048） |
| JWT 密钥 | **必须**用环境变量覆盖 `auth.jwt.secret`，勿用仓库默认值 |
| 数据源 | 用环境变量或 profile 覆盖，勿提交真实口令到公共仓库 |
| SSE | 公共配置已设 `spring.mvc.async.request-timeout=1800000`，与长连接对齐 |
| 链路追踪 | 请求头 / 响应头 `X-Trace-ID`；与前端 axios 注入对齐 |
| Actuator | 默认暴露 `health,info,prometheus`；生产健康详情仅授权可见 |

## 删除引用与错误码

删除实体前请使用 `DeleteReferenceUtils.requireNoReferences`。存在引用时会抛出 `BusinessException`，并在 `Result.errorParams` 中返回：

- `target`：目标名称  
- `boundBy`：引用方名称列表  
- `details`：含条数的摘要  

前端可用错误码 + 插值展示，无需解析中文 message。

## 透明查询缓存

MyBatis 层的查询缓存，业务/二开代码**无需任何改动**：新增表、Mapper、查询方法自动生效。

| 项 | 说明 |
|----|------|
| 读 | 所有 SELECT 以「最终 SQL（含租户条件、分页）+ 参数 + 涉及各表版本号」为 key 缓存结果 |
| 写 | 经 MyBatis 的 insert/update/delete 后递增涉及表的版本号，旧缓存立即不可达（O(1)，无需逐条删） |
| 事务 | 事务内写过的表，事务内读绕过缓存；提交后才失效，回滚不失效 |
| 降级 | Redis 异常自动直连库，连续失败熔断 |
| 排除 | `app.cache.exclude-tables`（默认 `hist_*`）；Mapper 方法加 `@NoCache` |
| TTL | `app.cache.default-ttl` 兜底；`app.cache.table-ttl.<表>` 或 `@CacheTtl(seconds=…)` 覆盖 |
| 指标 | Micrometer `app.cache.query{result=hit|miss|bypass|error}` |

注意：绕过 MyBatis 的写入（JdbcTemplate、手工 SQL、数据修复）不会自动失效，请注入
`QueryCacheInvalidator` 调用 `invalidateTables(...)` / `invalidateAll()`，否则最长等 TTL 过期。
自定义 MyBatis 插件若需要看到未缓存的原始查询，请注册在缓存插件之前。
