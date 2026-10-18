# M3 执行核心

> 状态:**设计草案,待核对**(未实现)· 依赖:M2 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_3__mes_m3.sql`)

## 1. 目标

人工操作下完成 Lot 从开批到流转、Hold、报废的最小闭环:工单→开批→Track-In/Out→Hold/Release→Scrap,含操作员终端与作业清单。同时落地 Lot/Wafer 状态机、经过次数计数与追溯链路(写入侧),以及 Interlock 引擎骨架。

## 2. 范围

- 工单创建/下达/关闭/取消
- Lot 类型、开批、Wafer 管理
- Track-In/Track-Out(Interlock 引擎骨架 + 内置规则)
- Hold/Release(多重 Hold、Release 角色)
- Scrap(Lot/Wafer 级)
- Lot/Wafer 历史、追溯链路写入
- 经过次数计数、下一步解析链
- 设备状态随 Track 联动、设备同时加工上限
- 作业清单、Lot/Wafer 列表、操作员终端 P0

## 3. 设计规则

- **Lot 状态**(`mes_lot.status`):`WAITING`(等待下一次 Track-In)、`RUNNING`(加工中)、`COMPLETED`(Route 走完)、`SHIPPED`(已出货,M4)、`SCRAPPED`(全部报废)、`TERMINATED`(终止,M4)、`MERGED`(已并入其他 Lot,M4)、`CONSUMED`(已被封装 Lot 投入耗尽,M6)。`MERGED/CONSUMED` 与 `TERMINATED` 分开,避免终止统计被污染。
- **终态与迁移矩阵**:**在制态** = {WAITING, RUNNING};**完成态** = {COMPLETED, SHIPPED, CONSUMED}(计入工单完成);**关闭态** = {SCRAPPED, TERMINATED, MERGED}(不计入完成,口径见 M6)。完成态与关闭态合称"终态",工单完工判定以此为准。允许的迁移:WAITING→RUNNING(Track-In)、RUNNING→WAITING(Track-Out 未走完 / Cancel Track-In)、RUNNING→COMPLETED(最后一站 Track-Out)、WAITING→COMPLETED(Skip/Force Move 越过最后一站)、COMPLETED→SHIPPED(Ship)、COMPLETED→RUNNING(Undo 最后一站 Track-Out,M4)、任意在制态→SCRAPPED(全部 Wafer 报废)、任意在制态→TERMINATED(Terminate)、WAITING→MERGED(被 Merge 的源 Lot)、WAITING→CONSUMED(被封装投入耗尽)。终态之后**只允许**:COMPLETED→SHIPPED、COMPLETED→RUNNING(Undo Track-Out,仅当该出站是最后一笔有效事务)、SCRAPPED→原在制态(Unscrap,按 `before_json` 恢复)与查询。`STORE`(Die Bank)站点上的 Wafer Lot 停在 WAITING,直至被投入封装耗尽。
- **到站钩子 ArrivalHook**:所有改变 Lot 当前步骤的事务(Track-Out、Skip、Force Move、Move Back、改 Route、进入/退出返工、Split 子 Lot、WIP 导入)统一调用:设置 `step_enter_at`、触发 `ON_ARRIVE` 的 Future Action、检查 Wafer 级 Track 子 Lot 的自动 Merge。不允许任何路径绕过该钩子。
- **Hold 是叠加标记不是状态**:`hold_count > 0` 即被 Hold;`mes_lot_hold` 每条记录一个原因,独立 Release,全部 Release 后 `hold_count=0` 才可继续作业。
- **Hold 期间允许的操作**:Release、Scrap、Split(MRB 处置需要,ACTIVE 的 Hold 复制到子 Lot)、Terminate(权限+原因)、改优先级、加备注;禁止 Track-In 与 Merge。若 Hold 发生在 RUNNING 期间,允许完成 Track-Out(设备已加工),Track-Out 后停在下一站点并保持 Hold。
- **Hold 幂等**:系统触发的 Hold(Interlock/OOS/Queue Time/Time Link/Future Action)写入 `active_key = lot|route_seq|source|reason`;**来源于单据的 Hold**(NCR)用 `lot|NCR|ncr_id`,同站点多张 NCR 互不合并,并在 `source_ref_id` 记录单据 ID,单据关闭时按 `(hold_source, source_ref_id)` 释放全部 Lot(含 Split 复制到子 Lot)上的 Hold;**持有 Lot 行锁后先查后插**,`mes_lot_hold.uk_active_key` 仅做兜底。手工 Hold 的 `active_key` 为 NULL,允许同原因多次(MySQL 唯一键允许多个 NULL)。
- **Wafer 状态**:`NORMAL`、`RUNNING`、`SCRAPPED`、`SPLIT_OUT`(拆出到子 Lot,M4)、`CONVERTED`(已转换为 Die,M6)、`RECLAIMED`(已回收,M4)。
- **加锁顺序**(M0 并发规则的具体化):**工单(仅开批/Bonus/工单拆分合并)→ 设备(含 Port)→ Lot → Wafer → 其他**;设备行是"设备并发数/设备状态联动/物料绑定剩余量"的互斥点。开批对工单行 `FOR UPDATE` 后,以实时 `SUM(mes_lot.initial_qty)` 校验余量(不读异步汇总值),防止并发超投;Track/Hold/Scrap 等 Lot 事务**不锁工单、不更新工单行**。
- **Track-In 事务**:① 校验(Interlock 引擎) → ② Lot `WAITING→RUNNING`,写 `equipment_id/port_id/recipe_version_id/track_in_at/track_in_by` → ③ 设备状态联动(见下) → ④ 写 `hist_mes_lot_history/hist_mes_transaction/hist_mes_genealogy_link`(Lot 级 Track 不逐片写 `hist_mes_wafer_history`,仅 Wafer 级 Track 写)。
- **Interlock 引擎骨架**(M3 即建成,M8 追加规则):`InterlockRule` 接口 + 注册表 + 严重度(BLOCK/WARN)+ 逐条结果汇总;BLOCK 时业务事务回滚,**回滚之后**用新事务写 `hist_mes_interlock_log`(M8 建表,M3 先写事务流水),此时外层行锁已释放。M3 内置规则(不可停用):`LOT_STATE`(状态 WAITING 且 `hold_count=0`)、`STEP_MATCH`(操作站点与 Lot 当前步骤一致)、`EQP_ALLOWED`(设备属于该步骤的设备组,能力匹配与有效期按 M2 规则)、`EQP_STATE`(设备状态非停机类,见联动放行规则)、`EQP_CAPACITY`(设备/Port 同时加工数未超上限,持锁后统计)、`RECIPE_ALLOWED`(Recipe 版本 ACTIVE 且在"步骤覆盖 → 站点+产品 → 站点"允许范围内)。`need_equipment=0`/`need_recipe=0` 的站点跳过对应规则。
- **Track-Out 事务**:① 录入良品数/报废 Wafer(见下"数量口径")、备注 → ② `mes_lot_step_pass` 中该 `(lot, step_id)` 的 `pass_count + 1` → ③ **下一步解析链**(M10 起在此做抽检决定并递增计数) → ④ 更新 Lot 位置/`step_enter_at`/`last_track_out_at`,到站时触发 Future Action(M4) → ⑤ M8 起写工具 `USE`、M9 起物料反冲(锁序见 M0) → ⑥ 设备状态联动 → ⑦ 写历史与追溯,并写 `LOT_TRACK_OUT` 与 `WORK_ORDER_PROGRESS` 事件(载荷与聚合键见 M0 事件目录);撤销本事务时重发 `WORK_ORDER_PROGRESS`。
- **下一步解析链**(可插拔,按优先级依次尝试,命中即停):返工分支内下一步/回归主流程(M4)→ 条件跳转(M4)→ 抽检跳站(M10)→ 默认下一个 `seq_no`。无下一步则 `COMPLETED`。各解析器在后续里程碑注册,M3 只有"默认下一步"。
- **经过次数(显式计数,不从历史推导)**:`mes_lot_step_pass(lot_id, step_id, pass_count)` 记录该 Lot 在某站点已 Track-Out 的次数;Lot 到达站点时 `pass_no = pass_count + 1`。**Split** 子 Lot 复制父 Lot 的计数;**Merge** 要求两个 Lot 对所有站点计数一致,否则拒绝;**WIP 初始化导入**必须提供各已过站点的计数(或由 Route 已过步骤推导,导入时校验);**Undo Track-Out** 回减。历史表的 `pass_no` 仅作展示。
- **数量口径**:Lot 当前计数单位下,"可逐个追踪个体"的单位(Wafer、Strip)必须指定具体个体:报废需选 Wafer(或 Strip),`qty` 由存活个体数推导,不允许只填良品数造成 `qty` 与 Wafer 实体不一致;只有不追踪个体的单位(Die、Unit)才允许直接录入数量。
- **设备状态联动**:开关 `mes.eqp.state.auto`(默认 true)。**Track-In** 仅在设备当前为 `STANDBY` 或 `status_source=TRACK` 时才置 `PRODUCTIVE`(Lot 类型映射见下)并标记 `status_source=TRACK`;设备处于停机/工程/人工设置的状态时(如 PM 后 Qual 的 Monitor Lot 进站)**保持原状态与原来源**,只记录 Lot 关联。**Track-Out** 仅当设备状态由 Track 联动设置、且设备上已无其他在制 Lot 时才回 `STANDBY`,人工设置的状态不被覆盖。Lot 类型映射:`ENGINEERING/MONITOR/TEST` 类 Lot 在 `STANDBY` 设备进站时置 `ENGINEERING`(参数 `mes.eqp.state.lottype.map` 可调)。
- **停机态放行**:`EQP_STATE` 规则默认拒绝 `SCHEDULED_DOWN/UNSCHEDULED_DOWN/NON_SCHEDULED`;但 `ENGINEERING` 与 `SCHEDULED_DOWN`(保养后 Qual)允许 `MONITOR/ENGINEERING/TEST` 类 Lot 进站,用于 PM 后验证(规则参数 `allow_lot_types`)。
- **Scrap**:Wafer 级报废置 `SCRAPPED` 并减 Lot 数量;Lot 级报废把所有未报废 Wafer 置 `SCRAPPED`;数量减到 0 时 Lot 置 `SCRAPPED`(发生过封装投入的源 Lot 除外,置 `CONSUMED`,见 M6)。必须选 `MES_SCRAP_REASON`。
- **工单进度汇总**:`mes_work_order` 的 `lot_count/started_qty/completed_qty/scrap_qty/terminated_qty` 由内部事件 `WORK_ORDER_PROGRESS` 异步重算(对工单行加锁后按 Lot 汇总),仅用于展示,**不在 Lot 事务内更新**。进度按工单起始单位(`qty_unit_id`)核算,`completed_qty` 只累计完成态 Lot(口径见 M6);**起始单位守恒**:Split 时 `initial_qty/scrap_qty` 随 Wafer 归属转移给子 Lot,Merge 时并入目标 Lot,Bonus 增加;转换后产生的 Die/Unit 级报废不计入工单报废。完工判定见 M6。
- **Lot 位置**:Track-In 置 `location_type=EQUIPMENT`;Track-Out 置 `AREA`(下一站点所在区域)。
- **对外契约**(`share.integration.mes.exec`):`WorkOrderProvider`(按 ID/工单号查工单、固化版本与状态,供 warehouse 领料单关联);`LotCommandProvider`(Hold/Release/Scrap,M4 追加 Rework/Split/Terminate;均经统一事务层,供 quality 的 MRB 处置调用)。
- **追溯写入**:开批、Track、Scrap 等事务都写 `hist_mes_genealogy_link`(M3 只写 Lot↔Wafer、Lot↔设备等基本关系;物料关系 M9、拆合批 M4 补充)。同一 `(parent, child, link_type, step)` 只记录一条(`INSERT … ON DUPLICATE KEY UPDATE`,见下),数量以各自业务表为准。查询页面在 M11。
- **追溯关系与撤销(去重后的口径)**:关系行只记"首次建立它的 `txn_id`",因此**不能**在查询时按 `txn_id` ∈ 撤销登记直接过滤——否则"撤销后又重新建立"的同一关系会被误藏。规则:① 写入用 `INSERT … AS new ON DUPLICATE KEY UPDATE txn_id = IF(EXISTS(SELECT 1 FROM hist_mes_txn_reversal r WHERE r.tenant_id = new.tenant_id AND r.orig_txn_id = hist_mes_genealogy_link.txn_id), new.txn_id, hist_mes_genealogy_link.txn_id)`(MySQL 8.0.19+ 行别名写法,子查询显式带 `tenant_id`),让被撤销的关系在再次发生时改指向新的有效事务;这是本表**唯一允许的回填**(仅 `txn_id`,仅在原事务已撤销时),与时间段表"只回填结束字段"同属只增表的受控例外,由 Mapper 上的专用 SQL 实现,不走通用 update;② 查询时仍按 `txn_id` ∈ `hist_mes_txn_reversal` 排除,此时被排除的只剩"撤销后未再发生"的关系。不用 `INSERT IGNORE`(会把截断等错误吞成警告)。

## 4. 表设计

### 4.1 新增表(9 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_hold_reason` | Hold 原因码 | `app-mes-exec` |
| `mes_work_order` | 工单 | `app-mes-exec` |
| `mes_lot` | Lot | `app-mes-exec` |
| `mes_lot_step_pass` | Lot 站点经过次数 | `app-mes-exec` |
| `mes_wafer` | Wafer | `app-mes-exec` |
| `mes_lot_hold` | Lot Hold 记录 | `app-mes-exec` |
| `hist_mes_lot_history` | Lot 事务历史 | `app-mes-exec` |
| `hist_mes_wafer_history` | Wafer 事务历史 | `app-mes-exec` |
| `hist_mes_genealogy_link` | 追溯关系 | `app-mes-exec` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_hold_reason` Hold 原因码
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `reason_code` | VARCHAR(64) | 是 |  | 原因码 |
| `reason_name` | VARCHAR(128) | 是 |  | 原因名称 |
| `hold_type` | VARCHAR(16) | 是 | OTHER | 类别 QUALITY/EQUIPMENT/PROCESS/ENGINEERING/MATERIAL/OTHER |
| `release_role_code` | VARCHAR(64) |  |  | 允许 Release 的角色编码,空表示任何有权限者 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_reason_code`(tenant_id, reason_code)

