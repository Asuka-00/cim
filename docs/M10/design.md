# M10 采集与质量判定

> 状态:**设计草案,待核对**(未实现)· 依赖:M3 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_10__mes_m10.sql`)

## 1. 目标

手工录入采集值并保存原始结果,按 Spec 上下限判定 OOS 并可自动 Hold;抽检规则决定 Lot 是否必经量测站点。不做控制图与判异(由 SPC 系统负责,本期不对接)。

## 2. 范围

- 采集项定义与站点绑定
- 采集结果录入与存储
- Spec 判定与自动 Hold
- 抽检规则与计数
- 终端录入
- 趋势展示(仅列表/折线)

## 3. 设计规则

- **采集项与规格分离**:`mes_collect_item` 定义参数本身;规格上下限在站点级 `mes_step_collect_item` 或步骤级 `mes_route_step_collect_item`。查找顺序(与 M2 一致):**步骤+产品 → 步骤通用 → 站点+产品 → 站点通用**,步骤级存在记录时**整体替换**站点级集合,同一站点在不同层的 CD 规格可以不同。
- **录入粒度**:Lot 级(一条值)、Wafer 级(每片一条)、Site 级(Wafer 内多点位,`site_no`)。由站点采集项的 `granularity` 决定终端录入形态。
- **判定**:`value_num` 小于 LSL 为 `OOS_LOW`,大于 USL 为 `OOS_HIGH`,否则 `OK`;文本/布尔类按期望值比较;采集项无规格时 `NA`。结果落库同时写 `judge_result` 与当时规格快照(`lsl/usl`),规格后续变更不影响历史。
- **采集录入事务**:录入是独立的 `COLLECT` 事务(经统一事务层),只允许 Lot 在量测站点 `RUNNING` 时录入(Track-In 后、Track-Out 前)。`COLLECT` 有自己的补偿处理器:追加 VOID 作废本事务写入的记录,并释放其产生的 OOS Hold;按"只撤销最后一笔有效事务",要撤销该站 Track-In 须先撤销其后的 `COLLECT`。Undo Track-Out 后 Lot 回到 RUNNING,已录入的结果仍属于本次经过,不作废。
- **自动 Hold**:`auto_hold_on_oos=1` 时 OOS 触发 Hold(`hold_source=OOS`,原因码取配置),在 `COLLECT` 事务内完成;因录入时 Lot 为 RUNNING,出站后停在下一站点并保持 Hold(M3)。
- **必采校验**:站点 `is_required=1` 的采集项未录入,Track-Out 被拒绝(量测站点)。
- **抽检**:`mes_sampling_rule` 定义抽检方式(每 N 个 Lot 抽 1 个、按比例、每 Lot 固定 Wafer 数、指定 Slot);`mes_step_sampling` 把规则绑定到量测站点(可限定产品)。抽检决定由 M10 注册到**下一步解析链**与 **ArrivalHook**:任何路径(Track-Out、Skip、Force Move、Move Back、改 Route、返工回归、WIP 导入)使 Lot 即将到达或到达量测步骤时,对 `(lot, route_seq, pass_no)` 做一次决定写 `mes_lot_sampling_plan`(已存在则沿用);未抽中且步骤 `is_sampling_step=1` 时由下一步解析跳过该站点。量测站点上**没有决定记录**时按"需要量测"处理。
- **抽检计数**:`mes_sampling_counter` 按 `(rule, step, route_step, product)` 维护计数(`route_step_id=0` 表示各层共用),命中后清零;可手工重置。计数在**做抽检决定时**原子递增并在同一事务读回新值,决定依赖该值,因此不能放到事务最后——这是 M0"热点行尽量靠后"的例外,其后只剩本 Lot 的少量写入,持锁时间可控。**撤销不回退计数**(期间可能已被其他 Lot 递增或清零):Undo Track-Out 只删除本次出站写入的 `mes_lot_sampling_plan` 行,重新出站时重新决定。
- **趋势**:仅按 `(item, product, step)` 取最近 N 次结果的列表/折线,不做 Western Electric 等判异规则。
- **更正与作废**:采集结果为只增表,更正通过追加记录表达:`record_type=ORIGINAL/CORRECTION/VOID`,`ref_id` 指向被更正的记录。"有效值"取同一 `(lot, wafer, site, item, route_seq, pass_no)` 链上的**最后一条记录**:为 VOID 则该点无有效值,否则取其值;必采校验、趋势、Spec 判定都只看有效值。撤销 `COLLECT` 事务时追加 VOID 记录(Undo Track-Out 不作废采集结果)。
- **原始数据体量**:`hist_mes_collect_result` 是增长最快的表,索引控制在 3 个以内;不做分区(D11),后续如需调整另行评估。

## 4. 表设计

### 4.1 新增表(9 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_collect_item` | 采集项 | `app-mes-base` |
| `mes_step_collect_item` | 站点采集项与规格(站点级) | `app-mes-base` |
| `mes_route_step_collect_item` | Route 步骤采集项与规格覆盖(步骤级,随 Route 版本) | `app-mes-base` |
| `hist_mes_collect_result` | 采集结果 | `app-mes-exec` |
| `mes_sampling_rule` | 抽检规则 | `app-mes-base` |
| `mes_step_sampling` | 站点抽检绑定(站点级) | `app-mes-base` |
| `mes_route_step_sampling` | Route 步骤抽检覆盖(步骤级,随 Route 版本) | `app-mes-base` |
| `mes_sampling_counter` | 抽检计数器 | `app-mes-exec` |
| `mes_lot_sampling_plan` | Lot 抽检决定 | `app-mes-exec` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_collect_item` 采集项
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `item_code` | VARCHAR(64) | 是 |  | 采集项编码 |
| `item_name` | VARCHAR(128) | 是 |  | 采集项名称 |
| `data_type` | VARCHAR(16) | 是 | NUMBER | 数据类型 NUMBER/TEXT/BOOL/ENUM |
| `unit` | VARCHAR(32) |  |  | 数值单位 |
| `decimals` | TINYINT |  |  | 小数位 |
| `enum_options` | JSON |  |  | 枚举选项 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_item_code`(tenant_id, item_code)

