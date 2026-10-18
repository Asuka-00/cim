# M2 主数据 II:制程与版本

> 状态:**设计草案,待核对**(未实现)· 依赖:M1 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_2__mes_m2.sql`)

## 1. 目标

把 M1 的实体编排成可执行的制程:产品版本、Route(含重复站点与单位转换站点)、BOM;建立统一的"版本状态机 + 审批记录"。Route 的步骤级绑定(Recipe、设备组、规格、抽检、工具)在本里程碑定型。

## 2. 范围

- 版本状态机与审批记录(Recipe/BOM/Route/产品版本复用)
- 产品版本与 Route 版本、步骤编排、重入
- 步骤计数单位与转换站点标记
- 站点绑定设备组与 Recipe,以及 Route 步骤级覆盖
- 设备 Port/Chamber 与能力/资格(含有效期)
- 设备同时加工上限;Queue Time / Time Link
- BOM 头/版本/行、站点绑定、反冲、替代料
- 站点组

## 3. 设计规则

- **版本状态机**:`DRAFT → PENDING → ACTIVE → INACTIVE`;审批驳回回到 `DRAFT`(驳回事件写 `hist_mes_approval_record`)。**同一对象(Route、BOM、Recipe、产品)任一时刻只有一个 ACTIVE 版本**;新版本生效时旧 ACTIVE 自动 INACTIVE;审批通过/生效时先对对象主表行(`mes_route/mes_bom/mes_recipe/mes_product`)`FOR UPDATE NOWAIT`,再降级旧版本、升级新版本,保证并发审批也只有一个 ACTIVE。INACTIVE 版本不再被新工单引用,在制 Lot 通过自己记录的版本 ID 继续使用。
- **生效即只读**:ACTIVE/INACTIVE 版本的头与明细不可改,改动必须复制新版本。复制保留来源 `copy_from_version_id`,并**一并复制**该版本关联的全部从属数据:步骤、Time Link、返工分支及返工步骤、条件跳转、Route 步骤级覆盖(Recipe/规格/抽检/工具)。
- **审批为简化版**(D14):提交时指定 `approver`,审批人 APPROVE/REJECT;不引入通用流程引擎。审批动作写 `hist_mes_approval_record`,版本表只保存当前状态与最近审批人。
- **Route 步骤**:`seq_no` 在同一 Route 版本内唯一(建议 10/20/30 步进)。同一个 `step_id` 可出现多次(重入)。"第 N 次经过"由 Lot 上的显式计数表 `mes_lot_step_pass`(M3)维护,不在步骤表里,也不从历史统计推导。
- **步骤级绑定的查找顺序(重入必需)**:同一站点在不同层使用的 Recipe、设备组、规格、抽检规则、工具都可能不同。**站点级绑定**(共享主数据)与**步骤级覆盖**(Route 版本的从属数据)分表存放:站点级 `mes_step_recipe/mes_step_tool/mes_step_collect_item/mes_step_sampling`;步骤级 `mes_route_step_recipe/mes_route_step_tool/mes_route_step_collect_item/mes_route_step_sampling`(都带 `route_version_id + route_step_id`,随版本复制、生效后只读)。**查找顺序**:步骤+产品 → 步骤通用(`product_id=0`)→ 站点+产品 → 站点通用。**覆盖语义是替换**:只要步骤级存在任一匹配记录,就**整体替换**站点级的该类集合(不做合并)。返工分支步骤不支持步骤级覆盖,使用自身的 `recipe_id/equipment_group_id` 与站点级绑定。
- **产品版本绑定主数据而非具体版本**:`mes_product_version` 绑定 `route_id` 与 `bom_id`;**下工单时**解析当前 ACTIVE 的 Route 版本与 BOM 版本并**固化**到工单(`route_version_id/bom_version_id`),Lot 开批时沿用。这样升 Route/BOM 版本无需重发产品版本,在制工单不受影响,新工单自动取新版本。产品版本生效前必须校验其 Route/BOM 当前有 ACTIVE 版本。
- **步骤计数单位**:每个 Route 步骤声明进入时的 `count_unit_id`;若 `is_unit_convert=1`,则 Track-Out 后计数单位切换为 `convert_to_unit_id`,且本步骤必须录入转换后数量(M6)。前道 Route 全程使用同一单位(Wafer),可不填。
- **Queue Time / Time Link**:步骤上的 `queue_time_max_min` 约束"进入本站点到 Track-In 的最长等待";`mes_route_time_link` 约束"从 A 步骤 Track-Out 到 B 步骤 Track-In 的最长间隔"。超时动作 `HOLD/WARN`;Time Link 的执行期计时按 Lot 存于 M8 `mes_lot_time_link_timer`(不用 `last_track_out_at`)。仅作用于**主流程步骤序号**;返工分支步骤有自己的 `queue_time_max_min`,不适用 Time Link。
- **设备能力与资格合一**:`mes_equipment_capability` 同时表达"设备可跑哪些站点/Recipe/产品"和"资格有效期与状态"(`VALID/SUSPENDED/EXPIRED/REVOKED`)。`SUSPENDED` 即 Equipment Inhibit(暂停该设备×Recipe/产品)。**匹配规则**:按 设备×站点 取能力记录,最具体者优先(Recipe+产品 → Recipe → 产品 → 通用 `0`);**命中的任一层为 SUSPENDED/REVOKED/EXPIRED 即拒绝**(Inhibit 不能被通用 VALID 覆盖);VALID 还需满足 `valid_from ≤ 当前时间 ≤ valid_to`(为空不限),每日任务把过期记录置 EXPIRED。某 设备×站点 没有任何能力记录时,按"站点-设备组绑定"放行。
- **设备同时加工上限**:`mes_equipment.max_concurrent_lots` 为设备总上限,`mes_equipment_port.max_concurrent` 为 Port/Chamber 级上限;M3 Interlock 以较严者为准(在持有设备行锁后统计)。
- **BOM 消耗方式**:`PER_WAFER/PER_DIE/PER_STRIP/PER_UNIT/PER_LOT/PER_RUN/PER_TIME`;反冲时按 Lot 当前计数单位与消耗方式换算用量。`PER_RUN` 指一次加工运行计一次(单 Lot 为该次出站;Batch 整批计一次,见 M5),`PER_TIME` 指按加工时长(分钟)。
- **BOM 行绑定**:`step_id` 必填,`route_seq_no` 可选(0 表示该站点每次经过都消耗;非 0 表示只在该主流程步骤消耗);返工分支步骤按 `step_id` 匹配 `route_seq_no=0` 的行。`backflush=1` 表示 Track-Out 自动扣减。
- **BOM 与 Route 的序号一致性**:BOM 与 Route 各自独立升版,`route_seq_no≠0` 的 BOM 行可能在新 Route 版本里不存在或换了站点。**下工单固化版本时**校验:BOM 版本每一行的 `step_id` 必须出现在固化的 Route 版本中;`route_seq_no≠0` 的行还要求该序号存在且其 `step_id` 与 BOM 行一致。任一不满足则拒绝下工单并列出冲突行;产品版本提交审批时对当前 ACTIVE 的 Route/BOM 做同样预检。Route 新版本生效时,对引用该 Route 的产品版本的 ACTIVE BOM 预检并产生 `SYSTEM` 预警(不阻断生效)。
- **替代料**:同一 `substitute_group` 内的多行互为替代,`substitute_priority` 小者优先;`substitute_policy` 为 `ALLOW/WARN/BLOCK`(使用非首选替代时直接放行/放行并产生预警/禁止)。不做"审批",避免反冲时序问题。

## 4. 表设计

### 4.1 新增表(15 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `hist_mes_approval_record` | 版本审批记录 | `app-mes-base` |
| `mes_step_group` | 站点组(工艺模块) | `app-mes-base` |
| `mes_route` | Route 主数据 | `app-mes-base` |
| `mes_route_version` | Route 版本 | `app-mes-base` |
| `mes_route_step` | Route 步骤 | `app-mes-base` |
| `mes_route_time_link` | Route Time Link 约束 | `app-mes-base` |
| `mes_step_equipment_group` | 站点-设备组绑定 | `app-mes-base` |
| `mes_step_recipe` | 站点-Recipe 绑定(站点级) | `app-mes-base` |
| `mes_route_step_recipe` | Route 步骤-Recipe 覆盖(步骤级,随 Route 版本) | `app-mes-base` |
| `mes_equipment_port` | 设备 Port/Chamber | `app-mes-base` |
| `mes_equipment_capability` | 设备能力与资格 | `app-mes-base` |
| `mes_bom` | BOM 主数据 | `app-mes-base` |
| `mes_bom_version` | BOM 版本 | `app-mes-base` |
| `mes_bom_item` | BOM 行 | `app-mes-base` |
| `mes_product_version` | 产品版本 | `app-mes-base` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `hist_mes_approval_record` 版本审批记录
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `biz_type` | VARCHAR(32) | 是 |  | 业务类型 PRODUCT_VERSION/ROUTE_VERSION/BOM_VERSION/RECIPE_VERSION/CONSUMPTION_ADJUST/ECN |
| `biz_id` | BIGINT | 是 |  | 业务对象ID |
| `action` | VARCHAR(16) | 是 |  | 动作 SUBMIT/APPROVE/REJECT/REVOKE |
| `from_status` | VARCHAR(16) |  |  | 变更前状态 |
| `to_status` | VARCHAR(16) |  |  | 变更后状态 |
| `assignee` | VARCHAR(64) |  |  | 指定审批人 |
| `operator` | VARCHAR(64) | 是 |  | 操作人 |
| `comment` | VARCHAR(512) |  |  | 意见 |
| `action_at` | DATETIME | 是 |  | 动作时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_biz`(tenant_id, biz_type, biz_id, action_at);索引 `idx_assignee`(tenant_id, assignee, action)

