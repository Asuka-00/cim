# M4 执行扩展

> 状态:**设计草案,待核对**(未实现)· 依赖:M3 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_4__mes_m4.sql`)

## 1. 目标

补齐日常生产必需的 Lot/Wafer 操作:拆批合批、返工、跳站、改 Route、纠错、预约动作、Wafer 级 Track、Carrier,以及 WIP 看板和作业清单排序。纠错事务的补偿规则在本里程碑定型,并随后续里程碑逐步追加补偿处理器。

## 2. 范围

- Split/Merge/Bonus/Ship/Terminate/Skip、Wafer 回收
- Rework 分支与条件跳转
- 在制 Lot 改 Route/产品版本
- 纠错事务:Cancel Track-In/Undo Track-Out/Move Back/Force Move/Unscrap
- Future Action 预约动作
- Wafer 级 Track、Slot Map、Wafer 用途与 Dummy 管控
- Carrier 台账与绑定
- 工单拆分合并与进度
- 作业清单排序规则、WIP 看板、Queue 视图

## 3. 设计规则

- **Split**:从父 Lot 选取 Wafer(或数量)拆出子 Lot;写 `hist_mes_lot_relation(SPLIT)`;父 Lot 数量与 Wafer 归属同步更新,Wafer 历史写 `SPLIT`。**起始单位守恒**:父 Lot 的 `initial_qty/scrap_qty` 按 Wafer 归属拆分给子 Lot,工单进度不重复计数。**子 Lot 继承规则**:产品版本/Route 版本/工单/优先级/Lot 类型照搬;当前步骤、`pass_no`、`mes_lot_step_pass` 全部复制;`step_enter_at`、`last_track_out_at` 照搬,M8 起 ACTIVE 的 Time Link 计时(`mes_lot_time_link_timer`)一并复制(**Queue Time/Time Link 不重新起算**);返工状态照搬;**ACTIVE 的 Hold 复制**到子 Lot(拆批不能绕过 Hold);`active_key` 按子 Lot 重写(键内含 lot_id,原样复制会撞唯一键),`source_ref_id` 保持不变;尚未触发的 Future Action 与抽检决定复制;`root_lot_id` 沿用;位置、载具不继承(需重新绑定)。Hold 期间允许 Split。**子 Lot 号** = `父 Lot 号 + "." + split_seq`(持父 Lot 行锁后取 `MAX+1`),显式 Split、Future Action SPLIT、Wafer 级 Track 隐式 Split 同一规则,不经编号规则取号(M0)。
- **Merge**:只允许:同工单、同产品版本、同 Route 版本、同当前步骤、`mes_lot_step_pass` 一致、同返工状态、同计数单位、状态 WAITING、均无 Hold、均不在 Batch 中;满足后 Wafer 迁移到目标 Lot,源 Lot 的 `initial_qty/scrap_qty/qty/wafer_count` 转入目标 Lot 后**源 Lot 这些字段清零**(与 Split 的转移语义一致,工单汇总与开批余量 `SUM(initial_qty)` 因此不重复计算),源 Lot 置 `MERGED`(不是 TERMINATED),写 `hist_mes_lot_relation(MERGE)`;源 Lot 上 PENDING 的 Future Action 置 `CANCELLED` 并在结果中列出,ACTIVE 的 Time Link 计时(M8)取两者中较早的起点保留到目标 Lot。
- **Rework**:`mes_route_rework`(base,随 Route 版本复制、生效只读)定义在哪些主流程步骤范围内可进入返工、返工步骤序列、回归主流程步骤、次数上限。进入返工写 `ENTER_REWORK` 事务,Lot 的 `rework_id/rework_seq_no` 有值;返工步骤走完回到 `return_seq_no`。**返工次数不存计数器**,按 `(root_lot_id, rework_id)` 从历史统计(经 `hist_mes_txn_reversal` 排除已撤销事务),因此拆批不能绕过 `max_count`。
- **返工分支内的约束口径**:BOM 行按 `step_id` 匹配 `route_seq_no=0` 的行(返工步骤同样消耗物料);Time Link、Future Action、条件跳转、抽检跳站只作用于**主流程步骤序号**,返工步骤不适用;返工步骤有自己的 `recipe_id/equipment_group_id/queue_time_max_min`。
- **Skip/Force Move**:Skip 跳到下一步骤,需权限与原因(`mes.step.skip.allowed`);Force Move 可跳到 Route 上任意步骤,记录跳过的步骤范围,Interlock 不校验被跳过站点。两者都不执行被跳过步骤的物料反冲,也不增加其 `pass_count`。
- **到站统一经 ArrivalHook**(M3 定义):Skip、Force Move、Move Back、改 Route、返工进入/回归、Split 子 Lot 到站都会设置 `step_enter_at` 并触发 `ON_ARRIVE` 的 Future Action,不能借这些操作绕过预约 Hold。
- **纠错事务通则**:① 只允许撤销该对象**最后一笔有效事务**(无后续有效事务依赖);② 都需权限与原因,二次确认(M8),写 `ref_txn_id` 并在 `hist_mes_txn_reversal` 登记;③ 每类事务登记一个**补偿处理器**,在同一数据库事务内回滚其全部副作用;④ 状态恢复依据历史表的 `before_json` 快照;⑤ **不可逆事务**:单位转换、形态转换、出货、**显式** Split/Merge,以及**执行过 Future Action SPLIT 的 Track-Out**;⑥ 设备状态补偿改为**追加新状态段**,且仅当当前段仍由本事务产生(`hist_mes_equipment_status.txn_id`)时才执行,否则只告警不改。
- **补偿处理器清单**(M4 起逐里程碑追加):`TRACK_IN`/`TRACK_OUT`:Lot 状态与位置/步骤/计时、`mes_lot_step_pass` 回减、设备状态(追加段)、Track-Out 时的 Wafer 报废、条件/返工位置、**Wafer 级 Track 的隐式 Split**(随 Cancel Track-In 补偿)与自动 Merge、Future Action 的 HOLD/CHANGE_PRIORITY/CHANGE_RECIPE、工单进度事件;追溯关系无需补偿处理(撤销登记后查询自动排除,再次发生时按 M3 规则改指向);M5 追加 Batch 成员(剔除时由剔除事务一并处理);M8 追加工具使用回退(`USE_REVERSAL`)、Cancel Track-In 的工具解绑(`UNBIND`)与 Time Link 计时恢复;M9 追加物料反冲回补(按原行加修正链的净消耗);M10 追加:撤销出站时作废本次出站写入的抽检决定(抽检计数不回退),采集录入是独立的 `COLLECT` 事务,由它自己的补偿处理器作废记录并释放其产生的 OOS Hold;M13 的 PM 计数不在补偿处理器内(由 maint 订阅 `LOT_TRACK_OUT` 与 `TXN_REVERSED` 异步增减,见 M13)。
- **Cancel Track-In**:RUNNING→WAITING,清空设备/Recipe/计时,设备状态按追加段补偿。**Undo Track-Out**:按被撤销出站的 `before_json` 恢复到出站前:回到原步骤并置 `RUNNING`(含最后一站的 COMPLETED→RUNNING),重新关联原设备/Port/Recipe/Track-In 计时;持设备行锁后校验并发上限(超限则拒绝撤销),设备状态按通则⑥追加段;`pass_count` 回减(它取消的就是那一次经过)。撤销后最后一笔有效事务回到 Track-In,可继续 Cancel Track-In 或重新出站。**Move Back**:回到此前步骤重走,**不回减 `pass_count`**(计数表示物理经过次数,重走得到新的 `pass_no`,采集结果与抽检决定因此不冲突)。**Unscrap**:恢复 Wafer,数量回加。
- **Future Action**:对指定 Lot 在指定主流程步骤自动执行 HOLD/SPLIT/CHANGE_RECIPE/CHANGE_PRIORITY/NOTIFY。`timing=ON_ARRIVE` 由到站钩子在**上一站 Track-Out 事务内到站时**触发(不在 Track-In 事务里);`AFTER_TRACK_OUT`:本步骤出站后。**预约绑定 `route_version_id + route_seq_no`**:改 Route/产品版本时,旧版本上 PENDING 的预约置 `CANCELLED` 并在改 Route 结果中列出,需要时按新版本重新预约。**失败处理**:动作在同一事务的 `NESTED` 保存点内执行,失败回滚到保存点后由外层事务写 `FAILED` 并自动加 Hold(`hold_source=FUTURE_ACTION`);不使用 REQUIRES_NEW(外层已持有 Lot 行锁)。`CHANGE_RECIPE` 写入 `mes_lot.recipe_override_id/recipe_override_seq_no`,Track-In 的 `RECIPE_ALLOWED` 规则据此只允许该 Recipe。
- **Wafer 级 Track**:Track-In/Out 可选部分 Wafer;未选中的 Wafer 留在原步骤,实际是隐式 Split(子 Lot 自动生成并继承 `root_lot_id`)。**自动 Merge 在到站钩子里触发**:当任一同 `root_lot_id` 的兄弟 Lot 到达同一步骤、均 WAITING、无 Hold、满足 Merge 全部约束时自动并回(参数 `mes.wafer.track.automerge`,默认 true);条件不满足则保留为独立 Lot,待人工 Merge。受 `mes.wafer.track.enabled` 开关控制。
- **Slot Map**:Wafer 的 `slot_no` 变更写 `hist_mes_slot_change`,同一 Lot 内 Slot 唯一(应用层校验)。
- **Dummy 管控**:`purpose=DUMMY` 的 Wafer 每次 Track-Out 使 `dummy_use_count+1`,达 `max_use_count`(或全局 `mes.dummy.max.reuse`)后禁止再使用,提示报废。
- **Carrier 与 Lot 绑定的单一事实来源**:`mes_carrier.current_lot_id`;`mes_lot.carrier_code` 仅为展示冗余。一个载具同时只绑定一个 Lot(Batch 内各 Lot 各自保留载具)。可复用载具类型限于 FOUP/CASSETTE/FRAME/MAGAZINE;出货包装的 Tray/Reel/Tube/Box 属于 `mes_pack_container`(M6),不在此表。**FRAME 逐片对应 Wafer**(M6 `mes_wafer.frame_carrier_id`):同一 Lot 可有多个 Frame,`current_lot_id` 随所载 Wafer 的 Lot 更新并写绑定历史,`mes_lot.carrier_code` 对 Frame 不填。绑定/解绑写 `hist_mes_carrier_binding`。
- **Wafer 回收**:Lot 终止或 Wafer 报废后可回收再利用:原 Wafer 置 `RECLAIMED`,再入厂时生成新的 `wafer_code` 的 Wafer,`recycled_from_wafer_id` 指向原 Wafer,`vendor_wafer_id` 保持。
- **Bonus**:从来料物料批(晶圆)补扣并新建 Wafer 归入该 Lot,要求同产品、Lot 处于在制态;工单 `started_qty` 通过进度事件增加;M9 起扣减来料库存。
- **作业清单排序**:`mes_dispatch_rule.sort_keys` 为有序数组,如 `[{"key":"IS_HOT","dir":"DESC"},{"key":"PRIORITY","dir":"ASC"},{"key":"QUEUE_URGENCY","dir":"DESC"},{"key":"STEP_ENTER_AT","dir":"ASC"}]`,可限定到站点/设备组/区域;无匹配时用 `is_default` 规则。
- **工单拆分合并**:拆分按数量生成子工单(`parent_wo_id`),未开批数量才可拆;合并仅限同产品版本、均未开批。
- **Hold 超时提醒**:`mes_hold_reason.timeout_minutes` 到期,由租户遍历的定时任务产生 `HOLD_TIMEOUT` 预警(`mes_alert`)。

## 4. 表设计

### 4.1 新增表(12 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `hist_mes_lot_relation` | Lot 关系 | `app-mes-exec` |
| `mes_route_rework` | Route 返工分支 | `app-mes-base` |
| `mes_route_rework_step` | Route 返工步骤 | `app-mes-base` |
| `mes_route_condition` | Route 条件跳转 | `app-mes-base` |
| `mes_future_action` | 预约动作 | `app-mes-exec` |
| `mes_carrier` | 载具(可复用的过程载具) | `app-mes-exec` |
| `hist_mes_carrier_binding` | 载具绑定历史(时间段表) | `app-mes-exec` |
| `hist_mes_slot_change` | Slot 变更历史 | `app-mes-exec` |
| `mes_lot_shipment` | Lot 出货记录 | `app-mes-exec` |
| `hist_mes_lot_route_change` | Lot 工艺变更记录 | `app-mes-exec` |
| `mes_lot_note` | Lot 备注/标签 | `app-mes-exec` |
| `mes_dispatch_rule` | 作业清单排序规则 | `app-mes-exec` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `hist_mes_lot_relation` Lot 关系
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `parent_lot_id` | BIGINT | 是 |  | 父 Lot / 源 Lot |
| `child_lot_id` | BIGINT | 是 |  | 子 Lot / 目标 Lot |
| `relation_type` | VARCHAR(16) | 是 |  | SPLIT/MERGE/BONUS/REWORK_SPLIT/PACKAGE_INPUT/SPLIT_BY_BIN |
| `qty` | BIGINT |  |  | 涉及数量 |
| `unit_id` | BIGINT |  |  | 计数单位 |
| `txn_id` | BIGINT |  |  | 事务ID |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_parent`(tenant_id, parent_lot_id);索引 `idx_child`(tenant_id, child_lot_id)