#### `mes_step_collect_item` 站点采集项与规格(站点级)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `step_id` | BIGINT | 是 |  | 站点 |
| `item_id` | BIGINT | 是 |  | 采集项 |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示通用 |
| `granularity` | VARCHAR(8) | 是 | LOT | 录入粒度 LOT/WAFER/SITE |
| `site_count` | INT |  |  | SITE 粒度的点位数 |
| `is_required` | TINYINT(1) | 是 | 1 | 是否必采 |
| `lsl` | DECIMAL(24,8) |  |  | 规格下限 |
| `usl` | DECIMAL(24,8) |  |  | 规格上限 |
| `target` | DECIMAL(24,8) |  |  | 目标值 |
| `expect_text` | VARCHAR(64) |  |  | 文本/布尔期望值 |
| `auto_hold_on_oos` | TINYINT(1) | 是 | 0 | OOS 自动 Hold |
| `hold_reason_code` | VARCHAR(64) |  |  | 自动 Hold 原因码 |
| `sort_no` | INT | 是 | 0 | 排序号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_step_item_product`(tenant_id, step_id, item_id, product_id);索引 `idx_item`(tenant_id, item_id)

#### `mes_route_step_collect_item` Route 步骤采集项与规格覆盖(步骤级,随 Route 版本)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `route_step_id` | BIGINT | 是 |  | Route 步骤 |
| `item_id` | BIGINT | 是 |  | 采集项 |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示通用 |
| `granularity` | VARCHAR(8) | 是 | LOT | 录入粒度 LOT/WAFER/SITE |
| `site_count` | INT |  |  | SITE 粒度的点位数 |
| `is_required` | TINYINT(1) | 是 | 1 | 是否必采 |
| `lsl` | DECIMAL(24,8) |  |  | 规格下限 |
| `usl` | DECIMAL(24,8) |  |  | 规格上限 |
| `target` | DECIMAL(24,8) |  |  | 目标值 |
| `expect_text` | VARCHAR(64) |  |  | 文本/布尔期望值 |
| `auto_hold_on_oos` | TINYINT(1) | 是 | 0 | OOS 自动 Hold |
| `hold_reason_code` | VARCHAR(64) |  |  | 自动 Hold 原因码 |
| `sort_no` | INT | 是 | 0 | 排序号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_route_step_item_product`(tenant_id, route_step_id, item_id, product_id);索引 `idx_version`(tenant_id, route_version_id)

