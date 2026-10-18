# M8 管控完整化

> 状态:**设计草案,待核对**(未实现)· 依赖:M4、M2 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_8__mes_m8.sql`)

## 1. 目标

Track-In 前所有校验规则就位:Recipe、工具、人员资格、Queue Time/Time Link、数据权限;并提供设备异常登记、关键操作二次确认。

## 2. 范围

- 工具类型/台账/使用与保养
- 人员资质(设备×Recipe×产品 的资格已并入 M2 的 mes_equipment_capability)
- Interlock 规则与校验日志
- Queue Time/Time Link 校验与自动 Hold
- 数据权限(区域/站点/设备组)
- 设备异常登记
- 二次确认与电子签名

## 3. 设计规则

- **Interlock 结构**:`mes_interlock_rule` 定义规则类型与作用范围;`InterlockEngine` 对一次 Track-In 依次执行所有适用规则,汇总结果 `PASS/WARN/BLOCK`,任一 `BLOCK` 即拒绝;完整明细写 `hist_mes_interlock_log` 并返回给前端。
- **规则类型**:`LOT_STATE`、`STEP_MATCH`、`EQP_ALLOWED`、`EQP_STATE`、`EQP_CAPACITY`、`RECIPE`、`TOOL`、`PERSON`、`QUEUE_TIME`、`TIME_LINK`、`MATERIAL`(M9)。M3 的 6 条基础校验(`LOT_STATE/STEP_MATCH/EQP_ALLOWED/EQP_STATE/EQP_CAPACITY/RECIPE`)在 M8 迁移为"系统内置规则"(`is_builtin=1`):不可删除、不可停用(`enabled` 不能改为 0)、不可降级;其他规则可按范围启用与降级为 WARN。
- **资格**:设备侧资格已在 M2 并入 `mes_equipment_capability`(状态 VALID/SUSPENDED/EXPIRED/REVOKED、有效期,SUSPENDED 即 Inhibit),本里程碑只补人员侧 `mes_user_qual`:"哪些人可操作哪些设备/设备组/站点",到期自动失效。未配置资格记录的对象视为不受限(规则的"严格模式"参数可改为未配置即拒绝)。
- **工具状态迁移**:`AVAILABLE →(CHECKOUT,装到设备)→ IN_USE`(`current_equipment_id` = 该设备)`→(RETURN)→ AVAILABLE`;清洗中 `CLEANING`、保养中或达寿命 `MAINTENANCE`、报废 `SCRAPPED`,完成后回 `AVAILABLE`。
- **工具校验**:站点(或 Route 步骤覆盖)配置所需工具类型(`mes_step_tool` / `mes_route_step_tool`);Track-In 只能选该类型中 **`IN_USE` 且装在本设备**的工具,或 `AVAILABLE` 的工具(同事务自动 CHECKOUT 到本设备),且 `used_count < life_limit`。同一工具可被本设备上的多个 Lot 共用(如 Reticle),工具行按 M0 规则短等待加锁。**事件与回退**:Track-In 写 `TRACK_IN_BIND`(`txn_id` = Track-In 事务);Track-Out 递增使用量并写 `USE`(`txn_id` = Track-Out 事务,`bind_txn_id` 指向绑定事务);Undo Track-Out 按 `txn_id` 找到 `USE` 行,写 `USE_REVERSAL` 并回减;Cancel Track-In 写 `UNBIND`(若为自动 CHECKOUT 则同时 RETURN)。达寿命 90% 产生预警,达 100% 置 `MAINTENANCE`。
- **Time Link 计时**:`last_track_out_at` 只记最近一次出站,不能表达不相邻的 A→C 链路,因此计时独立成表 `mes_lot_time_link_timer`:起点步骤 Track-Out 时开 ACTIVE 计时,终点步骤 Track-In 时置 DONE;Split 复制、Merge 取较早起点;Undo 起点出站把该次开的计时置 CANCELLED,Cancel 终点 Track-In 把被关闭的计时恢复 ACTIVE;改 Route 时旧版本计时置 CANCELLED。
- **Queue Time/Time Link 校验**:进站时 Queue Time 按 `step_enter_at`、Time Link 按 ACTIVE 计时的 `deadline_at` 判定是否超限;未进站的 Lot 由定时任务扫描(Time Link 走 `idx_status_deadline`),到达阈值产生预警事件,超限自动 Hold(`hold_source=QUEUE_TIME/TIME_LINK`),动作与原因码分别取步骤的 `queue_time_action` 与 `mes_route_time_link.action/reason_code`;同一 Lot/步骤已有同源 ACTIVE Hold 时不重复创建(M3 `active_key` 幂等),扫描任务按租户遍历、**逐 Lot 开独立事务并按规则加锁**后写 Hold 与预警(不在其他事务的行锁内嵌套)。
- **数据权限**:`mes_user_scope` 限定用户可见/可操作的区域、站点、设备组;为空表示不限。作业清单、Lot 列表、Track 操作均按范围过滤。
- **设备异常登记**:登记异常描述、影响 Lot;关闭时记录处理结果。不自动改变设备状态(由操作员单独切换),但可勾选"同时置停机"。
- **二次确认/电子签名**:`mes_confirm_rule` 配置哪些动作需要二次确认(同一用户再次输入密码)或第二人签名(另一个有权限用户账号+密码);签名留痕 `hist_mes_esign`。

## 4. 表设计

### 4.1 新增表(14 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_tool_type` | 工具类型 | `app-mes-base` |
| `mes_tool` | 工具台账 | `app-mes-base` |
| `mes_step_tool` | 站点-工具要求(站点级) | `app-mes-base` |
| `mes_route_step_tool` | Route 步骤-工具覆盖(步骤级,随 Route 版本) | `app-mes-base` |
| `hist_mes_tool_event` | 工具使用与保养记录 | `app-mes-base` |
| `mes_user_qual` | 人员资质 | `app-mes-base` |
| `mes_interlock_rule` | Interlock 规则 | `app-mes-base` |
| `hist_mes_interlock_log` | Interlock 校验日志 | `app-mes-exec` |
| `mes_lot_time_link_timer` | Lot Time Link 计时 | `app-mes-exec` |
| `mes_user_scope` | 用户数据权限 | `app-mes-base` |
| `mes_equipment_fault` | 设备异常 | `app-mes-base` |
| `mes_equipment_fault_lot` | 设备异常影响 Lot | `app-mes-base` |
| `mes_confirm_rule` | 二次确认规则 | `app-mes-base` |
| `hist_mes_esign` | 电子签名记录 | `app-mes-exec` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_tool_type` 工具类型
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `type_code` | VARCHAR(64) | 是 |  | 类型编码 |
| `type_name` | VARCHAR(128) | 是 |  | 类型名称 |
| `life_unit` | VARCHAR(16) | 是 | USES | 寿命单位 USES/WAFERS/MINUTES |
| `default_life` | INT |  |  | 默认寿命上限 |
| `clean_cycle` | INT |  |  | 默认清洗周期(同寿命单位) |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_type_code`(tenant_id, type_code)

