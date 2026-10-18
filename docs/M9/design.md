# M9 物料执行与自动反冲

> 状态:**设计草案,待核对**(未实现)· 依赖:M7、M8 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_9__mes_m9.sql`)

## 1. 目标

设备端物料绑定、按 BOM 自动反冲、开批扣减来料晶圆,消耗可追到 Lot/Wafer;提供低位/过期预警与手工修正。

## 2. 范围

- 设备物料位、上料/下料/换料
- 设备端剩余量/寿命递减
- 工单物料需求与齐套检查
- 开批扣减来料晶圆
- 物料类 Interlock
- Track-Out 自动反冲与消耗记录
- 低位/过期预警
- 手工修正、欠账清偿与替代料规则
- 开封/回温时效

## 3. 设计规则

- **库存经契约变动**:exec 的上料、直耗反冲、开批扣减、回补都调用 `share.integration.mes.warehouse.InventoryProvider`,在调用方业务事务内执行(同库同事务),exec 不直接写 warehouse 表;物料批 `IN_USE` 由 exec 在上料/下料/耗尽时调用 `recalcStatus` 并传入"是否仍有 ACTIVE 绑定",warehouse 不读 exec 表。
- **设备物料位**:`mes_equipment_slot` 定义设备上可装载物料的位置(药液桶、靶材位、气瓶位),可限定允许的物料类型或具体物料。
- **上料**:扫码物料批 + 选择槽位 → 校验(批 `AVAILABLE`、未过期、BOM 允许、槽位类型匹配)→ 从库存扣减装载量(`hist_mes_inventory_txn.txn_type=CONSUME`,`ref_type=BINDING`)→ 建 `mes_equipment_material_binding`(`ACTIVE`)。上料不把物料批置 `CONSUMED`(批状态由库存与绑定推导,见 M7)。同槽位已有 ACTIVE 绑定时,先下料(`UNMOUNTED`)。
- **下料剩余量处置**:下料时必须选处置 `unmount_disposition`:`RETURN_LINESIDE`(剩余量转回线边库位,写 `RETURN` 流水)或 `SCRAP`(报废,写 `SCRAP` 流水),并记录 `unmounted_remaining_qty`,保证账物一致。
- **反冲不足策略**(参数 `mes.backflush.shortage`):关键料 `BLOCK`——Track-Out 被拒绝;非关键料 `ALLOW`——放行,能扣的部分正常扣减,不足部分单独记一行欠账(`is_shortage=1`,`material_lot_id/binding_id/location_id` 均为 0,不扣库存与绑定),同时产生预警。
- **欠账清偿**:补料后执行 `SETTLE` 事务,指定物料批(及库位或绑定)扣减库存,并追加一对行:欠账维度的负数行与实际物料批的正数行(`source=SETTLE`,`adjust_ref_id` 指向欠账行;两行维度不同,不冲突),净消耗不变。某欠账行的未清偿量 = 原数量 − 指向它的 SETTLE 正数行之和,为 0 即清账。
- **防重复写入**:`hist_mes_material_consumption` 对 `(txn_id, bom_item_id, wafer_id, material_lot_id, binding_id, location_id)` 唯一,防止同一事务内对同一维度重复写入;重复提交由 M0 幂等键拦截。消耗记录为**只增表**:修正、Undo、欠账清偿各自经事务层取得独立 `txn_id`,只追加差额行。
- **剩余量递减**:反冲时优先从该设备、BOM 对应物料的 ACTIVE 绑定扣减;`remaining_qty ≤ 0` 置 `DEPLETED` 并产生内部事件;`remaining_qty ≤ low_threshold` 产生低位预警。
- **反冲计算**:按 BOM 行 `consume_mode` 与 Lot 当前计数单位换算——PER_WAFER × 本次 Track 的 Wafer 数;PER_DIE/PER_STRIP/PER_UNIT × 对应单位数量;PER_LOT × 1;PER_RUN × 1;PER_TIME × 加工分钟数(Track-Out 时间 − Track-In 时间)。Batch 出站时 PER_RUN/PER_TIME 只在 `seq` 最小的 `IN` 成员上扣一次(M5)。`scrap_rate` 不参与实际扣减,仅用于工单需求展开与差异判定。
- **直耗料**:BOM 行消耗物料不在设备端绑定时(如晶圆、封装料),反冲直接扣线边库存库位(按领料到线边的物料批先进先出),`binding_id=0`(该列非空,0 表示直耗线边)。
- **替代料**:BOM 行 `substitute_group` 同组内,按 `substitute_priority` 选可用物料;使用非首选替代时在消耗记录标记 `is_substitute=1`,按 BOM 行 `substitute_policy` 处理:`ALLOW` 放行、`WARN` 放行并产生预警、`BLOCK` 禁止。不设事后审批。
- **开批扣减来料晶圆**:开批请求指定来料物料批与数量;经 `InventoryProvider` 扣减库存,Wafer 回填 `source_material_lot_id`,写追溯链路 `OPEN_LOT`。晶圆物料批 1 片 = 库存 1 个"片"。若该物料批有来料 Wafer 清单(M7 `mes_incoming_wafer`),开批从清单选取(持清单行锁,要求 `AVAILABLE`),把 `vendor_wafer_id/customer_id` 写入新 Wafer,并经 `InventoryProvider` 把清单行置 `USED`、回填 `wafer_id`。
- **工单物料需求**:下达工单时按 BOM 展开写 `mes_wo_material_requirement`。仅 `PER_WAFER/PER_DIE/PER_STRIP/PER_UNIT` 行可由 `plan_qty` 精确换算:`required_qty = plan_qty × qty_per × (1+scrap_rate) × 该站点在 Route 中的出现次数(route_seq_no=0 时)`;`PER_LOT/PER_RUN/PER_TIME` 行无法由数量推算,仅登记不参与齐套检查。齐套检查以线边库存+库房可用量为准,缺料提示不阻断(参数 `mes.wo.kitting.block` 控制是否阻断下达)。
- **手工修正**:反冲量与实际不符时,申请修正(`mes_consumption_adjust`)经审批后,在消耗表**追加**一条差额行(`qty = qty_after − qty_before`,`source=ADJUST`,`adjust_ref_id` 指向原记录,自己的 `txn_id`),并同步库存/绑定剩余量;原记录不改。原记录的绑定已下料且剩余量转回线边时,差额改记到该线边库位(`binding_id=0`,`location_id` 为该库位);绑定已按 `SCRAP` 处置时拒绝修正。
- **开封/回温**:对 `open_life_hours`、`thaw_minutes` 非空的物料,开封/出库时记 `opened_at` 与 `open_expire_at`;Interlock 物料规则校验是否过期、是否已回温。
- **Undo Track-Out 联动**(M4 补偿处理器):撤销时按 `txn_id` 找到该事务产生的消耗记录,并沿 `adjust_ref_id` 汇总其已批准修正与清偿,按**净消耗**回补绑定剩余量/库存,追加负数行;若绑定已被下料或批已变更,或存在 `PENDING` 的修正申请,则拒绝撤销。
- **物料需求汇总**:每次写消耗行(含负数行、清偿行)同事务写 `MATERIAL_CONSUMED` 事件,订阅者对 `(wo_id, bom_item_id)` 按 `SUM(qty)` 重算 `consumed_qty`(负数行已表达冲销与撤销,不再排除撤销登记),天然幂等。聚合键 `WORK_ORDER/wo_id`,与工单进度串行。`issued_qty` 由 warehouse 在领/退料单确认时发布 `MATERIAL_ISSUED`(含 `wo_id/material_id/qty`,退料为负),exec 订阅后累加(投递表保证只生效一次)。

## 4. 表设计

### 4.1 新增表(5 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_equipment_slot` | 设备物料位 | `app-mes-exec` |
| `mes_equipment_material_binding` | 设备物料绑定 | `app-mes-exec` |
| `hist_mes_material_consumption` | 物料消耗记录 | `app-mes-exec` |
| `mes_consumption_adjust` | 消耗修正申请 | `app-mes-exec` |
| `mes_wo_material_requirement` | 工单物料需求 | `app-mes-exec` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_equipment_slot` 设备物料位
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `port_id` | BIGINT |  |  | Port/Chamber |
| `slot_code` | VARCHAR(64) | 是 |  | 槽位编码 |
| `slot_name` | VARCHAR(128) |  |  | 槽位名称 |
| `allowed_material_type` | VARCHAR(16) |  |  | 允许的物料类型,空表示不限 |
| `allowed_material_id` | BIGINT |  |  | 限定的物料,空表示不限 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_eqp_slot`(tenant_id, equipment_id, slot_code)