#### `mes_work_order` 工单
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wo_no` | VARCHAR(64) | 是 |  | 工单号 |
| `product_id` | BIGINT | 是 |  | 产品 |
| `product_version_id` | BIGINT | 是 |  | 产品版本 |
| `route_version_id` | BIGINT | 是 |  | 下工单时固化的 Route 版本 |
| `bom_version_id` | BIGINT |  |  | 下工单时固化的 BOM 版本 |
| `customer_id` | BIGINT |  |  | 客户 |
| `plan_qty` | BIGINT | 是 |  | 计划数量(起始计数单位) |
| `qty_unit_id` | BIGINT | 是 |  | 起始计数单位,工单进度均按此单位核算 |
| `priority` | TINYINT | 是 | 5 | 优先级 1(最高)–9(最低) |
| `due_date` | DATE |  |  | 交期 |
| `status` | VARCHAR(16) | 是 | CREATED | CREATED/RELEASED/IN_PROGRESS/COMPLETED/CLOSED/CANCELLED |
| `lot_count` | INT | 是 | 0 | 已开批数(异步汇总) |
| `started_qty` | BIGINT | 是 | 0 | 已投入数量(异步汇总) |
| `completed_qty` | BIGINT | 是 | 0 | 已完成数量(异步汇总) |
| `scrap_qty` | BIGINT | 是 | 0 | 已报废数量(异步汇总) |
| `terminated_qty` | BIGINT | 是 | 0 | 已终止 Lot 的剩余数量(异步汇总,不计入完成) |
| `released_at` | DATETIME |  |  | 下达时间 |
| `started_at` | DATETIME |  |  | 首次开批时间 |
| `completed_at` | DATETIME |  |  | 全部完成时间 |
| `closed_at` | DATETIME |  |  | 关闭时间 |
| `source` | VARCHAR(16) | 是 | MANUAL | 来源 MANUAL/IMPORT |
| `remark` | VARCHAR(512) |  |  | 备注 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_wo_no`(tenant_id, wo_no);索引 `idx_product`(tenant_id, product_id);索引 `idx_status_due`(tenant_id, status, due_date)