#### `mes_tool` 工具台账
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `tool_code` | VARCHAR(64) | 是 |  | 工具编码 |
| `tool_name` | VARCHAR(128) | 是 |  | 工具名称 |
| `type_id` | BIGINT | 是 |  | 工具类型 |
| `status` | VARCHAR(16) | 是 | AVAILABLE | AVAILABLE/IN_USE/CLEANING/MAINTENANCE/SCRAPPED |
| `used_count` | BIGINT | 是 | 0 | 累计使用量(按寿命单位) |
| `life_limit` | BIGINT |  |  | 寿命上限,空表示不限 |
| `since_clean` | BIGINT | 是 | 0 | 距上次清洗使用量 |
| `current_equipment_id` | BIGINT |  |  | 当前装载设备 |
| `last_clean_at` | DATETIME |  |  | 最近清洗时间 |
| `last_maint_at` | DATETIME |  |  | 最近保养时间 |
| `location_text` | VARCHAR(128) |  |  | 存放位置 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_tool_code`(tenant_id, tool_code);索引 `idx_type_status`(tenant_id, type_id, status);索引 `idx_equipment`(tenant_id, current_equipment_id)

#### `mes_step_tool` 站点-工具要求(站点级)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `step_id` | BIGINT | 是 |  | 站点 |
| `tool_type_id` | BIGINT | 是 |  | 所需工具类型 |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示通用 |
| `required_qty` | INT | 是 | 1 | 所需数量 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_step_tool`(tenant_id, step_id, tool_type_id, product_id)