#### `mes_step_group` 站点组(工艺模块)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `group_code` | VARCHAR(64) | 是 |  | 站点组编码,如 PHOTO/ETCH/DIFF |
| `group_name` | VARCHAR(128) | 是 |  | 站点组名称 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_group_code`(tenant_id, group_code)

#### `mes_route` Route 主数据
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_code` | VARCHAR(64) | 是 |  | Route 编码 |
| `route_name` | VARCHAR(128) | 是 |  | Route 名称 |
| `description` | VARCHAR(512) |  |  | 说明 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_route_code`(tenant_id, route_code)

#### `mes_route_version` Route 版本
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_id` | BIGINT | 是 |  | Route |
| `version_no` | INT | 是 |  | 版本号 |
| `status` | VARCHAR(16) | 是 | DRAFT | DRAFT/PENDING/ACTIVE/INACTIVE |
| `copy_from_version_id` | BIGINT |  |  | 复制来源版本 |
| `effective_at` | DATETIME |  |  | 生效时间 |
| `expire_at` | DATETIME |  |  | 失效时间 |
| `submitted_by` | VARCHAR(64) |  |  | 提交人 |
| `approved_by` | VARCHAR(64) |  |  | 审批人 |
| `approved_at` | DATETIME |  |  | 审批时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_route_version`(tenant_id, route_id, version_no);索引 `idx_route_status`(tenant_id, route_id, status)

#### `mes_route_step` Route 步骤
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `seq_no` | INT | 是 |  | 步骤序号,版本内唯一,建议 10 步进 |
| `step_id` | BIGINT | 是 |  | 站点;允许在同一版本内重复出现 |
| `step_alias` | VARCHAR(128) |  |  | 步骤别名(同站点多次出现时区分,如"光刻-层1") |
| `is_required` | TINYINT(1) | 是 | 1 | 必经站点;否则可 Skip 或被抽检规则跳过 |
| `equipment_group_id` | BIGINT |  |  | 步骤指定设备组,空表示取站点级绑定 |
| `count_unit_id` | BIGINT |  |  | 进入本步骤时的计数单位,空表示沿用上一步/产品基础单位 |
| `is_unit_convert` | TINYINT(1) | 是 | 0 | 是否单位转换站点 |
| `convert_to_unit_id` | BIGINT |  |  | 转换后的计数单位 |
| `std_process_min` | DECIMAL(10,2) |  |  | 标准加工时长(分钟) |
| `queue_time_max_min` | INT |  |  | 进入本站点到 Track-In 最长等待(分钟),空表示不限 |
| `queue_time_warn_min` | INT |  |  | Queue Time 预警阈值(分钟) |
| `queue_time_action` | VARCHAR(16) |  |  | 超时动作 HOLD/WARN |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_version_seq`(tenant_id, route_version_id, seq_no);索引 `idx_step`(tenant_id, step_id)