#### `mes_route_rework` Route 返工分支
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `rework_code` | VARCHAR(64) | 是 |  | 返工分支编码 |
| `rework_name` | VARCHAR(128) | 是 |  | 返工分支名称 |
| `entry_from_seq` | INT | 是 |  | 允许进入的主流程步骤范围起 |
| `entry_to_seq` | INT | 是 |  | 允许进入的主流程步骤范围止 |
| `return_seq_no` | INT | 是 |  | 返工完成后回到的主流程步骤序号 |
| `max_count` | INT | 是 | 1 | 同一 Lot 在该分支的最大返工次数 |
| `reason_code` | VARCHAR(64) |  |  | 默认返工原因码 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rework_code`(tenant_id, route_version_id, rework_code)

说明:属于 Route 版本的从属数据:版本复制时一并复制,版本生效后只读。

#### `mes_route_rework_step` Route 返工步骤
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `rework_id` | BIGINT | 是 |  | 返工分支 |
| `seq_no` | INT | 是 |  | 返工内序号 |
| `step_id` | BIGINT | 是 |  | 站点 |
| `is_required` | TINYINT(1) | 是 | 1 | 必经 |
| `recipe_id` | BIGINT |  |  | 步骤指定 Recipe |
| `equipment_group_id` | BIGINT |  |  | 步骤指定设备组 |
| `count_unit_id` | BIGINT |  |  | 计数单位 |
| `queue_time_max_min` | INT |  |  | 进入本站点到 Track-In 最长等待(分钟) |
| `queue_time_action` | VARCHAR(16) |  |  | 超时动作 HOLD/WARN |
| `std_process_min` | DECIMAL(10,2) |  |  | 标准加工时长(分钟) |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rework_seq`(tenant_id, rework_id, seq_no)