说明:进度字段由 `WORK_ORDER_PROGRESS` 事件异步汇总,不在 Lot 事务内更新。

#### `mes_lot` Lot
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_no` | VARCHAR(64) | 是 |  | Lot 号 |
| `lot_type` | VARCHAR(16) | 是 | PRODUCTION | PRODUCTION/ENGINEERING/MONITOR/DUMMY/CONTROL/TEST |
| `wo_id` | BIGINT |  |  | 工单,Monitor/Dummy 可无工单 |
| `product_id` | BIGINT | 是 |  | 产品 |
| `product_version_id` | BIGINT | 是 |  | 产品版本 |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `bom_version_id` | BIGINT |  |  | BOM 版本(沿用工单固化值) |
| `route_seq_no` | INT |  |  | 当前步骤序号,完成后保留最后一步 |
| `step_id` | BIGINT |  |  | 当前站点(冗余,用于作业清单) |
| `pass_no` | INT | 是 | 1 | 当前站点第 N 次经过 = mes_lot_step_pass.pass_count + 1 |
| `status` | VARCHAR(16) | 是 | WAITING | WAITING/RUNNING/COMPLETED/SHIPPED/SCRAPPED/TERMINATED/MERGED/CONSUMED |
| `hold_count` | INT | 是 | 0 | 未释放的 Hold 数 |
| `qty` | BIGINT | 是 | 0 | 当前数量(当前计数单位) |
| `qty_unit_id` | BIGINT | 是 |  | 当前计数单位 |
| `initial_qty` | BIGINT | 是 | 0 | 开批数量(起始计数单位) |
| `scrap_qty` | BIGINT | 是 | 0 | 累计报废数量(起始计数单位口径) |
| `current_scrap_qty` | BIGINT | 是 | 0 | 当前计数单位下的累计报废;单位转换时归零并存档到转换历史 |
| `wafer_count` | INT | 是 | 0 | 当前存活 Wafer 数 |
| `priority` | TINYINT | 是 | 5 | 优先级 1(最高)–9(最低) |
| `is_hot` | TINYINT(1) | 是 | 0 | Hot Lot |
| `equipment_id` | BIGINT |  |  | 当前加工设备(RUNNING 时) |
| `port_id` | BIGINT |  |  | 当前 Port/Chamber |
| `recipe_version_id` | BIGINT |  |  | 当前使用的 Recipe 版本 |
| `track_in_at` | DATETIME |  |  | 当前站点 Track-In 时间 |
| `track_in_by` | VARCHAR(64) |  |  | Track-In 操作人 |
| `step_enter_at` | DATETIME |  |  | 进入当前站点时间(Queue Time 起点) |
| `last_track_out_at` | DATETIME |  |  | 最近 Track-Out 时间(展示用;Time Link 计时见 M8 `mes_lot_time_link_timer`) |
| `carrier_code` | VARCHAR(64) |  |  | 载具编码(展示用冗余;事实来源是 mes_carrier.current_lot_id) |
| `location_type` | VARCHAR(16) |  |  | 位置类型 AREA/EQUIPMENT/RACK/OTHER |
| `location_id` | BIGINT |  |  | 位置对象ID |
| `location_text` | VARCHAR(128) |  |  | 位置文字描述(手工更新用) |
| `started_at` | DATETIME |  |  | 开批时间 |
| `finished_at` | DATETIME |  |  | 完成/报废/终止时间 |
| `remark` | VARCHAR(512) |  |  | 备注 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_lot_no`(tenant_id, lot_no);索引 `idx_wo`(tenant_id, wo_id);索引 `idx_step_status`(tenant_id, step_id, status);索引 `idx_equipment`(tenant_id, equipment_id);索引 `idx_product`(tenant_id, product_id);索引 `idx_status_priority`(tenant_id, status, priority, step_enter_at);索引 `idx_route_status`(tenant_id, route_version_id, status)