说明:不建 `(route_version_id, step_id)` 唯一约束,以支持重入。步骤级覆盖(Recipe/工具/规格/抽检)在 `mes_route_step_*` 从属表里引用本表;Recipe 由 `mes_route_step_recipe` 承载(可多个允许项)。

#### `mes_route_time_link` Route Time Link 约束
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `from_seq_no` | INT | 是 |  | 起点步骤序号(其 Track-Out 开始计时) |
| `to_seq_no` | INT | 是 |  | 终点步骤序号(其 Track-In 结束计时) |
| `max_minutes` | INT | 是 |  | 最长间隔(分钟) |
| `warn_minutes` | INT |  |  | 预警阈值(分钟) |
| `action` | VARCHAR(16) | 是 | HOLD | 超时动作 HOLD/WARN |
| `reason_code` | VARCHAR(64) |  |  | 自动 Hold 使用的原因码 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_link`(tenant_id, route_version_id, from_seq_no, to_seq_no)

#### `mes_step_equipment_group` 站点-设备组绑定
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `step_id` | BIGINT | 是 |  | 站点 |
| `group_id` | BIGINT | 是 |  | 设备组 |
| `priority` | INT | 是 | 0 | 优先级,数小优先 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_step_group`(tenant_id, step_id, group_id)

