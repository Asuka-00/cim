# Scaffold 单机部署 (`docker/single`)

## 最快路径

有网 + SSH（密码只输一次）：

```bash
# 在 admin/backend 根目录
./scripts/ship.sh 1.0.0 user@服务器IP                  # → /opt/scaffold/single
./scripts/ship.sh 1.0.0 user@服务器IP /data/scaffold  # → /data/scaffold/single
# 浏览器 http://服务器IP:8848
```

空气隔离 / 无 SSH（U 盘或内网盘）：

```bash
# 有网打包机（需 Docker，产物为 linux/amd64）
./scripts/package-offline.sh 1.0.0
# 拷 dist/scaffold-single-1.0.0.tgz 到目标机后：
tar -xzf scaffold-single-1.0.0.tgz
cd single
# 按需改 .env：冲突的宿主机端口
./install.sh
```

目标机只需 Docker + compose，架构 linux/amd64。`install.sh` 会 `docker load` 预构建镜像，再 `compose up --pull never`，不访问外网、不现场 build。

有网本机直接起：

```bash
../../scripts/build-release-zip.sh 1.0.0
docker compose up -d --build
```

## 说明

- 一个 **app** 容器：jar **内嵌前端**，UI/API 同源 `:8848`
- 支持管理页 **内网 zip 在线更版**（volume 软链 + 容器 restart）
- 中间件：MySQL + Redis（无 Kafka）
- 离线包内镜像平台固定为 `linux/amd64`（在 darwin/arm64 打包机也会交叉构建）

## 在线更版

```bash
../../scripts/build-release-zip.sh 1.0.1
# 上传 dist/release/app-1.0.1.zip → 激活
# 离线包里也带了 release/app-x.y.z.zip，同样可上传
```

数据卷 `./data/scaffold-home` → `/opt/scaffold`，更版不重建中间件。中间件升级才需重新打离线镜像包。

新进程启动时 Flyway 按 JDBC URL 选择 `db/migration/{mysql|postgresql|oracle|sqlserver}`。默认 compose 仍是 MySQL。换 PG/Oracle/SQL Server 时改 `spring.datasource.url`（及账号），目标库需为空或已有对齐 history。失败看 app 日志，不要手工改 `flyway_schema_history`。回滚只切软链，已加的列/表会保留。

## 端口

| 服务 | 默认宿主机端口 | `.env` |
|------|----------------|--------|
| app UI+API | 8848 / 18080 | `APP_PORT` / `APP_API_PORT` |
| mysql | 3306 | `MYSQL_PORT` |
| redis | 6379 | `REDIS_PORT` |
| oauth2-mock | 8180 | `OAUTH2_PORT` |

容器间走固定内部端口，改宿主机端口不必改应用配置。