#### `mes_equipment_material_binding` 设备物料绑定
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `slot_id` | BIGINT | 是 |  | 槽位 |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `material_lot_id` | BIGINT | 是 |  | 物料批 |
| `material_id` | BIGINT | 是 |  | 物料 |
| `initial_qty` | DECIMAL(18,4) | 是 |  | 装载量 |
| `remaining_qty` | DECIMAL(18,4) | 是 |  | 剩余量 |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `low_threshold` | DECIMAL(18,4) |  |  | 低位预警阈值 |
| `life_limit_minutes` | INT |  |  | 寿命上限(分钟),空表示不按时间 |
| `status` | VARCHAR(16) | 是 | ACTIVE | ACTIVE/DEPLETED/UNMOUNTED |
| `unmount_disposition` | VARCHAR(16) |  |  | 下料剩余量处置 RETURN_LINESIDE/SCRAP |
| `unmounted_remaining_qty` | DECIMAL(18,4) |  |  | 下料时剩余量 |
| `mounted_by` | VARCHAR(64) |  |  | 上料人 |
| `mounted_at` | DATETIME | 是 |  | 上料时间 |
| `unmounted_by` | VARCHAR(64) |  |  | 下料人 |
| `unmounted_at` | DATETIME |  |  | 下料时间 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_slot_status`(tenant_id, slot_id, status);索引 `idx_equipment_material`(tenant_id, equipment_id, material_id, status);索引 `idx_material_lot`(tenant_id, material_lot_id)

#### `hist_mes_material_consumption` 物料消耗记录
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT |  |  | Lot |
| `wafer_id` | BIGINT | 是 | 0 | Wafer,0 表示 Lot 级消耗 |
| `route_seq_no` | INT |  |  | 步骤序号 |
| `step_id` | BIGINT |  |  | 站点 |
| `pass_no` | INT |  |  | 第 N 次经过 |
| `equipment_id` | BIGINT |  |  | 设备 |
| `bom_item_id` | BIGINT | 是 | 0 | 来源 BOM 行,0 表示无 |
| `material_id` | BIGINT | 是 |  | 物料 |
| `material_lot_id` | BIGINT | 是 | 0 | 物料批,0 表示无 |
| `binding_id` | BIGINT | 是 | 0 | 设备端绑定,0 表示直耗线边 |
| `location_id` | BIGINT | 是 | 0 | 直耗线边时被扣减的库位,设备端绑定为 0 |
| `qty` | DECIMAL(18,4) | 是 |  | 消耗数量;修正为差额,Undo/清偿冲销为负数 |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `consume_mode` | VARCHAR(16) |  |  | 消耗方式 |
| `source` | VARCHAR(16) | 是 | BACKFLUSH | BACKFLUSH/OPEN_LOT/MANUAL/ADJUST/UNDO/SETTLE |
| `is_substitute` | TINYINT(1) | 是 | 0 | 是否使用替代料 |
| `is_shortage` | TINYINT(1) | 是 | 0 | 欠账:库存/剩余量不足时仍放行的消耗 |
| `adjust_ref_id` | BIGINT |  |  | 被修正/被清偿的原消耗记录 |
| `txn_id` | BIGINT | 是 |  | 事务ID(手工、修正、Undo 冲销都有自己的事务,不用 0 哨兵) |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:唯一 `uk_backflush`(tenant_id, txn_id, bom_item_id, wafer_id, material_lot_id, binding_id, location_id);索引 `idx_lot`(tenant_id, lot_id, occurred_at);索引 `idx_wafer`(tenant_id, wafer_id);索引 `idx_material_lot`(tenant_id, material_lot_id);索引 `idx_binding`(tenant_id, binding_id)

说明:只增表:修正追加差额行,Undo 与欠账清偿追加冲销行。**每笔手工录入、修正、Undo 冲销都经事务层取得自己的 `txn_id`**,因此不会与原记录键冲突;同一事务内对同一物料批、跨多个线边库位的先进先出扣减以 `location_id` 区分。

#### `mes_consumption_adjust` 消耗修正申请
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `adjust_no` | VARCHAR(64) | 是 |  | 修正单号 |
| `consumption_id` | BIGINT | 是 |  | 原消耗记录 |
| `qty_before` | DECIMAL(18,4) | 是 |  | 修正前数量 |
| `qty_after` | DECIMAL(18,4) | 是 |  | 修正后数量 |
| `reason` | VARCHAR(256) | 是 |  | 修正原因 |
| `status` | VARCHAR(16) | 是 | PENDING | PENDING/APPROVED/REJECTED |
| `applicant` | VARCHAR(64) |  |  | 申请人 |
| `approver` | VARCHAR(64) |  |  | 审批人 |
| `approved_at` | DATETIME |  |  | 审批时间 |
| `comment` | VARCHAR(256) |  |  | 审批意见 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_adjust_no`(tenant_id, adjust_no);索引 `idx_consumption`(tenant_id, consumption_id);索引 `idx_status`(tenant_id, status)

