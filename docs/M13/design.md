# M13 质量 / 维保 / 高级分析

> 状态:**设计草案,待核对**(未实现)· 依赖:M11 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_13__mes_m13.sql`)

## 1. 目标

不合格品与评审、良率、Wafer 标识与 Sort,设备预防保养与验证、维修记录,E10 统计与 OEE,库存盘点,自定义看板。P2 及以后阶段,可按需求分批实现。

## 2. 范围

- 新建 app-mes-quality / app-mes-maint 模块与启用开关
- 缺陷、NCR、MRB、良率
- Wafer 标识/Sort
- PM 计划/工单/Qual、维修记录
- E10/OEE 统计
- 库存盘点
- 自定义看板

## 3. 设计规则

- **可选模块**:M13 新建 `app-mes-quality` 与 `app-mes-maint`(依赖 `app-mes-core`,经 `share.integration.mes.*` 契约访问 base/exec),由 `app.mes.quality.enabled` / `app.mes.maint.enabled`(默认 false)控制装配;未启用时其订阅者不注册,base/exec 不感知。盘点属于 warehouse、看板属于 base,不受开关影响。
- **NCR/MRB 流程**:发现不合格 → 建 NCR(关联 Lot/Wafer/数量/缺陷)→ Lot 自动 Hold(`hold_source=NCR`,`active_key = lot|NCR|ncr_id`,`source_ref_id = ncr_id`;原因码 `NCR_<类型>` 由 `MesTenantInitializer` 预置到 `mes_hold_reason`)→ 提交 MRB → 评审决定处置(`REWORK/SCRAP/USE_AS_IS/RETURN`)→ 执行处置(调用对应 Lot 事务)→ 关闭 NCR,按 `(NCR, ncr_id)` 释放全部 Lot 上的 Hold(含部分处置时 Split 出的子 Lot)。
- **处置执行**:MRB 的决定通过统一事务层落到 Lot/Wafer 操作,不在 quality 模块直接改 Lot 表;跨模块经 `share.integration.mes.exec.LotCommandProvider`(M3/M4 定义)。处置映射:REWORK→`rework`,SCRAP→`scrap`,USE_AS_IS→仅 Release,RETURN(退回客户/供应商)→`terminate`(原因码 `MRB_RETURN`);部分处置先 `split` 再对子 Lot 执行。
- **良率**:Die 良/不良数的**唯一数据源**是 `mes_lot_die_bin`(及其 Wafer Map,M6);`hist_mes_yield_record` 只是按站点/Lot 的**派生快照**(量测/Sort/FT 后写入),`mes_wafer_sort_result` 仅保存等级与测试时间,不重复保存 Die 数。
- **PM 触发**:`trigger_type` 为 TIME/LOT_COUNT/WAFER_COUNT/USAGE;计划作用于设备组时,**每台设备单独计数**(`mes_pm_plan_equipment`)。计数**异步**(2026-10-16 确认):maint 以订阅者 `PM_COUNTER` 订阅 `LOT_TRACK_OUT` 与 `TXN_REVERSED`,M0 投递表保证每条事件只处理一次、同一 Lot 的撤销不会先于出站处理;exec 不依赖 maint,计数允许秒级延迟(事件轮询默认 2 秒)。每次计数在 `hist_mes_pm_count_log` 记一行(`txn_id + plan_equipment_id + direction` 唯一兜底),增量按触发类型取:LOT_COUNT 每个 Lot 计 1(Batch 内各计 1),WAFER_COUNT 取 `wafer_count`,USAGE 按运行次数计,Batch 以 `parent_txn_id` 去重、一炉计 1。撤销时按 `orig_txn_id` 查出 INCR 行,逐行写 REVERSE 并原子回减;原计数发生在 PM 完成之前(`occurred_at < last_done_at`)时只记 REVERSE(`applied=0`)不回减;Batch 内单个 Lot 的撤销不回退 USAGE。计数用原子 `UPDATE counter_value = counter_value ± n`,不走乐观锁。到 `interval_value` 为该设备生成 PM 工单。PM 工单开始时可经 `EquipmentStatusProvider`(来源 `SYSTEM`)置设备 `SCHEDULED_DOWN`;完成并需要 Qual 时,Qual 通过才恢复 `STANDBY`。**Qual 本身要在停机/工程态下运行 Monitor/Engineering 类 Lot**,由 M3 `EQP_STATE` 规则的放行参数 `allow_lot_types` 支持。
- **E10/OEE**:基于 `hist_mes_equipment_status` 的时间段汇总(日表 `mes_equipment_daily_stat`),OEE = 可用率 × 性能率 × 良率;性能率需要标准节拍,首期仅输出可用率和 E10 各类占比,OEE 作为可选项。
- **盘点**:建盘点单快照账面数 → 录入实盘数 → 生成差异 → 审批后过账(`COUNT_ADJ`)。**从建单快照到过账**整段时间用 `mes_object_lock` 锁住盘点范围:按库位盘点锁各 `LOCATION`,整库盘点锁 `WAREHOUSE`;领退料、上料与直耗反冲都校验库位及其所属仓库未被锁(线边库位盘点会拦截反冲,应安排在停线或换班时)。差异 = 实盘数 − 快照账面数。
- **看板**:`mes_dashboard.layout_json` 保存组件与数据源配置,首期只做预置组件的排布。

## 4. 表设计

### 4.1 新增表(15 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_defect_record` | 缺陷记录 | `app-mes-quality` |
| `mes_ncr` | 不合格品报告 NCR | `app-mes-quality` |
| `mes_mrb` | MRB 评审 | `app-mes-quality` |
| `hist_mes_yield_record` | 良率记录 | `app-mes-quality` |
| `mes_wafer_mark` | Wafer 标识 | `app-mes-quality` |
| `mes_wafer_sort_result` | Wafer Sort 结果 | `app-mes-quality` |
| `mes_pm_plan` | PM 计划 | `app-mes-maint` |
| `mes_pm_plan_equipment` | PM 计划-设备计数 | `app-mes-maint` |
| `hist_mes_pm_count_log` | PM 计数流水 | `app-mes-maint` |
| `mes_pm_order` | PM 工单 | `app-mes-maint` |
| `mes_repair_record` | 维修记录 | `app-mes-maint` |
| `mes_equipment_daily_stat` | 设备日统计 | `app-mes-maint` |
| `mes_stocktake` | 盘点单 | `app-mes-warehouse` |
| `mes_stocktake_item` | 盘点明细 | `app-mes-warehouse` |
| `mes_dashboard` | 自定义看板 | `app-mes-base` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_defect_record` 缺陷记录
所属模块:`app-mes-quality`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `wafer_id` | BIGINT |  |  | Wafer |
| `step_id` | BIGINT |  |  | 发现站点 |
| `defect_code` | VARCHAR(64) | 是 |  | 缺陷码(字典 MES_DEFECT_CODE) |
| `defect_qty` | INT | 是 | 1 | 缺陷数量 |
| `pos_x` | DECIMAL(12,4) |  |  | 位置 X |
| `pos_y` | DECIMAL(12,4) |  |  | 位置 Y |
| `image_ref` | VARCHAR(256) |  |  | 图片 `mes_file.id`(M12) |
| `found_by` | VARCHAR(64) |  |  | 发现人 |
| `found_at` | DATETIME | 是 |  | 发现时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_lot`(tenant_id, lot_id);索引 `idx_code_time`(tenant_id, defect_code, found_at)