#### `mes_route_step_tool` Route 步骤-工具覆盖(步骤级,随 Route 版本)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `route_step_id` | BIGINT | 是 |  | Route 步骤 |
| `tool_type_id` | BIGINT | 是 |  | 所需工具类型(如每层不同 Reticle) |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示通用 |
| `required_qty` | INT | 是 | 1 | 所需数量 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_route_step_tool`(tenant_id, route_step_id, tool_type_id, product_id);索引 `idx_version`(tenant_id, route_version_id)

说明:存在匹配记录时整体替换站点级 `mes_step_tool`;版本复制时一并复制,生效后只读。

#### `hist_mes_tool_event` 工具使用与保养记录
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `tool_id` | BIGINT | 是 |  | 工具 |
| `event_type` | VARCHAR(16) | 是 |  | CHECKOUT/RETURN/TRACK_IN_BIND/UNBIND/USE/USE_REVERSAL/CLEAN/MAINTAIN/SCRAP |
| `lot_id` | BIGINT |  |  | 关联 Lot |
| `equipment_id` | BIGINT |  |  | 关联设备 |
| `step_id` | BIGINT |  |  | 关联站点 |
| `usage_qty` | BIGINT |  |  | 本次使用量 |
| `txn_id` | BIGINT |  |  | 产生本事件的事务(绑定为 Track-In,使用为 Track-Out,纠错按它查找) |
| `bind_txn_id` | BIGINT |  |  | USE/USE_REVERSAL 对应的 Track-In 绑定事务 |
| `used_total_after` | BIGINT |  |  | 事件后累计使用量 |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_tool_time`(tenant_id, tool_id, occurred_at);索引 `idx_lot`(tenant_id, lot_id);索引 `idx_txn`(tenant_id, txn_id)

#### `mes_user_qual` 人员资质
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `user_name` | VARCHAR(64) | 是 |  | 用户名(sys_user.user_name,与 operator 等字段口径一致) |
| `target_type` | VARCHAR(16) | 是 |  | EQUIPMENT/EQUIPMENT_GROUP/STEP |
| `target_id` | BIGINT | 是 |  | 目标对象ID |
| `status` | VARCHAR(16) | 是 | VALID | VALID/EXPIRED/REVOKED |
| `valid_from` | DATETIME |  |  | 生效起 |
| `valid_to` | DATETIME |  |  | 生效止 |
| `certified_by` | VARCHAR(64) |  |  | 认证人 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_user_qual`(tenant_id, user_name, target_type, target_id);索引 `idx_status`(tenant_id, status, valid_to)

#### `mes_interlock_rule` Interlock 规则
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `rule_code` | VARCHAR(64) | 是 |  | 规则编码 |
| `rule_name` | VARCHAR(128) | 是 |  | 规则名称 |
| `rule_type` | VARCHAR(24) | 是 |  | LOT_STATE/STEP_MATCH/EQP_ALLOWED/EQP_STATE/EQP_CAPACITY/RECIPE/TOOL/PERSON/QUEUE_TIME/TIME_LINK/MATERIAL |
| `scope_type` | VARCHAR(24) | 是 | GLOBAL | GLOBAL/STEP/PRODUCT/EQUIPMENT_GROUP/AREA |
| `scope_id` | BIGINT |  |  | 范围对象ID |
| `severity` | VARCHAR(8) | 是 | BLOCK | BLOCK/WARN |
| `is_builtin` | TINYINT(1) | 是 | 0 | 系统内置,不可删除/不可停用/不可降级 |
| `params_json` | JSON |  |  | 规则参数,如严格模式、预警阈值 |
| `sort_no` | INT | 是 | 0 | 执行顺序 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rule_code`(tenant_id, rule_code);索引 `idx_type_scope`(tenant_id, rule_type, scope_type, scope_id)

