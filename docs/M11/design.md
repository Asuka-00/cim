# M11 追溯与报表

> 状态:**设计草案,待核对**(未实现)· 依赖:M9、M10 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_11__mes_m11.sql`)

## 1. 目标

全链路正反向追溯(含封测单位转换与形态转换)、基础报表与生产日报。追溯数据早在 M3 起写入,本里程碑做查询与展示。

## 2. 范围

- Genealogy 正反向查询
- 追溯查询页
- WIP/产出/Cycle Time/Hold-Scrap-Rework 报表
- 生产日报

## 3. 设计规则

- **追溯查询**:基于 `hist_mes_genealogy_link`,以任一对象为起点向上/向下遍历。应用层迭代 BFS,限制最大深度(默认 12)和节点数(默认 5000),避免关系爆炸;**遇到 EQUIPMENT/TOOL 节点只展示不继续向下展开**(否则一台设备会关联出大量 Lot,需另用"设备+时间窗"查询);结果按层返回并标注关系类型。
- **典型查询**:① 物料批 → 受影响 Lot/Wafer/Reel(正向);② Wafer/Reel → 来料批、设备、操作人、工艺历史(反向);③ 设备 + 时间窗 → 经过的 Lot/Wafer。
- **报表数据来源**:实时类(WIP、Queue)直接查 `mes_lot`;统计类(产出、Cycle Time、Hold/Scrap/Rework)优先查 `hist_mes_lot_history`(走 `idx_step_type_time`),**排除已撤销事务一律经 `hist_mes_txn_reversal`**,不对事务流水做反连接;日报由租户遍历的定时任务汇总到 `mes_report_daily`,避免大表实时聚合。
- **Cycle Time**:Lot 级=完成时间 − 开批时间;站点级=Track-Out − 进入站点时间(含 Queue 与加工),可拆 Queue Time 与 Process Time。
- **日报口径**:班次来自 M12 的班别设置;M11 先按自然日,班次维度字段保留。
- **WIP 快照**:定时(默认每小时)写 `hist_mes_wip_snapshot`,用于看板趋势与历史对比。

## 4. 表设计

### 4.1 新增表(2 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_report_daily` | 生产日报汇总 | `app-mes-exec` |
| `hist_mes_wip_snapshot` | WIP 快照 | `app-mes-exec` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_report_daily` 生产日报汇总
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `stat_date` | DATE | 是 |  | 统计日期 |
| `shift_code` | VARCHAR(32) | 是 | ALL | 班次编码,ALL 表示全天 |
| `area_id` | BIGINT | 是 | 0 | 区域,0 表示合计 |
| `step_id` | BIGINT | 是 | 0 | 站点,0 表示合计 |
| `product_id` | BIGINT | 是 | 0 | 产品,0 表示合计 |
| `unit_id` | BIGINT | 是 | 0 | 计数单位,0 表示合计(不同单位的数量一般不应合计) |
| `in_qty` | BIGINT | 是 | 0 | Track-In 数量 |
| `out_qty` | BIGINT | 是 | 0 | Track-Out 数量 |
| `scrap_qty` | BIGINT | 是 | 0 | 报废数量 |
| `rework_qty` | BIGINT | 是 | 0 | 返工数量 |
| `hold_count` | INT | 是 | 0 | Hold 次数 |
| `avg_queue_min` | DECIMAL(12,2) |  |  | 平均 Queue 时长(分钟) |
| `avg_process_min` | DECIMAL(12,2) |  |  | 平均加工时长(分钟) |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_daily`(tenant_id, stat_date, shift_code, area_id, step_id, product_id, unit_id)

说明:由定时任务按天(及班次)重算,可重复执行(先删后插)。

#### `hist_mes_wip_snapshot` WIP 快照
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `snapshot_at` | DATETIME | 是 |  | 快照时间 |
| `area_id` | BIGINT |  |  | 区域 |
| `step_id` | BIGINT |  |  | 站点 |
| `product_id` | BIGINT |  |  | 产品 |
| `lot_type` | VARCHAR(16) |  |  | Lot 类型,空表示合计 |
| `unit_id` | BIGINT |  |  | 计数单位(转换前后的数量不可直接相加,按单位分行) |
| `lot_count` | INT | 是 | 0 | Lot 数 |
| `wafer_count` | INT | 是 | 0 | Wafer 数 |
| `qty` | BIGINT | 是 | 0 | 数量(unit_id 口径) |
| `hold_lot_count` | INT | 是 | 0 | Hold Lot 数 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_time`(tenant_id, snapshot_at);索引 `idx_step_time`(tenant_id, step_id, snapshot_at)

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- GET `/api/v1/mes/genealogy/forward?type=&id=&depth=`、`/genealogy/backward`
- GET `/api/v1/mes/genealogy/equipment-window?equipmentId=&from=&to=`
- GET `/api/v1/mes/reports/wip`、`/output`、`/cycle-time`、`/hold-scrap-rework`、`/daily`
- GET `/api/v1/mes/wip/snapshots`(趋势)

## 6. 验收标准

1. 输入物料批可查到受影响 Lot/Wafer/Reel
2. 输入 Reel 或 Wafer 可反查物料批、设备、操作人与工艺历史(Reel 反查到 Lot/Wafer 的粒度为 Lot 级;启用 Strip/Unit 序列号后可到 Strip/Unit)
3. 被撤销事务不出现在统计与追溯结果中
4. 日报产出数与 Lot 历史汇总一致
5. 追溯深度/节点数超限时给出截断提示而不是超时

## 7. 待确认

1. 报表是否需要导出 Excel?当前放 M12 的通用导出能力。