说明:存在匹配记录时整体替换站点级采集项集合;版本复制时一并复制,生效后只读。

#### `hist_mes_collect_result` 采集结果
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `wafer_id` | BIGINT |  |  | Wafer |
| `site_no` | INT |  |  | 点位号 |
| `record_type` | VARCHAR(16) | 是 | ORIGINAL | ORIGINAL/CORRECTION/VOID |
| `ref_id` | BIGINT |  |  | 被更正/作废的原记录 |
| `product_id` | BIGINT |  |  | 产品(冗余,趋势查询按 item+step+product 不必回表联查 Lot) |
| `route_seq_no` | INT |  |  | 步骤序号 |
| `step_id` | BIGINT | 是 |  | 站点 |
| `pass_no` | INT |  |  | 第 N 次经过 |
| `equipment_id` | BIGINT |  |  | 设备 |
| `item_id` | BIGINT | 是 |  | 采集项 |
| `value_num` | DECIMAL(24,8) |  |  | 数值 |
| `value_text` | VARCHAR(256) |  |  | 文本值 |
| `judge_result` | VARCHAR(16) | 是 | NA | OK/OOS_LOW/OOS_HIGH/OOS/NA |
| `lsl` | DECIMAL(24,8) |  |  | 判定时规格下限 |
| `usl` | DECIMAL(24,8) |  |  | 判定时规格上限 |
| `collected_by` | VARCHAR(64) |  |  | 录入人 |
| `collected_at` | DATETIME | 是 |  | 录入时间 |
| `txn_id` | BIGINT |  |  | 事务ID |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot_step`(tenant_id, lot_id, step_id, collected_at);索引 `idx_item_step_time`(tenant_id, item_id, step_id, product_id, collected_at);索引 `idx_wafer`(tenant_id, wafer_id)

#### `mes_sampling_rule` 抽检规则
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `rule_code` | VARCHAR(64) | 是 |  | 规则编码 |
| `rule_name` | VARCHAR(128) | 是 |  | 规则名称 |
| `sample_type` | VARCHAR(16) | 是 | EVERY_N_LOT | ALL/EVERY_N_LOT/PERCENT/FIXED |
| `n_value` | INT |  |  | 每 N 个 Lot 抽 1 个 |
| `percent` | DECIMAL(6,2) |  |  | 抽检百分比 |
| `wafer_count` | INT |  |  | 每 Lot 抽取 Wafer 数,空表示全部 |
| `slot_list` | VARCHAR(128) |  |  | 指定 Slot 列表,逗号分隔 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rule_code`(tenant_id, rule_code)

#### `mes_step_sampling` 站点抽检绑定(站点级)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `step_id` | BIGINT | 是 |  | 量测站点 |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示通用 |
| `rule_id` | BIGINT | 是 |  | 抽检规则 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_step_product`(tenant_id, step_id, product_id)

#### `mes_route_step_sampling` Route 步骤抽检覆盖(步骤级,随 Route 版本)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `route_step_id` | BIGINT | 是 |  | Route 步骤 |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示通用 |
| `rule_id` | BIGINT | 是 |  | 抽检规则 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_route_step_product`(tenant_id, route_step_id, product_id);索引 `idx_version`(tenant_id, route_version_id)