#### `hist_mes_interlock_log` Interlock 校验日志
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `lot_no` | VARCHAR(64) |  |  | Lot 号 |
| `step_id` | BIGINT |  |  | 站点 |
| `route_seq_no` | INT |  |  | 步骤序号 |
| `equipment_id` | BIGINT |  |  | 设备 |
| `action` | VARCHAR(24) | 是 | TRACK_IN | 被校验动作 TRACK_IN/BATCH_TRACK_IN/PRECHECK |
| `result` | VARCHAR(8) | 是 |  | PASS/WARN/BLOCK |
| `details_json` | JSON |  |  | 逐条规则结果 [{ruleCode,type,result,message}] |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `checked_at` | DATETIME | 是 |  | 校验时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot_time`(tenant_id, lot_id, checked_at);索引 `idx_result_time`(tenant_id, result, checked_at)

#### `mes_lot_time_link_timer` Lot Time Link 计时
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `time_link_id` | BIGINT | 是 |  | Time Link 约束(mes_route_time_link) |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `from_seq_no` | INT | 是 |  | 起点步骤序号 |
| `to_seq_no` | INT | 是 |  | 终点步骤序号 |
| `start_at` | DATETIME | 是 |  | 开始计时(起点步骤 Track-Out 时间) |
| `deadline_at` | DATETIME | 是 |  | 超限时间 = start_at + max_minutes |
| `status` | VARCHAR(16) | 是 | ACTIVE | ACTIVE/DONE/CANCELLED |
| `alert_level` | VARCHAR(16) | 是 | NONE | NONE/WARNED/BREACHED,扫描任务据此不重复处理 |
| `start_txn_id` | BIGINT | 是 |  | 开始计时的出站事务 |
| `end_txn_id` | BIGINT |  |  | 结束计时的事务(终点 Track-In 或取消) |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_lot_link_start`(tenant_id, lot_id, time_link_id, start_txn_id);索引 `idx_status_deadline`(tenant_id, status, deadline_at);索引 `idx_lot_status`(tenant_id, lot_id, status)

说明:起点步骤 Track-Out 时为每条从该步骤出发的 Time Link 开一条 ACTIVE 计时,终点步骤 Track-In 时置 DONE;Split 复制 ACTIVE 行(`start_txn_id` 沿用),Merge 保留较早起点。

#### `mes_user_scope` 用户数据权限
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `user_name` | VARCHAR(64) | 是 |  | 用户名(sys_user.user_name) |
| `scope_type` | VARCHAR(24) | 是 |  | AREA/STEP/EQUIPMENT_GROUP |
| `scope_id` | BIGINT | 是 |  | 范围对象ID |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_user_scope`(tenant_id, user_name, scope_type, scope_id)

#### `mes_equipment_fault` 设备异常
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `fault_no` | VARCHAR(64) | 是 |  | 异常单号 |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `fault_code` | VARCHAR(64) |  |  | 异常码 |
| `severity` | VARCHAR(8) | 是 | MINOR | MINOR/MAJOR/CRITICAL |
| `description` | VARCHAR(1024) | 是 |  | 异常描述 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `reported_by` | VARCHAR(64) |  |  | 登记人 |
| `status` | VARCHAR(16) | 是 | OPEN | OPEN/HANDLING/CLOSED |
| `handled_by` | VARCHAR(64) |  |  | 处理人 |
| `handled_at` | DATETIME |  |  | 处理完成时间 |
| `solution` | VARCHAR(1024) |  |  | 处理结果 |
| `affected_lot_count` | INT | 是 | 0 | 影响 Lot 数 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_fault_no`(tenant_id, fault_no);索引 `idx_equipment_time`(tenant_id, equipment_id, occurred_at);索引 `idx_status`(tenant_id, status)