#### `mes_route_condition` Route 条件跳转
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `from_seq_no` | INT | 是 |  | 在哪个主流程步骤 Track-Out 后判定 |
| `to_seq_no` | INT | 是 |  | 满足条件跳转到的步骤序号 |
| `condition_type` | VARCHAR(24) | 是 |  | LOT_TYPE/QTY/COLLECT_RESULT/FLAG/PRODUCT_ATTR |
| `condition_json` | JSON | 是 |  | 条件表达式 {"op":"EQ","field":"…","value":"…"} |
| `priority` | INT | 是 | 0 | 多条件命中时数小优先 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_from`(tenant_id, route_version_id, from_seq_no)

说明:属于 Route 版本的从属数据,随版本复制,生效后只读。

#### `mes_future_action` 预约动作
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `route_version_id` | BIGINT | 是 |  | 预约时的 Route 版本,改 Route 后旧版本预约取消 |
| `route_seq_no` | INT | 是 |  | 触发的主流程步骤序号 |
| `pass_no` | INT |  |  | 指定第几次经过触发,空表示首次到达 |
| `timing` | VARCHAR(24) | 是 | ON_ARRIVE | 触发时机 ON_ARRIVE(上一站出站事务内到站时)/AFTER_TRACK_OUT |
| `action_type` | VARCHAR(24) | 是 |  | HOLD/SPLIT/CHANGE_RECIPE/CHANGE_PRIORITY/NOTIFY |
| `action_params` | JSON |  |  | 动作参数 |
| `status` | VARCHAR(16) | 是 | PENDING | PENDING/DONE/FAILED/CANCELLED |
| `created_by_user` | VARCHAR(64) |  |  | 预约人 |
| `executed_at` | DATETIME |  |  | 执行时间 |
| `result_message` | VARCHAR(512) |  |  | 执行结果/失败原因 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_lot_status`(tenant_id, lot_id, status);索引 `idx_trigger`(tenant_id, route_seq_no, status)