说明:在制 Lot 与历史 Lot 同表(D11 不做归档);作业清单走 `idx_step_status`,ECN/改 Route 影响分析走 `idx_route_status`。`equipment_id/port_id` 在 Track-Out 时必须清空,否则设备并发统计会扫到历史 Lot。

#### `mes_lot_step_pass` Lot 站点经过次数
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `step_id` | BIGINT | 是 |  | 站点 |
| `pass_count` | INT | 是 | 0 | 已 Track-Out 次数 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_lot_step`(tenant_id, lot_id, step_id)

说明:显式计数:Track-Out 加 1,Undo 减 1,Split 复制,Merge 校验一致,WIP 导入必填。返工分支内的站点计入同表(与主流程站点同 `step_id` 时累加)。

#### `mes_wafer` Wafer
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wafer_code` | VARCHAR(64) | 是 |  | WaferID(本厂内部 ID) |
| `lot_id` | BIGINT | 是 |  | 所属 Lot |
| `slot_no` | INT |  |  | Slot 号 |
| `status` | VARCHAR(16) | 是 | NORMAL | NORMAL/RUNNING/SCRAPPED/SPLIT_OUT/CONVERTED/RECLAIMED |
| `purpose` | VARCHAR(16) | 是 | PRODUCT | 用途 PRODUCT/MONITOR/DUMMY/CONTROL |
| `customer_id` | BIGINT |  |  | 来料客户 |
| `vendor_wafer_id` | VARCHAR(64) |  |  | 来料原始/客户 Wafer 编号,可与其他客户重复 |
| `recycled_from_wafer_id` | BIGINT |  |  | 回收再入厂时指向原 Wafer |
| `source_material_lot_id` | BIGINT |  |  | 来料物料批(M9 开批扣减时回填) |
| `dummy_use_count` | INT | 是 | 0 | Dummy 已使用次数 |
| `scrap_reason` | VARCHAR(64) |  |  | 报废原因码 |
| `scrapped_step_id` | BIGINT |  |  | 报废发生站点 |
| `scrapped_at` | DATETIME |  |  | 报废时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_wafer_code`(tenant_id, wafer_code);索引 `idx_lot_slot`(tenant_id, lot_id, slot_no);索引 `idx_status`(tenant_id, status);索引 `idx_vendor`(tenant_id, customer_id, vendor_wafer_id)

说明:`wafer_code` 为本厂内部唯一 ID(系统生成或按规则);客户给的编号放 `vendor_wafer_id`,不要求唯一,回收片再入厂生成新的 `wafer_code`。

#### `mes_lot_hold` Lot Hold 记录
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `reason_code` | VARCHAR(64) | 是 |  | Hold 原因码 |
| `hold_type` | VARCHAR(16) |  |  | 类别(取自原因码) |
| `hold_source` | VARCHAR(16) | 是 | MANUAL | 来源 MANUAL/INTERLOCK/OOS/QUEUE_TIME/TIME_LINK/FUTURE_ACTION/NCR/SYSTEM |
| `status` | VARCHAR(16) | 是 | ACTIVE | ACTIVE/RELEASED |
| `active_key` | VARCHAR(160) |  |  | 系统 Hold 的幂等键:lot\|seq\|source\|reason,单据来源为 lot\|NCR\|ncr_id;仅 ACTIVE 时有值,手工 Hold 与已释放为 NULL |
| `hold_step_id` | BIGINT |  |  | Hold 发生站点 |
| `hold_seq_no` | INT |  |  | Hold 发生步骤序号 |
| `hold_by` | VARCHAR(64) |  |  | Hold 操作人 |
| `hold_at` | DATETIME | 是 |  | Hold 时间 |
| `hold_remark` | VARCHAR(512) |  |  | Hold 备注 |
| `release_role_code` | VARCHAR(64) |  |  | 要求的 Release 角色(取自原因码,冗余) |
| `release_by` | VARCHAR(64) |  |  | Release 操作人 |
| `release_at` | DATETIME |  |  | Release 时间 |
| `release_remark` | VARCHAR(512) |  |  | Release 备注 |
| `hold_txn_id` | BIGINT |  |  | Hold 事务 |
| `release_txn_id` | BIGINT |  |  | Release 事务 |
| `source_ref_id` | BIGINT | 是 | 0 | 来源单据ID(NCR 等),0 表示无 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_active_key`(tenant_id, active_key);索引 `idx_lot_status`(tenant_id, lot_id, status);索引 `idx_status_time`(tenant_id, status, hold_at);索引 `idx_source_ref`(tenant_id, hold_source, source_ref_id)

