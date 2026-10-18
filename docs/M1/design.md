# M1 主数据 I:独立实体

> 状态:**设计草案,待核对**(未实现)· 依赖:M0 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_1__mes_m1.sql`)

## 1. 目标

落地不依赖其他 MES 实体的主数据:计数单位、产品、站点、设备、物料、Recipe。设备状态可手工切换并留历史。

## 2. 范围

- 计数单位、物料单位与换算
- 产品/产品族/客户/产品换算系数
- 区域、站点(含是否需要设备/Recipe 标志)
- 设备组、设备台账、E10 状态与历史
- 物料主数据
- Recipe 主数据与版本(草稿/生效/失效)

## 3. 设计规则

- **唯一键**:各主数据 `*_code` 在租户内唯一;物理删除前必须无引用(`DeleteReferenceUtils`),否则只能停用。
- **"空表示通用"一律用 0 哨兵**:凡出现在唯一键里的"可选限定"(产品、Recipe、物料、Route 步骤等),列定义为 `BIGINT NOT NULL DEFAULT 0`,`0` 表示通用/不限;不用 NULL,因为 MySQL 唯一键对 NULL 不去重会产生重复行。
- **计数单位 vs 物料单位**:`mes_count_unit` 表示 Lot 的"件"(Wafer/Die/Strip/Unit/Tray/Reel…),用于生产数量;`mes_uom` 表示物料计量(mL/kg/片/盒…),用于库存与 BOM。二者独立。
- **产品换算系数**:`mes_product_unit_conv` 记录"1 个 from 单位 = factor 个 to 单位",如 1 Wafer = 1200 Die、1 Strip = 40 Unit。转换站点录入实际数量时,以该系数得到参考值并校验偏差。
- **站点是否需要设备/Recipe**:`mes_step.need_equipment/need_recipe` 默认都为 1;目检、HOLD、SHIP、仓储类站点设为 0,此时 Track-In/Out 不选设备/Recipe,Interlock 的设备/Recipe 规则不适用,版本完整性校验也不要求绑定设备组/Recipe。
- **E10 状态**:`e10_state` 为 6 个标准大类(PRODUCTIVE/STANDBY/ENGINEERING/SCHEDULED_DOWN/UNSCHEDULED_DOWN/NON_SCHEDULED);`status_code` 为厂内细分(字典 `MES_EQP_STATUS_CODE`);停机类状态必须带 `reason_code`(字典 `MES_EQP_STOP_REASON`)。`status_source`(MANUAL/TRACK/SYSTEM)记录当前状态由谁设置,M3 的 Track 联动据此不覆盖人工状态。
- **设备状态变更**:写 `hist_mes_equipment_status`——关闭上一段(回填 `end_at`、`duration_sec`),新开一段;同时更新 `mes_equipment` 上的当前状态冗余字段。M3 起 Track-In/Out 自动联动(`source=TRACK`)。其他模块改设备状态只经契约 `share.integration.mes.base.EquipmentStatusProvider`(传目标状态、`status_source`、原因码与 `txn_id`):exec 的 Track 联动用 `TRACK`,maint 的 PM 开始/Qual 通过用 `SYSTEM`。Chamber 级状态在 M2 的 `mes_equipment_port` 上维护,历史用 `port_id` 区分(0 表示设备级)。
- **Recipe 版本不可变**:状态 ACTIVE 后不可修改内容,只能复制出新版本;**同一 Recipe 同时只允许一个 ACTIVE**,新版本生效时旧 ACTIVE 自动转 INACTIVE(在制 Lot 仍引用旧版本 ID)。生效时先对 `mes_recipe` 主表行 `FOR UPDATE NOWAIT`,再降级旧 ACTIVE、升级新版本,并发生效也只会有一个 ACTIVE。审批(PENDING,驳回回到 DRAFT)在 M2 增加,届时 `activate` 接口只由审批通过触发。
- **产品版本不在本里程碑**:`mes_product` 是静态主体,产品版本、Route/BOM 绑定在 M2。

## 4. 表设计

### 4.1 新增表(15 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_count_unit` | 计数单位 | `app-mes-base` |
| `mes_uom` | 物料计量单位 | `app-mes-base` |
| `mes_uom_conversion` | 物料单位换算 | `app-mes-base` |
| `mes_product_family` | 产品族 | `app-mes-base` |
| `mes_customer` | 客户 | `app-mes-base` |
| `mes_product` | 产品/料号 | `app-mes-base` |
| `mes_product_unit_conv` | 产品计数单位换算系数 | `app-mes-base` |
| `mes_area` | 区域/Bay | `app-mes-base` |
| `mes_step` | 站点 | `app-mes-base` |
| `mes_equipment_group` | 设备组 | `app-mes-base` |
| `mes_equipment` | 设备台账 | `app-mes-base` |
| `hist_mes_equipment_status` | 设备状态历史(时间段表) | `app-mes-base` |
| `mes_material` | 物料主数据 | `app-mes-base` |
| `mes_recipe` | Recipe 主数据 | `app-mes-base` |
| `mes_recipe_version` | Recipe 版本 | `app-mes-base` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_count_unit` 计数单位
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `unit_code` | VARCHAR(32) | 是 |  | 单位编码 WAFER/DIE/STRIP/UNIT/FRAME/TRAY/REEL/TUBE… |
| `unit_name` | VARCHAR(64) | 是 |  | 单位名称 |
| `unit_type` | VARCHAR(16) | 是 |  | 类别 WAFER/DIE/STRIP/UNIT/CARRIER/OTHER |
| `is_base` | TINYINT(1) | 是 | 0 | 是否基础单位(最小可计数单位) |
| `sort_no` | INT | 是 | 0 | 排序号 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_unit_code`(tenant_id, unit_code)