#### `mes_carrier` 载具(可复用的过程载具)
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `carrier_code` | VARCHAR(64) | 是 |  | 载具编码 |
| `carrier_type` | VARCHAR(16) | 是 | FOUP | FOUP/CASSETTE/FRAME/MAGAZINE/OTHER |
| `capacity` | INT | 是 | 25 | 容量(Slot 数或件数) |
| `status` | VARCHAR(16) | 是 | EMPTY | EMPTY/OCCUPIED/CLEANING/MAINTENANCE/SCRAPPED |
| `current_lot_id` | BIGINT |  |  | 当前绑定 Lot(与 Lot 绑定关系的唯一事实来源) |
| `use_count` | INT | 是 | 0 | 累计使用次数 |
| `clean_cycle_uses` | INT |  |  | 清洗周期(次),空表示不管控 |
| `last_clean_at` | DATETIME |  |  | 最近清洗时间 |
| `location_text` | VARCHAR(128) |  |  | 位置 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_carrier_code`(tenant_id, carrier_code);索引 `idx_lot`(tenant_id, current_lot_id);索引 `idx_status`(tenant_id, status)

说明:出货包装容器(Tray/Reel/Tube/Box)不在此表,见 M6 `mes_pack_container`。

#### `hist_mes_carrier_binding` 载具绑定历史(时间段表)
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `carrier_id` | BIGINT | 是 |  | 载具 |
| `lot_id` | BIGINT | 是 |  | Lot |
| `bound_at` | DATETIME | 是 |  | 绑定时间 |
| `bound_by` | VARCHAR(64) |  |  | 绑定人 |
| `unbound_at` | DATETIME |  |  | 解绑时间 |
| `unbound_by` | VARCHAR(64) |  |  | 解绑人 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_carrier_time`(tenant_id, carrier_id, bound_at);索引 `idx_lot`(tenant_id, lot_id)