#### `mes_step_recipe` 站点-Recipe 绑定(站点级)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `step_id` | BIGINT | 是 |  | 站点 |
| `recipe_id` | BIGINT | 是 |  | Recipe |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示通用 |
| `is_default` | TINYINT(1) | 是 | 0 | 是否默认 Recipe |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_step_recipe`(tenant_id, step_id, recipe_id, product_id)

#### `mes_route_step_recipe` Route 步骤-Recipe 覆盖(步骤级,随 Route 版本)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `route_version_id` | BIGINT | 是 |  | Route 版本 |
| `route_step_id` | BIGINT | 是 |  | Route 步骤 |
| `recipe_id` | BIGINT | 是 |  | Recipe |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示通用 |
| `is_default` | TINYINT(1) | 是 | 0 | 是否默认 Recipe |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_route_step_recipe`(tenant_id, route_step_id, recipe_id, product_id);索引 `idx_version`(tenant_id, route_version_id)

说明:存在匹配记录时整体替换站点级 `mes_step_recipe`;版本复制时一并复制,生效后只读。

#### `mes_equipment_port` 设备 Port/Chamber
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `port_code` | VARCHAR(64) | 是 |  | Port/Chamber 编码 |
| `port_name` | VARCHAR(128) |  |  | 名称 |
| `kind` | VARCHAR(16) | 是 | PORT | 类型 PORT/CHAMBER |
| `max_concurrent` | INT | 是 | 1 | 同时加工 Lot 数上限 |
| `e10_state` | VARCHAR(24) | 是 | STANDBY | Chamber 级 E10 状态(Port 一般沿用设备) |
| `status_code` | VARCHAR(64) |  |  | 细分状态码 |
| `reason_code` | VARCHAR(64) |  |  | 原因码(停机类) |
| `status_source` | VARCHAR(16) | 是 | MANUAL | 状态设置来源 MANUAL/TRACK/SYSTEM |
| `status_since` | DATETIME |  |  | 当前状态起始时间 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_eqp_port`(tenant_id, equipment_id, port_code)

#### `mes_equipment_capability` 设备能力与资格
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `step_id` | BIGINT | 是 |  | 可加工站点 |
| `recipe_id` | BIGINT | 是 | 0 | 限定 Recipe,0 表示该站点所有 Recipe |
| `product_id` | BIGINT | 是 | 0 | 限定产品,0 表示所有产品 |
| `status` | VARCHAR(16) | 是 | VALID | VALID/SUSPENDED/EXPIRED/REVOKED;SUSPENDED 即设备 Inhibit |
| `valid_from` | DATETIME |  |  | 资格生效起 |
| `valid_to` | DATETIME |  |  | 资格生效止 |
| `qualified_by` | VARCHAR(64) |  |  | 认证人 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_capability`(tenant_id, equipment_id, step_id, recipe_id, product_id);索引 `idx_step`(tenant_id, step_id);索引 `idx_status_valid`(tenant_id, status, valid_to)