#### `mes_uom` 物料计量单位
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `uom_code` | VARCHAR(32) | 是 |  | 单位编码 |
| `uom_name` | VARCHAR(64) | 是 |  | 单位名称 |
| `dimension` | VARCHAR(16) | 是 |  | 量纲 COUNT/VOLUME/WEIGHT/LENGTH/AREA/TIME/OTHER |
| `decimals` | TINYINT | 是 | 0 | 默认小数位数 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_uom_code`(tenant_id, uom_code)

#### `mes_uom_conversion` 物料单位换算
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `from_uom_id` | BIGINT | 是 |  | 源单位 |
| `to_uom_id` | BIGINT | 是 |  | 目标单位 |
| `material_id` | BIGINT | 是 | 0 | 限定物料,0 表示通用 |
| `factor` | DECIMAL(18,6) | 是 |  | 换算系数:1 个源单位 = factor 个目标单位 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_conv`(tenant_id, from_uom_id, to_uom_id, material_id)

说明:查找顺序:限定物料 → 通用(`material_id=0`)。

#### `mes_product_family` 产品族
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `family_code` | VARCHAR(64) | 是 |  | 产品族编码 |
| `family_name` | VARCHAR(128) | 是 |  | 产品族名称 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_family_code`(tenant_id, family_code)

#### `mes_customer` 客户
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `customer_code` | VARCHAR(64) | 是 |  | 客户编码 |
| `customer_name` | VARCHAR(128) | 是 |  | 客户名称 |
| `contact` | VARCHAR(128) |  |  | 联系人 |
| `phone` | VARCHAR(64) |  |  | 电话 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_customer_code`(tenant_id, customer_code)

#### `mes_product` 产品/料号
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `product_code` | VARCHAR(64) | 是 |  | 产品料号 |
| `product_name` | VARCHAR(128) | 是 |  | 产品名称 |
| `product_type` | VARCHAR(16) | 是 | WAFER | 产品类型 WAFER/DIE/PACKAGE/OTHER |
| `family_id` | BIGINT |  |  | 产品族 |
| `customer_id` | BIGINT |  |  | 客户 |
| `customer_part_no` | VARCHAR(64) |  |  | 客户料号 |
| `wafer_size_mm` | INT |  |  | Wafer 尺寸(mm):150/200/300 |
| `gross_die_per_wafer` | INT |  |  | 每 Wafer 毛 Die 数 |
| `base_unit_id` | BIGINT | 是 |  | 基础计数单位,开批默认单位 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_product_code`(tenant_id, product_code);索引 `idx_family`(tenant_id, family_id);索引 `idx_customer`(tenant_id, customer_id)

#### `mes_product_unit_conv` 产品计数单位换算系数
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `product_id` | BIGINT | 是 |  | 产品 |
| `from_unit_id` | BIGINT | 是 |  | 源计数单位 |
| `to_unit_id` | BIGINT | 是 |  | 目标计数单位 |
| `factor` | DECIMAL(18,6) | 是 |  | 1 个源单位 = factor 个目标单位(参考值) |
| `tolerance_pct` | DECIMAL(8,4) | 是 | 5 | 允许偏差百分比,超出需确认或阻断 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_conv`(tenant_id, product_id, from_unit_id, to_unit_id)