#### `mes_ncr` 不合格品报告 NCR
所属模块:`app-mes-quality`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `ncr_no` | VARCHAR(64) | 是 |  | NCR 编号 |
| `lot_id` | BIGINT | 是 |  | Lot |
| `step_id` | BIGINT |  |  | 发生站点 |
| `ncr_type` | VARCHAR(16) | 是 | PROCESS | PROCESS/MATERIAL/EQUIPMENT/INCOMING/OTHER |
| `qty` | BIGINT | 是 |  | 不合格数量 |
| `unit_id` | BIGINT |  |  | 计数单位 |
| `description` | VARCHAR(1024) | 是 |  | 问题描述 |
| `status` | VARCHAR(16) | 是 | OPEN | OPEN/MRB/DISPOSED/CLOSED |
| `hold_id` | BIGINT |  |  | 首个关联的 Lot Hold(展示用;释放按 `source_ref_id`) |
| `reported_by` | VARCHAR(64) |  |  | 报告人 |
| `reported_at` | DATETIME | 是 |  | 报告时间 |
| `closed_at` | DATETIME |  |  | 关闭时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_ncr_no`(tenant_id, ncr_no);索引 `idx_lot`(tenant_id, lot_id);索引 `idx_status`(tenant_id, status)

#### `mes_mrb` MRB 评审
所属模块:`app-mes-quality`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `mrb_no` | VARCHAR(64) | 是 |  | MRB 编号 |
| `ncr_id` | BIGINT | 是 |  | NCR |
| `decision` | VARCHAR(16) |  |  | 处置 REWORK/SCRAP/USE_AS_IS/RETURN |
| `rework_code` | VARCHAR(64) |  |  | 返工分支编码(REWORK 时) |
| `decision_qty` | BIGINT |  |  | 处置数量 |
| `decided_by` | VARCHAR(64) |  |  | 评审人 |
| `decided_at` | DATETIME |  |  | 评审时间 |
| `executed` | TINYINT(1) | 是 | 0 | 处置是否已执行 |
| `remark` | VARCHAR(1024) |  |  | 评审意见 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_mrb_no`(tenant_id, mrb_no);索引 `idx_ncr`(tenant_id, ncr_id)

