# M5 Batch 组批

> 状态:**设计草案,待核对**(未实现)· 依赖:M4 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_5__mes_m5.sql`)

## 1. 目标

支持炉管、清洗槽等批处理设备:多个 Lot 组成 Batch,一次操作进出站,事务展开为各 Lot 的独立事务。

## 2. 范围

- 站点/设备批处理属性
- Batch 创建与约束校验
- Batch Track-In/Out
- 拆批/取消/剔除
- Batch 历史与终端界面

## 3. 设计规则

- **批处理属性**:`mes_step.is_batch_step=1` 并配置 `batch_min_lots/batch_max_lots/batch_max_wafers`;设备可用 `mes_equipment.batch_max_lots` 覆盖(同一站点上不同设备腔体容量不同)。
- **组批约束**:同站点、同 Recipe 版本、同设备;各 Lot 状态 WAITING 且无 Hold;可选约束"同产品"/"同 Lot 类型"通过 `mes.batch.same.product` 参数控制。
- **生命周期**:`OPEN`(正在组批)→ `READY`(满足最小批量,可进站)→ `RUNNING` → `COMPLETED`;任一阶段可 `CANCELLED`。进站前可增删 Lot;进站后只能整体出站或剔除异常 Lot。组批编辑期间用 `mes_object_lock` 做跨请求占用。
- **成员与 `mes_lot.batch_id`**:加入 Batch 时写 `batch_id`,移除、剔除、Batch `CANCELLED/COMPLETED` 时清空;重新加入同一 Batch 复用原 `REMOVED` 行改回 `IN`(`uk_batch_lot` 不冲突)。`batch_id` 非空的 Lot 不能单独 Track-In/Split/Merge:M5 给内置规则 `LOT_STATE` 追加"不在 Batch 中,或由本 Batch 事务发起"。
- **Batch Track-In/Out**:一次请求。默认(`mes.batch.partial=false`)在**同一事务**内对每个 Lot 执行标准 Track-In/Out(沿用 M3 逻辑,含 Interlock),子事务以 `parent_txn_id` 挂在 Batch 事务下,任一 Lot 失败整批回滚并返回逐 Lot 原因。**部分成功模式**(`mes.batch.partial=true`)不在外层持锁:改为逐 Lot 独立事务、各自加锁并分别提交,**不保证整批原子**,设备锁在每个子事务内获取。父事务在全部子事务结束后写入:至少一个 Lot 成功即 `SUCCESS`(占用幂等键,逐 Lot 结果写 `payload_json.response`,同 `request_id` 重试直接返回该结果),全部失败为 `FAILED`;`mes_batch.status` 与失败 Lot 的移出在父事务内完成。父事务写入前宕机时,已提交的子事务没有幂等键保护,重试由各 Lot 的状态校验(Track-In 要求 WAITING 且在该步骤,Track-Out 要求 RUNNING)拒绝重复执行,结果中标为"已完成"。
- **剔除 Lot**:`RUNNING` 时剔除是**一个事务**:对该 Lot 执行 Cancel Track-In 的补偿处理(M4)、置 `mes_batch_lot.status=REMOVED`、更新 Batch 的 `lot_count/wafer_count/total_qty`、清空 `mes_lot.batch_id`;不改变 Batch 其他 Lot。剔除后 Batch 无成员则置 `CANCELLED`。
- **设备并发口径**:参数 `mes.batch.count.as.one`(**默认 true**):Batch 整体占用设备 1 个并发名额——M5 起 `EQP_CAPACITY` 按 `COUNT(DISTINCT COALESCE(batch_id, id))` 统计设备上的在制占用,Batch 进站只在第一个 Lot 前校验一次,同一 Batch 的后续 Lot 不再占名额;置 false 时改为按 `COUNT(*)` 逐 Lot 占用,每个 Lot 进站都校验,批处理设备的 `max_concurrent_lots` 应不小于其批量上限。
- **批级物料**:BOM 行 `PER_RUN/PER_TIME` 在 Batch 出站时只扣一次,记在 `seq` 最小的 `IN` 成员的子事务上,其余 Lot 跳过(M9);按片/按件的消耗仍逐 Lot 扣减。
- **批次可追溯**:`hist_mes_lot_history.batch_id`(M3 已含)记录每条历史所属 Batch,Batch 结束后仍可从历史反查。

## 4. 表设计

### 4.1 新增表(2 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_batch` | Batch | `app-mes-exec` |
| `mes_batch_lot` | Batch 内 Lot | `app-mes-exec` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_batch` Batch
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `batch_no` | VARCHAR(64) | 是 |  | Batch 号 |
| `step_id` | BIGINT | 是 |  | 站点 |
| `equipment_id` | BIGINT |  |  | 设备 |
| `recipe_version_id` | BIGINT |  |  | Recipe 版本 |
| `status` | VARCHAR(16) | 是 | OPEN | OPEN/READY/RUNNING/COMPLETED/CANCELLED |
| `lot_count` | INT | 是 | 0 | 当前 Lot 数 |
| `wafer_count` | INT | 是 | 0 | 当前 Wafer 总数 |
| `total_qty` | BIGINT | 是 | 0 | 总数量 |
| `created_by_user` | VARCHAR(64) |  |  | 创建人 |
| `track_in_at` | DATETIME |  |  | 进站时间 |
| `track_out_at` | DATETIME |  |  | 出站时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_batch_no`(tenant_id, batch_no);索引 `idx_step_status`(tenant_id, step_id, status);索引 `idx_equipment`(tenant_id, equipment_id)