#### `mes_area` 区域/Bay
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `area_code` | VARCHAR(64) | 是 |  | 区域编码 |
| `area_name` | VARCHAR(128) | 是 |  | 区域名称 |
| `parent_id` | BIGINT |  |  | 上级区域 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_area_code`(tenant_id, area_code);索引 `idx_parent`(tenant_id, parent_id)

#### `mes_step` 站点
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `step_code` | VARCHAR(64) | 是 |  | 站点编码 |
| `step_name` | VARCHAR(128) | 是 |  | 站点名称 |
| `step_type` | VARCHAR(16) | 是 | PROCESS | 站点类型 PROCESS/MEASURE/INSPECT/HOLD/SHIP/CONVERT/PACK/STORE |
| `area_id` | BIGINT |  |  | 所属区域 |
| `need_equipment` | TINYINT(1) | 是 | 1 | 是否需要选择设备;目检/HOLD/SHIP/仓储类站点为 0 |
| `need_recipe` | TINYINT(1) | 是 | 1 | 是否需要选择 Recipe |
| `description` | VARCHAR(512) |  |  | 工艺说明 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_step_code`(tenant_id, step_code);索引 `idx_area`(tenant_id, area_id)

说明:`CONVERT` 为单位转换站点(M6),`STORE` 为 Die Bank/成品仓类仓储站点。Queue Time/标准工时在 Route 步骤上配置(M2),因为同一站点在不同 Route/不同层可不同。

#### `mes_equipment_group` 设备组
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `group_code` | VARCHAR(64) | 是 |  | 设备组编码 |
| `group_name` | VARCHAR(128) | 是 |  | 设备组名称 |
| `area_id` | BIGINT |  |  | 所属区域 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_group_code`(tenant_id, group_code)

#### `mes_equipment` 设备台账
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `equipment_code` | VARCHAR(64) | 是 |  | 设备编码 |
| `equipment_name` | VARCHAR(128) | 是 |  | 设备名称 |
| `equipment_type` | VARCHAR(16) | 是 | PROCESS | 设备类型 PROCESS/MEASURE/TEST/PACK/TRANSPORT/OTHER |
| `group_id` | BIGINT |  |  | 设备组 |
| `area_id` | BIGINT |  |  | 所属区域 |
| `vendor` | VARCHAR(64) |  |  | 厂商 |
| `model` | VARCHAR(64) |  |  | 型号 |
| `serial_no` | VARCHAR(64) |  |  | 序列号 |
| `e10_state` | VARCHAR(24) | 是 | STANDBY | 当前 E10 大类 |
| `status_code` | VARCHAR(64) |  |  | 当前细分状态码 |
| `reason_code` | VARCHAR(64) |  |  | 当前原因码(停机类) |
| `status_source` | VARCHAR(16) | 是 | MANUAL | 当前状态设置来源 MANUAL/TRACK/SYSTEM |
| `status_since` | DATETIME |  |  | 当前状态起始时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_equipment_code`(tenant_id, equipment_code);索引 `idx_group`(tenant_id, group_id);索引 `idx_area`(tenant_id, area_id);索引 `idx_e10`(tenant_id, e10_state)