说明:时间段表:只允许回填解绑字段。

#### `hist_mes_slot_change` Slot 变更历史
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `wafer_id` | BIGINT | 是 |  | Wafer |
| `carrier_id` | BIGINT |  |  | 载具 |
| `slot_before` | INT |  |  | 变更前 Slot |
| `slot_after` | INT |  |  | 变更后 Slot |
| `reason` | VARCHAR(256) |  |  | 原因 |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot_time`(tenant_id, lot_id, occurred_at);索引 `idx_wafer`(tenant_id, wafer_id)

#### `mes_lot_shipment` Lot 出货记录
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `ship_no` | VARCHAR(64) | 是 |  | 出货单号 |
| `lot_id` | BIGINT | 是 |  | Lot |
| `wo_id` | BIGINT |  |  | 工单 |
| `customer_id` | BIGINT |  |  | 客户 |
| `qty` | BIGINT | 是 |  | 出货数量 |
| `unit_id` | BIGINT | 是 |  | 出货计数单位 |
| `shipped_at` | DATETIME | 是 |  | 出货时间 |
| `shipped_by` | VARCHAR(64) |  |  | 出货人 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_ship_no`(tenant_id, ship_no);索引 `idx_lot`(tenant_id, lot_id);索引 `idx_wo`(tenant_id, wo_id)

#### `hist_mes_lot_route_change` Lot 工艺变更记录
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `from_product_version_id` | BIGINT |  |  | 原产品版本 |
| `to_product_version_id` | BIGINT |  |  | 新产品版本 |
| `from_route_version_id` | BIGINT |  |  | 原 Route 版本 |
| `to_route_version_id` | BIGINT |  |  | 新 Route 版本 |
| `from_seq_no` | INT |  |  | 变更前步骤序号 |
| `to_seq_no` | INT |  |  | 变更后步骤序号 |
| `reason_code` | VARCHAR(64) |  |  | 原因码 |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `txn_id` | BIGINT |  |  | 事务ID |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot`(tenant_id, lot_id, occurred_at)

#### `mes_lot_note` Lot 备注/标签
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `note_type` | VARCHAR(16) | 是 | NOTE | NOTE/WARNING/ENGINEERING/TAG |
| `content` | VARCHAR(1024) | 是 |  | 内容(TAG 类型为标签文本) |
| `pinned` | TINYINT(1) | 是 | 0 | 是否置顶/在终端醒目显示 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_lot`(tenant_id, lot_id)