#### `mes_batch_lot` Batch 内 Lot
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `batch_id` | BIGINT | 是 |  | Batch |
| `lot_id` | BIGINT | 是 |  | Lot |
| `seq` | INT | 是 | 1 | 组内序号 |
| `qty` | BIGINT |  |  | 加入时数量 |
| `status` | VARCHAR(16) | 是 | IN | IN/REMOVED |
| `added_at` | DATETIME | 是 |  | 加入时间 |
| `removed_at` | DATETIME |  |  | 移除时间 |
| `remove_reason` | VARCHAR(256) |  |  | 移除原因 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_batch_lot`(tenant_id, batch_id, lot_id);索引 `idx_lot`(tenant_id, lot_id)

### 4.2 变更表(3 处)

#### 变更 `mes_step` 站点

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `is_batch_step` | TINYINT(1) | 是 | 0 | 是否批处理站点 |
| `batch_min_lots` | INT |  |  | 最小批量(Lot 数) |
| `batch_max_lots` | INT |  |  | 最大批量(Lot 数) |
| `batch_max_wafers` | INT |  |  | 最大 Wafer 总数,空表示不限 |

#### 变更 `mes_equipment` 设备台账

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `batch_max_lots` | INT |  |  | 设备级批量上限,覆盖站点设置;空表示取站点设置 |

#### 变更 `mes_lot` Lot

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `batch_id` | BIGINT |  |  | 当前所属 Batch |

新增索引:索引 `idx_batch`(tenant_id, batch_id)

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/batches` CRUD;POST `…/{id}/lots`(加入)、DELETE `…/{id}/lots/{lotId}`(移除)
- POST `/api/v1/mes/batches/{id}/ready`、`/track-in`、`/track-out`、`/cancel`
- GET `/api/v1/mes/batches/candidates?stepId=&equipmentId=` 可组批 Lot 列表

## 6. 验收标准

1. 3 个 Lot 组成 Batch 进出站,各 Lot 历史完整且能通过 `batch_id` 关联 Batch
2. 批量低于最小值不能进站,超过最大值不能加入
3. Batch 内某 Lot Interlock 失败时,整体拒绝并返回具体 Lot 的原因,且不产生半个 Batch 的状态
4. Batch 占用设备名额的口径与参数一致:默认参数下 6 个 Lot 的 Batch 可在 `max_concurrent_lots=1` 的设备进站
5. RUNNING 中剔除的 Lot 回到 WAITING、`batch_id` 清空,Batch 计数同步;Batch 中的 Lot 不能单独进站
6. 一炉 Batch 出站时 PER_RUN 物料只扣一次

## 7. 待确认

1. 批处理设备"占用名额"口径默认按整体算 1;若你们的设备按 Lot 数占位,需要把 `mes.batch.count.as.one` 默认值改为 false。