#### `mes_wo_material_requirement` 工单物料需求
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wo_id` | BIGINT | 是 |  | 工单 |
| `bom_item_id` | BIGINT | 是 | 0 | BOM 行,0 表示无 |
| `material_id` | BIGINT | 是 |  | 物料 |
| `step_id` | BIGINT |  |  | 消耗站点 |
| `required_qty` | DECIMAL(18,4) | 是 |  | 需求数量 |
| `issued_qty` | DECIMAL(18,4) | 是 | 0 | 已领到线边数量 |
| `consumed_qty` | DECIMAL(18,4) | 是 | 0 | 已消耗数量(由 MATERIAL_CONSUMED 事件异步汇总,不在 Track-Out 事务内更新,避免同工单热点) |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `kitting_status` | VARCHAR(16) | 是 | UNCHECKED | UNCHECKED/OK/SHORT |
| `short_qty` | DECIMAL(18,4) |  |  | 缺料数量 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_wo_bom`(tenant_id, wo_id, bom_item_id);索引 `idx_material`(tenant_id, material_id)

### 4.2 变更表(1 处)

#### 变更 `mes_material` 物料主数据

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `open_life_hours` | INT |  |  | 开封后有效期(小时),空表示无 |
| `thaw_minutes` | INT |  |  | 回温最短时间(分钟),空表示无 |

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/equipment/{id}/slots` CRUD
- POST `/api/v1/mes/equipment-slots/{id}/mount`(上料)、`/unmount`(下料)、`/replace`(换料)
- GET `/api/v1/mes/material-bindings`
- GET `/api/v1/mes/work-orders/{id}/material-requirements`;POST `…/check-kitting`
- GET `/api/v1/mes/material-consumptions`(按 Lot/Wafer/物料批)
- `/api/v1/mes/consumption-adjusts` CRUD;POST `…/{id}/approve`、`/reject`
- GET `/api/v1/mes/alerts`;POST `/api/v1/mes/alerts/{id}/read`
- 终端:`/api/v1/mes/terminal/material/mount`、`/terminal/material/issue-confirm`

## 6. 验收标准

1. 开批扣减晶圆库存,Wafer 能反查来料批
2. 上料后 Track-Out 自动扣减设备端剩余量,消耗记录能追到 Lot/Wafer/物料批
3. 耗尽后下一次 Track-In 被物料 Interlock 拦截,低位提前预警
4. 手工修正审批通过后库存与绑定剩余量一致,原记录保留
5. Undo Track-Out 后消耗按净消耗回补(含已批准修正)
6. 欠账消耗经清偿后净消耗不变、库存扣减到实际物料批
7. 选用来料 Wafer 清单开批后,Wafer 带出客户编号,清单行为 USED

## 7. 待确认

1. 反冲用量是否乘损耗率?当前设计:不乘,损耗率只用于需求展开与差异判定,请确认。
2. 晶圆物料批的"1 片"与 Lot 的 Wafer 一一对应,是否允许一个物料批对应多个 Lot?当前允许(一批来料开多个 Lot)。