说明:同一 Lot 可有多条 ACTIVE 记录;`mes_lot.hold_count` 为其数量的冗余。Release 时 `active_key` 置 NULL。

#### `hist_mes_lot_history` Lot 事务历史
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `lot_no` | VARCHAR(64) | 是 |  | Lot 号(冗余) |
| `txn_id` | BIGINT |  |  | 事务ID(hist_mes_transaction) |
| `txn_type` | VARCHAR(32) | 是 |  | CREATE/TRACK_IN/TRACK_OUT/HOLD/RELEASE/SCRAP/PRIORITY/… |
| `route_version_id` | BIGINT |  |  | Route 版本 |
| `route_seq_no` | INT |  |  | 步骤序号 |
| `step_id` | BIGINT |  |  | 站点 |
| `pass_no` | INT |  |  | 第 N 次经过(展示用) |
| `rework_id` | BIGINT |  |  | 所在返工分支,空表示主流程 |
| `rework_seq_no` | INT |  |  | 返工分支内步骤序号 |
| `batch_id` | BIGINT |  |  | 所属 Batch(M5) |
| `equipment_id` | BIGINT |  |  | 设备 |
| `port_id` | BIGINT |  |  | Port/Chamber |
| `recipe_version_id` | BIGINT |  |  | Recipe 版本 |
| `qty_before` | BIGINT |  |  | 变更前数量 |
| `qty_after` | BIGINT |  |  | 变更后数量 |
| `unit_before_id` | BIGINT |  |  | 变更前计数单位 |
| `unit_after_id` | BIGINT |  |  | 变更后计数单位 |
| `status_before` | VARCHAR(16) |  |  | 变更前状态 |
| `status_after` | VARCHAR(16) |  |  | 变更后状态 |
| `reason_code` | VARCHAR(64) |  |  | 原因码 |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `remark` | VARCHAR(512) |  |  | 备注 |
| `before_json` | JSON |  |  | 变更前 Lot 关键字段快照(位置/步骤/计时/返工/单位等);**仅对可撤销的事务类型写入**,纠错事务据此恢复 |
| `extra_json` | JSON |  |  | 扩展信息 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot_time`(tenant_id, lot_id, occurred_at);索引 `idx_step_type_time`(tenant_id, step_id, txn_type, occurred_at);索引 `idx_equipment_time`(tenant_id, equipment_id, occurred_at);索引 `idx_txn`(tenant_id, txn_id);索引 `idx_batch`(tenant_id, batch_id)

说明:体量大表。按事务类型+时间的统计走 `idx_step_type_time`,不另建仅按类型的索引;`before_json` 只给可撤销类型写。

#### `hist_mes_wafer_history` Wafer 事务历史
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wafer_id` | BIGINT | 是 |  | Wafer |
| `wafer_code` | VARCHAR(64) | 是 |  | WaferID(冗余) |
| `lot_id` | BIGINT |  |  | 发生时所属 Lot |
| `lot_no` | VARCHAR(64) |  |  | Lot 号(冗余) |
| `txn_id` | BIGINT |  |  | 事务ID |
| `txn_type` | VARCHAR(32) | 是 |  | CREATE/TRACK_IN/TRACK_OUT/SCRAP/SPLIT/MERGE/SLOT_CHANGE/… |
| `route_seq_no` | INT |  |  | 步骤序号 |
| `step_id` | BIGINT |  |  | 站点 |
| `pass_no` | INT |  |  | 第 N 次经过(展示用) |
| `equipment_id` | BIGINT |  |  | 设备 |
| `slot_no_before` | INT |  |  | 变更前 Slot |
| `slot_no_after` | INT |  |  | 变更后 Slot |
| `status_before` | VARCHAR(16) |  |  | 变更前状态 |
| `status_after` | VARCHAR(16) |  |  | 变更后状态 |
| `reason_code` | VARCHAR(64) |  |  | 原因码 |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `remark` | VARCHAR(512) |  |  | 备注 |
| `before_json` | JSON |  |  | 变更前 Wafer 关键字段快照 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_wafer_time`(tenant_id, wafer_id, occurred_at);索引 `idx_lot_time`(tenant_id, lot_id, occurred_at);索引 `idx_txn`(tenant_id, txn_id)

说明:**Lot 级 Track 不逐片写入**(25 片/站会放大 25 倍):只写 Wafer 自身发生变化的事件(报废、拆合批、Slot 变更、Wafer 级 Track、转换、回收)。

#### `hist_mes_genealogy_link` 追溯关系
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `parent_type` | VARCHAR(16) | 是 |  | 上游对象类型 MATERIAL_LOT/LOT/WAFER/STRIP/CONTAINER/EQUIPMENT/TOOL |
| `parent_id` | BIGINT | 是 |  | 上游对象ID |
| `parent_code` | VARCHAR(64) |  |  | 上游对象编码(冗余) |
| `child_type` | VARCHAR(16) | 是 |  | 下游对象类型 |
| `child_id` | BIGINT | 是 |  | 下游对象ID |
| `child_code` | VARCHAR(64) |  |  | 下游对象编码(冗余) |
| `link_type` | VARCHAR(16) | 是 |  | CREATE/PROCESS/CONSUME/SPLIT/MERGE/CONVERT/PACK/OPEN_LOT |
| `step_id` | BIGINT | 是 | 0 | 发生站点,0 表示与站点无关 |
| `qty` | DECIMAL(18,4) |  |  | 首次记录时的数量(非累计,累计以业务表为准) |
| `unit_code` | VARCHAR(32) |  |  | 单位编码 |
| `equipment_id` | BIGINT |  |  | 发生设备 |
| `txn_id` | BIGINT |  |  | 首次建立该关系的事务ID |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:唯一 `uk_link`(tenant_id, parent_type, parent_id, child_type, child_id, link_type, step_id);索引 `idx_child`(tenant_id, child_type, child_id);索引 `idx_txn`(tenant_id, txn_id)

说明:关系表去重:同一对象对在同一类型与站点下只记一条,避免每次反冲/每片 Wafer 都写入造成膨胀。递归查询在应用层做(不用递归 CTE,便于限制深度与节点数)。

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/hold-reasons` CRUD
- `/api/v1/mes/work-orders` CRUD;POST `…/{id}/release`、`/close`、`/cancel`
- POST `/api/v1/mes/work-orders/{id}/lots` 开批(数量、WaferID 列表或自动生成)
- GET `/api/v1/mes/lots`、`/lots/{id}`、`/lots/{id}/history`、`/lots/{id}/wafers`
- POST `/api/v1/mes/lots/{id}/track-in`、`/track-out`、`/hold`、`/release`、`/scrap`、`/priority`
- POST `/api/v1/mes/wafers/{id}/scrap`;GET `/api/v1/mes/wafers`、`/wafers/{id}/history`
- GET `/api/v1/mes/worklist`(按设备/站点/区域的待加工队列)
- 终端:`/api/v1/mes/terminal/scan`(扫码解析对象)、`/terminal/lots/{no}/actions` 返回当前可执行动作