#### `mes_dispatch_rule` 作业清单排序规则
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `rule_code` | VARCHAR(64) | 是 |  | 规则编码 |
| `rule_name` | VARCHAR(128) | 是 |  | 规则名称 |
| `scope_type` | VARCHAR(16) | 是 | GLOBAL | 适用范围 GLOBAL/STEP/EQUIPMENT_GROUP/AREA |
| `scope_id` | BIGINT | 是 | 0 | 范围对象ID,GLOBAL 时为 0 |
| `sort_keys` | JSON | 是 |  | 排序键数组 [{"key","dir"}] |
| `is_default` | TINYINT(1) | 是 | 0 | 默认规则 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rule_code`(tenant_id, rule_code);索引 `idx_scope`(tenant_id, scope_type, scope_id)

### 4.2 变更表(4 处)

#### 变更 `mes_lot` Lot

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `parent_lot_id` | BIGINT |  |  | 父 Lot(Split 产生的子 Lot) |
| `root_lot_id` | BIGINT |  |  | 根 Lot |
| `split_seq` | INT |  |  | 拆批序号 |
| `rework_id` | BIGINT |  |  | 所在返工分支,空表示主流程 |
| `rework_seq_no` | INT |  |  | 返工分支内步骤序号 |
| `recipe_override_id` | BIGINT |  |  | Future Action CHANGE_RECIPE 指定的 Recipe |
| `recipe_override_seq_no` | INT |  |  | 该指定生效的主流程步骤序号 |

新增索引:索引 `idx_parent`(tenant_id, parent_lot_id);索引 `idx_root`(tenant_id, root_lot_id)

#### 变更 `mes_wafer` Wafer

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `max_use_count` | INT |  |  | Dummy/Monitor 最大使用次数,空表示取全局参数 |

#### 变更 `mes_hold_reason` Hold 原因码

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `timeout_minutes` | INT |  |  | Hold 超时提醒阈值(分钟) |

#### 变更 `mes_work_order` 工单

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `parent_wo_id` | BIGINT |  |  | 父工单(拆分产生) |
| `merged_into_wo_id` | BIGINT |  |  | 已合并到的工单 |

新增索引:索引 `idx_parent`(tenant_id, parent_wo_id)

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- POST `/api/v1/mes/lots/{id}/split`、`/merge`、`/bonus`、`/ship`、`/terminate`、`/skip`、`/route-change`
- POST `/api/v1/mes/lots/{id}/rework`(进入返工)、`/rework/exit`
- POST `/api/v1/mes/lots/{id}/cancel-track-in`、`/undo-track-out`、`/move-back`、`/force-move`
- POST `/api/v1/mes/wafers/{id}/unscrap`、`/reclaim`;POST `/api/v1/mes/lots/{id}/slot-map`
- POST `/api/v1/mes/lots/batch-actions`(批量 Hold/Release/改优先级,逐 Lot 独立事务,返回逐条结果)
- `/api/v1/mes/lots/{id}/future-actions`、`/lots/{id}/notes` CRUD
- `/api/v1/mes/carriers` CRUD;POST `…/{id}/bind`、`/unbind`
- `/api/v1/mes/dispatch-rules` CRUD;GET `/api/v1/mes/worklist?ruleId=`
- GET `/api/v1/mes/wip/board`、`/wip/queues`;POST `/api/v1/mes/work-orders/{id}/split`、`/merge`
- `/api/v1/mes/route-versions/{vid}/reworks`、`/conditions` CRUD(随 Route 版本只读规则)

## 6. 验收标准

1. 拆批合批后父子关系、Wafer 归属、数量一致,合批后源 Lot 数量为 0、工单数量不重复;拆出的子 Lot 继承 Hold 与计时,无法借拆批绕过 Hold
2. 返工入口、返工步骤、回归步骤按定义执行,达次数上限被拒绝;ACTIVE 的 Route 版本不能再改返工定义
3. 误 Track-In/Track-Out 可回退:Lot、`pass_count`、设备状态、Wafer 报废、工单进度全部一致;已有后续事务依赖时被拒绝;最后一站出站撤销后 Lot 回到 RUNNING,可再 Cancel Track-In
4. 单位转换/出货/显式拆合批/含 Future Action SPLIT 的出站不能被撤销;同一事务不能被撤销两次
5. 子 Lot 拆出后再次进入返工,次数按 root 累计,无法借拆批绕过上限
6. Move Back 重走同一站点后,必采校验要求重新录入采集结果
7. 预约 Hold 在到站时自动触发;触发失败不影响上一站 Track-Out,Lot 被自动 Hold
8. 部分 Wafer 进出站后,Lot 与子 Lot 的数量、位置、历史正确
9. WIP 看板数量与 Lot 列表按站点汇总一致

## 7. 待确认

1. Wafer 级 Track 的"自动 Merge 回主 Lot"是否默认开启?当前设计默认开启(`mes.wafer.track.automerge`)。
