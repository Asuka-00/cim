# Scaffold Backend 开发规范

> 适用于 `Jdk21+element-plus/backend`（app-common / app-system / app-boot）。  
> 业务项目可在此脚手架上扩展独立模块，勿把领域逻辑写进 common。

---

## 1. 模块结构

```
backend/
├── app-common/     # 公共底座：HTTP、MDC、仓储基类、DB 仓储支撑、异常
├── app-system/     # 系统域：RBAC、租户、字典、系统配置、操作/文件日志
└── app-boot/       # 启动入口与 application*.properties、logback
```

- **common**：仅通用能力，禁止业务实体与业务 API  
- **system**：系统管理能力（可被业务模块依赖）  
- **boot**：只做聚合与配置，不写业务代码  

派生项目新增领域时增加独立模块（如 `xxx-biz`），由 boot 依赖。

---

## 2. 包组织（feature / domain）

```
com.scaffold.{module}.{domain}
├── api/v1/                 # Controller
├── domain/
│   ├── entity|dto|vo|query
│   ├── converter/          # MapStruct
│   ├── repository/         # 接口
│   └── service/            # 应用服务
└── infra/
    ├── repository/         # 实现（db / om）
    ├── security/
    └── mapper/
```

禁止按纯 layer 在模块根下堆砌 `controller/service/dao`。

---

## 3. 可观测性约定

| 能力 | 说明 |
|------|------|
| `X-Trace-ID` | `TraceIdFilter` 读取/生成/回写响应头；MDC 贯穿请求 |
| 用户/租户 | `TraceIdInterceptor` 在 Security 后补全 |
| 异步 | `MdcTaskDecorator` / `MdcPropagatingExecutorService` 透传 MDC |
| 日志文件 | `app.log` / `sql.log` / `audit.log` / `error.log` / `performance.log` |
| 日志类型 API | `LogFileType`：APP / AUDIT / SQL / ERROR / SLOW |
| Instant JSON | 支持 `2026-07-30T08:00:00Z` / offset / 本地时间 |
| 删除引用 | `DeleteReferenceUtils` 抛出 `errorParams`：`target` / `boundBy` / `details` |

---

## 4. MapStruct + Lombok

父 POM `annotationProcessorPaths` 顺序固定：

1. lombok  
2. **lombok-mapstruct-binding**  
3. mapstruct-processor  

缺少 binding 时可能生成空 `*ConverterImpl`。

---

## 5. 错误码与 Result

- 通用错误码：`ErrorCodeConstants`（1xxxxx / 2xxxxx 系统段）  
- 业务错误码由业务模块扩展，勿污染 common 通用段  
- `Result` 支持 `errorParams` 供前端 i18n 插值  

---

## 6. 测试

- 公共能力：`app-common` 单测  
- API：`@WebMvcTest` 或模块内切片测试  
- 启动：`app-boot` 集成测试（按需）  

```bash
./mvnw -pl app-common,app-system,app-boot -am test
```

---

## 7. 配置

三文件、**同序功能域**（见 `application.properties` 章节号与 README）：

| 文件 | 内容 |
|------|------|
| `application.properties` | 全环境默认（HTTP、JWT、日志通道、短雪花 ID、Flyway…） |
| `application-dev.properties` | 仅差异：连接串、宽松 JWT、SQL 日志、console text、短保留 |
| `application-prod.properties` | 仅差异：Docker 服务名、`/app/logs`、json、大连接池/队列 |

原则：

1. profile **只覆盖差异**，勿复制整份公共配置  
2. 敏感项用环境变量覆盖（`auth.jwt.secret`、数据源口令）  
3. SSE：`spring.mvc.async.request-timeout` ≥ 长连接超时  
4. 多实例：`app.id-generator.worker-id` 必须唯一  
5. OAuth2 业务开关在系统配置种子中维护，不进 properties 默认清单

---

**文档版本**: v1.0  
**更新日期**: 2026-08-04  
**适用项目**: Jdk21 + Element Plus Scaffold Backend