## 6. 验收标准

1. 一个 Lot 按 Route 跑完多个站点(含重复站点,`pass_no` 递增),并在第 2 次经过同一站点时使用该层指定的 Recipe
2. 中途两个独立原因同时 Hold,逐个 Release 后才可 Track-In;同一原因的系统 Hold 重复触发只产生一条
3. Hold 发生在加工中,Track-Out 后保持 Hold
4. 报废部分 Wafer 后数量与 Wafer 状态一致,历史完整;Wafer 单位下只填良品数被拒绝
5. 设备已满负荷时并发的 Track-In 只有一个通过;最后一个 Lot Track-Out 后设备回待机,人工置为停机的设备不被覆盖
6. 同一工单下多个 Lot 并发 Track-Out 不产生版本冲突,工单进度最终一致
7. 重复提交相同 request_id 只生效一次;并发 Track-In 同一 Lot 仅一个成功,另一个立即得到"对象被占用"
8. `need_equipment=0` 的站点(如目检)可不选设备完成 Track-In/Out
9. 设备处于 SCHEDULED_DOWN 时,Monitor Lot 进站不改变设备状态与来源;Track-Out 后设备仍为 SCHEDULED_DOWN
10. 并发开批不会超出工单计划量;Lot 事务并发不锁工单
11. 任何改变当前步骤的路径(Skip/Force Move/Move Back/改 Route)都触发到站钩子
12. Track-In 设备 E → Cancel Track-In → 再次 Track-In 设备 E 后,Lot↔E 的追溯关系仍可查到;只 Cancel 不再进站时查不到
13. 最后一站 Track-Out 后 Lot 直接为 COMPLETED;TERMINATED 的 Lot 不计入工单完成数
14. 每次 Track-Out(含 Batch 内每个 Lot)都产生一条 `LOT_TRACK_OUT` 事件

## 7. 待确认

1. `priority` 数值含义:建议 1(最高)–9(最低),默认 5。是否接受?
2. Lot 在 Hold 时是否允许修改 Lot 优先级/备注?当前设计允许。