#### `mes_equipment_fault_lot` 设备异常影响 Lot
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `fault_id` | BIGINT | 是 |  | 异常单 |
| `lot_id` | BIGINT | 是 |  | Lot |
| `impact` | VARCHAR(256) |  |  | 影响说明 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_fault_lot`(tenant_id, fault_id, lot_id)

#### `mes_confirm_rule` 二次确认规则
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `action_code` | VARCHAR(64) | 是 |  | 动作编码 HOLD_RELEASE/SCRAP/UNSCRAP/FORCE_MOVE/VERSION_ACTIVATE/… |
| `confirm_mode` | VARCHAR(16) | 是 | REAUTH | 模式 NONE/REAUTH/SECOND_SIGN |
| `second_role_code` | VARCHAR(64) |  |  | 第二签名人需具备的角色 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_action`(tenant_id, action_code)

#### `hist_mes_esign` 电子签名记录
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `action_code` | VARCHAR(64) | 是 |  | 动作编码 |
| `biz_type` | VARCHAR(32) |  |  | 业务对象类型 |
| `biz_id` | BIGINT |  |  | 业务对象ID |
| `signer` | VARCHAR(64) | 是 |  | 签名人 |
| `signer_role` | VARCHAR(64) |  |  | 签名人角色 |
| `sign_type` | VARCHAR(16) | 是 |  | PRIMARY/SECOND |
| `meaning` | VARCHAR(256) |  |  | 签名含义/原因 |
| `txn_id` | BIGINT |  |  | 事务ID |
| `signed_at` | DATETIME | 是 |  | 签名时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_biz`(tenant_id, biz_type, biz_id);索引 `idx_signer_time`(tenant_id, signer, signed_at)

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/tool-types`、`/tools` CRUD;POST `/tools/{id}/checkout`、`/return`、`/clean`、`/maintain`
- `/api/v1/mes/steps/{id}/tools` 站点工具要求
- `/api/v1/mes/user-quals` CRUD(设备资格在 M2 的 `/equipment/{id}/capabilities`)
- `/api/v1/mes/interlock-rules` CRUD;GET `/api/v1/mes/interlock-logs`
- POST `/api/v1/mes/interlock/check`(Track-In 前预检,不落事务)
- `/api/v1/mes/user-scopes` CRUD
- `/api/v1/mes/equipment-faults` CRUD;POST `…/{id}/close`
- `/api/v1/mes/confirm-rules` CRUD

## 6. 验收标准

1. Recipe 不匹配、工具超寿命、无资质、Queue Time 超时时 Track-In 被拦截,并有逐条明细
2. WARN 级规则只提示不阻断
3. 用户只能看到并操作授权区域的 Lot 与设备
4. 需二次确认的动作未确认时被拒绝,确认后留签名记录
5. A→C 的 Time Link 中间经过 B 站,计时仍从 A 出站起算;Split 子 Lot 继承计时
6. 工具 Undo/Cancel 后使用量与绑定恢复,`hist_mes_tool_event` 有对应冲销事件;内置规则不能停用

## 7. 待确认

1. "未配置资格即放行"与"未配置即拒绝"哪个作为默认?当前设计默认放行(便于上线),可按规则切换。
2. 电子签名的法规等级要求(如是否需要符合 21 CFR Part 11)?一般 MES 通用场景按"二次认证+留痕"即可。
3. 框架支持 OAuth2 登录,这类用户没有本地密码,REAUTH 无法重新输入密码:是否改用"确认码/PIN"或再次走 OAuth2 授权?