说明:M2 与 M8 原有的"设备能力"和"设备资格"在此合并为一张表,避免两套优先级。

#### `mes_bom` BOM 主数据
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `bom_code` | VARCHAR(64) | 是 |  | BOM 编码 |
| `bom_name` | VARCHAR(128) | 是 |  | BOM 名称 |
| `description` | VARCHAR(512) |  |  | 说明 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_bom_code`(tenant_id, bom_code)

#### `mes_bom_version` BOM 版本
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `bom_id` | BIGINT | 是 |  | BOM |
| `version_no` | INT | 是 |  | 版本号 |
| `status` | VARCHAR(16) | 是 | DRAFT | DRAFT/PENDING/ACTIVE/INACTIVE |
| `copy_from_version_id` | BIGINT |  |  | 复制来源版本 |
| `effective_at` | DATETIME |  |  | 生效时间 |
| `expire_at` | DATETIME |  |  | 失效时间 |
| `submitted_by` | VARCHAR(64) |  |  | 提交人 |
| `approved_by` | VARCHAR(64) |  |  | 审批人 |
| `approved_at` | DATETIME |  |  | 审批时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_bom_version`(tenant_id, bom_id, version_no);索引 `idx_bom_status`(tenant_id, bom_id, status)

#### `mes_bom_item` BOM 行
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `bom_version_id` | BIGINT | 是 |  | BOM 版本 |
| `line_no` | INT | 是 |  | 行号 |
| `material_id` | BIGINT | 是 |  | 物料 |
| `qty_per` | DECIMAL(18,6) | 是 |  | 单位用量 |
| `uom_id` | BIGINT | 是 |  | 用量单位 |
| `scrap_rate` | DECIMAL(8,4) | 是 | 0 | 允许损耗率(%),仅用于需求展开与差异判定,不参与实际扣减 |
| `consume_mode` | VARCHAR(16) | 是 | PER_WAFER | PER_WAFER/PER_DIE/PER_STRIP/PER_UNIT/PER_LOT/PER_RUN/PER_TIME |
| `step_id` | BIGINT | 是 |  | 消耗站点 |
| `route_seq_no` | INT | 是 | 0 | 指定主流程步骤序号,0 表示该站点每次经过都消耗 |
| `backflush` | TINYINT(1) | 是 | 1 | Track-Out 自动反冲 |
| `is_critical` | TINYINT(1) |  |  | 覆盖物料的关键料标识,空表示沿用物料设置 |
| `substitute_group` | VARCHAR(32) |  |  | 替代组编码,同组互为替代 |
| `substitute_priority` | INT |  |  | 组内优先级,数小优先 |
| `substitute_policy` | VARCHAR(8) | 是 | ALLOW | 使用非首选替代的策略 ALLOW/WARN/BLOCK |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_version_line`(tenant_id, bom_version_id, line_no);索引 `idx_step`(tenant_id, step_id);索引 `idx_material`(tenant_id, material_id)

#### `mes_product_version` 产品版本
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `product_id` | BIGINT | 是 |  | 产品 |
| `version_no` | INT | 是 |  | 版本号 |
| `status` | VARCHAR(16) | 是 | DRAFT | DRAFT/PENDING/ACTIVE/INACTIVE |
| `route_id` | BIGINT |  |  | 绑定 Route(主数据);下工单时解析当前 ACTIVE 版本并固化到工单 |
| `bom_id` | BIGINT |  |  | 绑定 BOM(主数据);下工单时解析当前 ACTIVE 版本并固化到工单 |
| `copy_from_version_id` | BIGINT |  |  | 复制来源版本 |
| `effective_at` | DATETIME |  |  | 生效时间 |
| `expire_at` | DATETIME |  |  | 失效时间 |
| `submitted_by` | VARCHAR(64) |  |  | 提交人 |
| `approved_by` | VARCHAR(64) |  |  | 审批人 |
| `approved_at` | DATETIME |  |  | 审批时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_product_version`(tenant_id, product_id, version_no);索引 `idx_product_status`(tenant_id, product_id, status)