#### `hist_mes_equipment_status` 设备状态历史(时间段表)
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `equipment_id` | BIGINT | 是 |  | 设备 |
| `port_id` | BIGINT | 是 | 0 | Port/Chamber,0 表示设备级 |
| `from_e10_state` | VARCHAR(24) |  |  | 变更前 E10 大类 |
| `to_e10_state` | VARCHAR(24) | 是 |  | 变更后 E10 大类 |
| `status_code` | VARCHAR(64) |  |  | 细分状态码 |
| `reason_code` | VARCHAR(64) |  |  | 原因码 |
| `start_at` | DATETIME | 是 |  | 本段起始时间 |
| `end_at` | DATETIME |  |  | 本段结束时间,空表示进行中 |
| `duration_sec` | BIGINT |  |  | 时长(秒),段结束时回填 |
| `source` | VARCHAR(16) | 是 | MANUAL | 来源 MANUAL/TRACK/SYSTEM |
| `lot_id` | BIGINT |  |  | 关联 Lot(Track 联动时) |
| `txn_id` | BIGINT |  |  | 产生本段的事务ID(纠错时据此判断当前段是否仍由该事务产生) |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_eqp_start`(tenant_id, equipment_id, port_id, start_at);索引 `idx_state_start`(tenant_id, to_e10_state, start_at)

说明:时间段表:只允许回填 `end_at/duration_sec`,不改其他列。同一设备(同一 Port)同一时刻只能有一段 `end_at IS NULL`。E10 统计、OEE 全部基于本表。

#### `mes_material` 物料主数据
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `material_code` | VARCHAR(64) | 是 |  | 物料编码 |
| `material_name` | VARCHAR(128) | 是 |  | 物料名称 |
| `material_type` | VARCHAR(16) | 是 | RAW | 物料类型 WAFER/RAW/AUX/CHEMICAL/GAS/PACKAGE/OTHER |
| `spec` | VARCHAR(256) |  |  | 规格型号 |
| `base_uom_id` | BIGINT | 是 |  | 基础计量单位 |
| `is_critical` | TINYINT(1) | 是 | 0 | 关键料:缺料/过期时 Interlock 阻断 |
| `shelf_life_days` | INT |  |  | 保质期(天) |
| `need_incoming_inspect` | TINYINT(1) | 是 | 0 | 是否需要来料检验 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_material_code`(tenant_id, material_code);索引 `idx_type`(tenant_id, material_type)

#### `mes_recipe` Recipe 主数据
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `recipe_code` | VARCHAR(64) | 是 |  | Recipe 编码 |
| `recipe_name` | VARCHAR(128) | 是 |  | Recipe 名称 |
| `equipment_type` | VARCHAR(16) |  |  | 适用设备类型 |
| `description` | VARCHAR(512) |  |  | 说明 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_recipe_code`(tenant_id, recipe_code)

说明:MES 只管 Recipe 元数据与版本,不保存设备侧 Recipe Body。

#### `mes_recipe_version` Recipe 版本
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `recipe_id` | BIGINT | 是 |  | Recipe |
| `version_no` | INT | 是 |  | 版本号,从 1 递增 |
| `status` | VARCHAR(16) | 是 | DRAFT | 状态 DRAFT/ACTIVE/INACTIVE(M2 增加 PENDING) |
| `param_desc` | JSON |  |  | 参数说明(键值对,仅供查阅与校验) |
| `effective_at` | DATETIME |  |  | 生效时间 |
| `expire_at` | DATETIME |  |  | 失效时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_recipe_version`(tenant_id, recipe_id, version_no);索引 `idx_recipe_status`(tenant_id, recipe_id, status)

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/count-units`、`/uoms`、`/uom-conversions` CRUD
- `/api/v1/mes/product-families`、`/customers`、`/products`、`/products/{id}/unit-conversions` CRUD
- `/api/v1/mes/areas`、`/steps` CRUD
- `/api/v1/mes/equipment-groups`、`/equipment` CRUD
- POST `/api/v1/mes/equipment/{id}/status` 手工变更状态;GET `/api/v1/mes/equipment/{id}/status-history`
- `/api/v1/mes/materials` CRUD
- `/api/v1/mes/recipes`、`/recipes/{id}/versions` CRUD;POST `/recipes/{id}/versions/{vid}/activate` 生效(M2 起由审批通过触发,不再直接调用)

## 6. 验收标准

1. 创建一套产品(含 Wafer→Die 换算)、站点、设备、物料、Recipe
2. 设备状态手工切换,历史时间段连续无重叠,时长累加正确
3. Recipe ACTIVE 版本不可编辑,复制出新版本后可编辑并生效,旧版本自动失效,且任何时刻只有一个 ACTIVE
4. 被引用的主数据删除被拒绝并提示引用方

## 7. 待确认

1. 产品是否需要"产品类型"区分(Wafer 产品/封装产品)?当前设计为 `product_type` 字段,取值 WAFER/DIE/PACKAGE/OTHER。
2. 设备是否需要"资产编号"?当前仅保留 `serial_no`。