#### `hist_mes_yield_record` 良率记录
所属模块:`app-mes-quality`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `wafer_id` | BIGINT |  |  | Wafer,空表示 Lot 级 |
| `step_id` | BIGINT |  |  | 统计时点站点 |
| `good_qty` | BIGINT | 是 | 0 | 良品数 |
| `bad_qty` | BIGINT | 是 | 0 | 不良数 |
| `yield_pct` | DECIMAL(8,4) |  |  | 良率(%) |
| `bin_summary` | JSON |  |  | Bin 汇总 |
| `txn_id` | BIGINT |  |  | 来源事务,统计时经撤销登记排除 |
| `recorded_at` | DATETIME | 是 |  | 记录时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot_time`(tenant_id, lot_id, recorded_at);索引 `idx_wafer`(tenant_id, wafer_id);索引 `idx_txn`(tenant_id, txn_id)

#### `mes_wafer_mark` Wafer 标识
所属模块:`app-mes-quality`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wafer_id` | BIGINT | 是 |  | Wafer |
| `mark_type` | VARCHAR(16) | 是 |  | SCRIBE/LASER/BARCODE |
| `mark_value` | VARCHAR(64) | 是 |  | 标识内容 |
| `verified` | TINYINT(1) | 是 | 0 | 是否已扫码核对 |
| `verified_by` | VARCHAR(64) |  |  | 核对人 |
| `verified_at` | DATETIME |  |  | 核对时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_wafer_type`(tenant_id, wafer_id, mark_type);索引 `idx_value`(tenant_id, mark_value)

#### `mes_wafer_sort_result` Wafer Sort 结果
所属模块:`app-mes-quality`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wafer_id` | BIGINT | 是 |  | Wafer |
| `lot_id` | BIGINT | 是 |  | Lot |
| `sort_grade` | VARCHAR(16) |  |  | 等级 |
| `tested_at` | DATETIME |  |  | 测试时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_wafer`(tenant_id, wafer_id);索引 `idx_lot`(tenant_id, lot_id)

#### `mes_pm_plan` PM 计划
所属模块:`app-mes-maint`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `plan_code` | VARCHAR(64) | 是 |  | 计划编码 |
| `plan_name` | VARCHAR(128) | 是 |  | 计划名称 |
| `target_type` | VARCHAR(16) | 是 | EQUIPMENT | EQUIPMENT/EQUIPMENT_GROUP |
| `target_id` | BIGINT | 是 |  | 设备/设备组 |
| `trigger_type` | VARCHAR(16) | 是 |  | TIME/LOT_COUNT/WAFER_COUNT/USAGE |
| `interval_value` | BIGINT | 是 |  | 触发间隔(天/Lot 数/Wafer 数/使用量) |
| `need_qual` | TINYINT(1) | 是 | 0 | 完成后是否需要 Qual |
| `checklist_json` | JSON |  |  | 检查清单 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_plan_code`(tenant_id, plan_code);索引 `idx_target`(tenant_id, target_type, target_id)