说明:工单、Lot 绑定到具体的 `product_version_id`;Route/BOM 的具体版本在下工单时固化到工单,在制 Lot 不受后续升版影响。

### 4.2 变更表(3 处)

#### 变更 `mes_recipe_version` Recipe 版本

状态集扩展为 DRAFT/PENDING/ACTIVE/INACTIVE;审批明细见 `hist_mes_approval_record`。

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `submitted_by` | VARCHAR(64) |  |  | 提交人 |
| `approved_by` | VARCHAR(64) |  |  | 审批人 |
| `approved_at` | DATETIME |  |  | 审批时间 |

#### 变更 `mes_step` 站点

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `step_group_id` | BIGINT |  |  | 所属站点组 |

新增索引:索引 `idx_step_group`(tenant_id, step_group_id)

#### 变更 `mes_equipment` 设备台账

设备总上限;Port/Chamber 另有各自上限。

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `max_concurrent_lots` | INT | 是 | 1 | 设备同时加工 Lot 数上限 |

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/approvals`:GET 待我审批;POST `/submit`、`/approve`、`/reject`(按 bizType+bizId)
- `/api/v1/mes/products/{id}/versions` CRUD;POST `…/{vid}/copy`、`/submit`
- `/api/v1/mes/routes`、`/routes/{id}/versions`、`/route-versions/{vid}/steps`、`/time-links` CRUD;POST `…/copy`
- `/api/v1/mes/route-steps/{id}/bindings` Route 步骤级覆盖(Recipe/工具/规格/抽检)
- `/api/v1/mes/steps/{id}/equipment-groups`、`/steps/{id}/recipes` 站点级绑定维护;`/api/v1/mes/step-groups` CRUD
- `/api/v1/mes/equipment/{id}/ports`、`/capabilities` CRUD
- `/api/v1/mes/boms`、`/boms/{id}/versions`、`/bom-versions/{vid}/items` CRUD;POST `…/copy`、`/submit`

## 6. 验收标准

1. 为一个产品配出完整 Route(含重复站点、单位转换站点)与 BOM,提交审批并生效
2. 生效后版本不可修改,复制出新版本可编辑,且本里程碑已有的从属数据(步骤、Time Link、`mes_route_step_recipe`)一并复制;返工/条件跳转(M4)、工具覆盖(M8)、采集/抽检覆盖(M10)在各自里程碑把复制逻辑补入并验收
3. 同一 Route/BOM/Recipe/产品任意时刻只有一个 ACTIVE 版本(含并发生效)
4. 同一站点出现两次、指定不同 Recipe 的 Route:第 2 层用第 1 层 Recipe 时 Track-In 被拒绝
5. 升级 Route/BOM 版本后无需重发产品版本:新工单自动取新 ACTIVE 版本,已下达工单与在制 Lot 仍按原版本
6. 步骤级覆盖随 Route 版本复制;已生效版本的步骤级覆盖不可修改
7. Route 步骤的站点 `need_equipment=1` 但未配置可用设备组时,版本提交审批被拒绝;`need_equipment=0` 的站点不受此限
8. BOM 行的 `step_id/route_seq_no` 与固化的 Route 版本不一致时,下工单被拒绝并列出冲突行
9. 同一设备×站点同时有指定 Recipe 的 SUSPENDED 记录与通用 VALID 记录时,该 Recipe 进站被拒绝;过期能力记录不再放行

## 7. 待确认

1. 产品版本是否需要独立的"生效日期区间"?当前仅 `effective_at/expire_at` 两个时间点。
2. Time Link 是否需要支持"跨 Route 版本"?当前限定同一 Route 版本内。