说明:存在匹配记录时替换站点级抽检绑定;版本复制时一并复制,生效后只读。

#### `mes_sampling_counter` 抽检计数器
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `rule_id` | BIGINT | 是 |  | 规则 |
| `step_id` | BIGINT | 是 |  | 站点 |
| `route_step_id` | BIGINT | 是 | 0 | Route 步骤,0 表示不区分步骤(各层抽检计数互不混用时配置步骤级) |
| `product_id` | BIGINT | 是 | 0 | 产品,0 表示通用 |
| `counter` | INT | 是 | 0 | 当前计数 |
| `last_sampled_lot_id` | BIGINT |  |  | 最近被抽中的 Lot |
| `last_sampled_at` | DATETIME |  |  | 最近抽中时间 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rule_step_product`(tenant_id, rule_id, step_id, route_step_id, product_id)

说明:计数器是所有 Lot 共享的热点行:用原子 `UPDATE … SET counter = counter + 1`(命中后清零),不用 NOWAIT 行锁,避免不同 Lot 并发出站互相误报占用。

#### `mes_lot_sampling_plan` Lot 抽检决定
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `route_seq_no` | INT | 是 |  | 量测步骤序号 |
| `pass_no` | INT | 是 | 1 | 第 N 次经过 |
| `step_id` | BIGINT | 是 |  | 量测站点 |
| `rule_id` | BIGINT |  |  | 依据的规则 |
| `need_measure` | TINYINT(1) | 是 |  | 本 Lot 是否需要量测 |
| `wafer_slots` | VARCHAR(128) |  |  | 被抽中的 Slot 列表 |
| `decided_at` | DATETIME | 是 |  | 决定时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_lot_seq_pass`(tenant_id, lot_id, route_seq_no, pass_no)

### 4.2 变更表(1 处)

#### 变更 `mes_route_step` Route 步骤

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `is_sampling_step` | TINYINT(1) | 是 | 0 | 抽检站点:未被抽中的 Lot 自动跳过 |

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/collect-items` CRUD;`/api/v1/mes/steps/{id}/collect-items` 绑定维护
- `/api/v1/mes/sampling-rules` CRUD;`/api/v1/mes/steps/{id}/sampling` 绑定维护
- POST `/api/v1/mes/lots/{id}/collect`(录入采集结果,批量)
- GET `/api/v1/mes/collect-results`(按 Lot/Wafer/参数/时间);GET `/api/v1/mes/collect-results/trend`
- GET `/api/v1/mes/lots/{id}/sampling-plans`;POST `/api/v1/mes/sampling-counters/{id}/reset`
- 终端:`/api/v1/mes/terminal/lots/{no}/collect-form` 返回应录入项与规格

## 6. 验收标准

1. 量测站点录入数据,超规格自动 Hold,历史保留规格快照
2. 每 3 个 Lot 抽 1 个:连续 6 个 Lot 经过,恰好 2 个进量测站点,其余自动跳站
3. 必采项缺失时 Track-Out 被拒绝
4. 趋势列表按时间倒序,支持按产品/设备过滤
5. 作废后该点无有效值,必采校验重新要求录入
6. 经 Skip/Force Move/Move Back 到达量测站点的 Lot 也有抽检决定;无决定时按需要量测处理
7. 撤销 `COLLECT` 后其 OOS Hold 被释放;Lot 非 RUNNING 时不能录入

## 7. 待确认

1. OOS 是否需要区分"警戒限"(预警不 Hold)与"规格限"(Hold)?当前仅规格限。
2. Wafer 级多点位(Site)是否首期做?当前表结构已预留 `site_no`,录入界面可放 P2。