#### `mes_pm_plan_equipment` PM 计划-设备计数
所属模块:`app-mes-maint`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `plan_id` | BIGINT | 是 |  | PM 计划 |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `counter_value` | BIGINT | 是 | 0 | 距上次 PM 的累计计数 |
| `last_done_at` | DATETIME |  |  | 上次完成时间 |
| `next_due_at` | DATETIME |  |  | 下次到期时间(TIME 型) |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_plan_equipment`(tenant_id, plan_id, equipment_id);索引 `idx_due`(tenant_id, next_due_at)

#### `hist_mes_pm_count_log` PM 计数流水
所属模块:`app-mes-maint`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `plan_equipment_id` | BIGINT | 是 |  | PM 计划-设备计数行 |
| `equipment_id` | BIGINT | 是 |  | 设备(冗余) |
| `txn_id` | BIGINT | 是 |  | 计数来源事务;USAGE 型在 Batch 下取父事务 |
| `direction` | VARCHAR(8) | 是 |  | INCR/REVERSE |
| `delta` | BIGINT | 是 |  | 计数量(正数) |
| `applied` | TINYINT(1) | 是 | 1 | 是否实际改了计数;撤销跨 PM 周期时为 0 |
| `counter_after` | BIGINT |  |  | 处理后计数 |
| `event_id` | BIGINT |  |  | 来源领域事件 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:唯一 `uk_txn_plan_dir`(tenant_id, txn_id, plan_equipment_id, direction);索引 `idx_plan_time`(tenant_id, plan_equipment_id, occurred_at)

说明:只增表。唯一键兜底重复计数;撤销时按原事务查 INCR 行逐行写 REVERSE。

#### `mes_pm_order` PM 工单
所属模块:`app-mes-maint`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `pm_no` | VARCHAR(64) | 是 |  | PM 单号 |
| `plan_id` | BIGINT |  |  | 来源计划 |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `status` | VARCHAR(16) | 是 | PENDING | PENDING/IN_PROGRESS/COMPLETED/QUAL_PENDING/CLOSED/CANCELLED |
| `scheduled_at` | DATETIME |  |  | 计划时间 |
| `started_at` | DATETIME |  |  | 开始时间 |
| `completed_at` | DATETIME |  |  | 完成时间 |
| `executor` | VARCHAR(64) |  |  | 执行人 |
| `result` | VARCHAR(1024) |  |  | 执行结果 |
| `qual_required` | TINYINT(1) | 是 | 0 | 是否需要 Qual |
| `qual_result` | VARCHAR(16) |  |  | Qual 结论 PASS/FAIL |
| `qual_by` | VARCHAR(64) |  |  | Qual 确认人 |
| `qual_at` | DATETIME |  |  | Qual 时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_pm_no`(tenant_id, pm_no);索引 `idx_equipment_status`(tenant_id, equipment_id, status);索引 `idx_plan`(tenant_id, plan_id)

#### `mes_repair_record` 维修记录
所属模块:`app-mes-maint`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `repair_no` | VARCHAR(64) | 是 |  | 维修单号 |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `fault_id` | BIGINT |  |  | 关联异常单 |
| `started_at` | DATETIME | 是 |  | 开始时间 |
| `finished_at` | DATETIME |  |  | 完成时间 |
| `repaired_by` | VARCHAR(64) |  |  | 维修人 |
| `parts_used` | VARCHAR(512) |  |  | 更换部件 |
| `result` | VARCHAR(1024) |  |  | 维修结果 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_repair_no`(tenant_id, repair_no);索引 `idx_equipment_time`(tenant_id, equipment_id, started_at)

#### `mes_equipment_daily_stat` 设备日统计
所属模块:`app-mes-maint`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `stat_date` | DATE | 是 |  | 统计日期 |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `productive_sec` | BIGINT | 是 | 0 | PRODUCTIVE 秒数 |
| `standby_sec` | BIGINT | 是 | 0 | STANDBY 秒数 |
| `engineering_sec` | BIGINT | 是 | 0 | ENGINEERING 秒数 |
| `sched_down_sec` | BIGINT | 是 | 0 | SCHEDULED_DOWN 秒数 |
| `unsched_down_sec` | BIGINT | 是 | 0 | UNSCHEDULED_DOWN 秒数 |
| `non_sched_sec` | BIGINT | 是 | 0 | NON_SCHEDULED 秒数 |
| `lot_count` | INT | 是 | 0 | 加工 Lot 数 |
| `wafer_count` | INT | 是 | 0 | 加工 Wafer 数 |
| `fault_count` | INT | 是 | 0 | 异常次数 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_date_equipment`(tenant_id, stat_date, equipment_id)

#### `mes_stocktake` 盘点单
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `stocktake_no` | VARCHAR(64) | 是 |  | 盘点单号 |
| `wh_id` | BIGINT | 是 |  | 库房 |
| `location_id` | BIGINT |  |  | 库位,空表示整库 |
| `status` | VARCHAR(16) | 是 | DRAFT | DRAFT/COUNTING/REVIEWING/POSTED/CANCELLED |
| `planned_at` | DATETIME |  |  | 计划时间 |
| `counted_by` | VARCHAR(64) |  |  | 盘点人 |
| `approved_by` | VARCHAR(64) |  |  | 审批人 |
| `posted_at` | DATETIME |  |  | 过账时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_stocktake_no`(tenant_id, stocktake_no);索引 `idx_wh_status`(tenant_id, wh_id, status)

#### `mes_stocktake_item` 盘点明细
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `stocktake_id` | BIGINT | 是 |  | 盘点单 |
| `material_lot_id` | BIGINT | 是 |  | 物料批 |
| `location_id` | BIGINT | 是 |  | 库位 |
| `book_qty` | DECIMAL(18,4) | 是 |  | 账面数量 |
| `count_qty` | DECIMAL(18,4) |  |  | 实盘数量 |
| `diff_qty` | DECIMAL(18,4) |  |  | 差异数量 |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `reason` | VARCHAR(256) |  |  | 差异原因 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_stocktake_lot_loc`(tenant_id, stocktake_id, material_lot_id, location_id)

#### `mes_dashboard` 自定义看板
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `dashboard_code` | VARCHAR(64) | 是 |  | 看板编码 |
| `dashboard_name` | VARCHAR(128) | 是 |  | 看板名称 |
| `owner` | VARCHAR(64) |  |  | 所有者 |
| `is_public` | TINYINT(1) | 是 | 0 | 是否公开 |
| `layout_json` | JSON | 是 |  | 布局与组件配置 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_dashboard_code`(tenant_id, dashboard_code)

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/defect-records`、`/ncrs`、`/mrbs` CRUD;POST `/ncrs/{id}/submit-mrb`、`/mrbs/{id}/decide`、`/ncrs/{id}/close`
- GET `/api/v1/mes/yield/lots/{id}`、`/yield/wafers/{id}`;`/wafer-marks`、`/wafer-sort-results` CRUD
- `/api/v1/mes/pm-plans`、`/pm-orders`、`/repair-records` CRUD;POST `/pm-orders/{id}/start`、`/complete`、`/qual`
- GET `/api/v1/mes/equipment/{id}/e10-stats`、`/equipment/oee`
- `/api/v1/mes/stocktakes` CRUD;POST `…/{id}/count`、`/approve`、`/post`
- `/api/v1/mes/dashboards` CRUD
- GET `/api/v1/mes/reports/due-date-fulfillment`、`/reports/material-consumption`、`/reports/route-change-impact`;GET `/api/v1/mes/genealogy/report?type=&id=` 追溯报告导出

## 6. 验收标准

1. 不合格从发现到处置闭环,Lot 状态与历史一致
2. PM 到期自动生成工单,Qual 未通过设备不可恢复可用
3. 盘点差异审批过账后库存与流水平衡
4. E10 各状态占比与状态历史累计时长一致
5. PM 计数在出站撤销后回退,同一事件重复投递不重复计数;PM 完成后再撤销旧出站不产生负数
6. 同一 Lot 两张 NCR 各自 Hold,关闭一张不放行另一张;子 Lot 上复制的 NCR Hold 随 NCR 关闭释放
7. 盘点期间被锁库位不能领退料

## 7. 待确认

1. OEE 的性能率是否需要?(需要标准节拍数据,独立运行下需手工维护)
2. MRB 是否需要会议纪要/附件?当前仅记录结论与备注。

已决定:PM 计数采用事件异步(2026-10-16 确认),不要求"第 N 次出站即拦截下一次进站"。
