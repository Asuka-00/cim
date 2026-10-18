# M7 轻量库房

> 状态:**设计草案,待核对**(未实现)· 依赖:M1;领料单关联工单经 share.integration.mes.exec.WorkOrderProvider,M3 完成前 wo_id 可留空 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_7__mes_m7.sql`)

## 1. 目标

物料有入口、有库位、有库存,为后续反冲与追溯打底。不做采购、成本、复杂库内作业(那是 WMS/ERP 范围)。

## 2. 范围

- 库房/库位(含线边库位与设备/区域的关联)
- 供应商
- 物料批与状态
- 来料收货与检验
- 来料 Wafer 清单
- 库存与流水
- 领料/退料/转移
- 冻结/过期

## 3. 设计规则

- **库存口径**:`mes_inventory` 按 `(物料批, 库位)` 记账,`qty_available = qty_on_hand − qty_frozen`;所有变动经 `MesInventoryService` 一个入口,同事务写 `hist_mes_inventory_txn`(`txn_id` 必填)。对其他模块经契约 `share.integration.mes.warehouse.InventoryProvider` 暴露(扣减/回补/物料批状态重算/来料 Wafer 清单置 USED),调用方在自己的业务事务内调用。
- **单据确认**:收货、领/退/调拨单的确认走统一事务层 `MesTransactionService`(带 `request_id` 幂等),先对单头 `FOR UPDATE NOWAIT` 并校验 `status=DRAFT`,重复确认被拒绝;领/退料确认同事务发布 `MATERIAL_ISSUED`(M0 事件目录)。
- **物料批状态**:`PENDING_QC`(待检)→ `AVAILABLE`(合格可用);`IN_USE`(库存已为 0 但仍有 ACTIVE 的设备端绑定)、`CONSUMED`(库存与设备端均为 0);旁路:`QUARANTINE`(隔离)、`FROZEN`(整批冻结)、`EXPIRED`(过期)。`AVAILABLE/IN_USE/CONSUMED` 由库存与 exec 调用 `recalcStatus` 时传入的绑定情况推导(warehouse 不读 exec 表),不在上料时直接置 `CONSUMED`。仅 `AVAILABLE` 可领料/上料。
- **批号与冻结口径**:物料批号 `lot_code` 在租户内唯一(终端只扫批号即可定位),供应商批号另存;冻结两层口径——批级 `status=FROZEN` 表示整批不可用,库存行的 `qty_frozen` 表示该库位的部分冻结(如待判定数量);盘点期间的冻结用 `mes_object_lock`(M13),不用 `qty_frozen`,两者不混用。
- **来料收货**:一次收货单 `mes_receipt` 含多行 `mes_receipt_item`;每行生成或关联物料批,入指定库位。`need_incoming_inspect=1` 的物料批初始 `PENDING_QC`,检验通过转 `AVAILABLE`,不合格转 `QUARANTINE`,特采转 `AVAILABLE` 并留标记。
- **领料/退料**:统一用 `mes_issue_order`(`order_type=ISSUE/RETURN/TRANSFER`)。领料从库房库位到线边库位,扣源库存、加目标库存;退料反向。线边库存即 `wh_type=LINESIDE` 库房下的库位。
- **过期处理**:定时任务把 `expire_date < today` 且仍有库存的物料批置 `EXPIRED`,同时产生 `MATERIAL_EXPIRED` 内部事件;过期批不可领用,处理方式为报废调整(`ADJUST`)。
- **先进先出建议**:领料界面按 `expire_date, received_at` 排序给出推荐批次,不强制。
- **线边库位**:`mes_location.area_id/equipment_id` 把线边库位关联到区域或设备旁;M9 反冲直耗料时按"设备旁库位 → 区域库位"顺序、先进先出扣减。
- **来料 Wafer 清单**:客户随货的 WaferID 清单在收货时录入 `mes_incoming_wafer`(状态 AVAILABLE),M9 开批时选取并自动带出,避免手工重输;开批后回填 `wafer_id` 置 `USED`。
- **不做**:采购订单、供应商评价、库内上架策略、成本核算。

## 4. 表设计

### 4.1 新增表(12 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_warehouse` | 库房 | `app-mes-warehouse` |
| `mes_location` | 库位 | `app-mes-warehouse` |
| `mes_supplier` | 供应商 | `app-mes-warehouse` |
| `mes_material_lot` | 物料批 | `app-mes-warehouse` |
| `mes_inventory` | 库存 | `app-mes-warehouse` |
| `hist_mes_inventory_txn` | 库存流水 | `app-mes-warehouse` |
| `mes_receipt` | 收货单 | `app-mes-warehouse` |
| `mes_receipt_item` | 收货明细 | `app-mes-warehouse` |
| `mes_incoming_inspection` | 来料检验 | `app-mes-warehouse` |
| `mes_issue_order` | 领退料/转移单 | `app-mes-warehouse` |
| `mes_issue_item` | 领退料明细 | `app-mes-warehouse` |
| `mes_incoming_wafer` | 来料 Wafer 清单 | `app-mes-warehouse` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_warehouse` 库房
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wh_code` | VARCHAR(64) | 是 |  | 库房编码 |
| `wh_name` | VARCHAR(128) | 是 |  | 库房名称 |
| `wh_type` | VARCHAR(16) | 是 | RAW | 类型 RAW/LINESIDE/QUARANTINE/OTHER |
| `area_id` | BIGINT |  |  | 所属区域 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_wh_code`(tenant_id, wh_code)

#### `mes_location` 库位
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wh_id` | BIGINT | 是 |  | 库房 |
| `loc_code` | VARCHAR(64) | 是 |  | 库位编码 |
| `loc_name` | VARCHAR(128) |  |  | 库位名称 |
| `parent_id` | BIGINT |  |  | 上级库位 |
| `loc_type` | VARCHAR(16) | 是 | SHELF | 类型 SHELF/CABINET/FLOOR/BIN/EQUIPMENT_SIDE |
| `area_id` | BIGINT |  |  | 关联区域(线边库位) |
| `equipment_id` | BIGINT |  |  | 关联设备(设备旁库位) |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_wh_loc`(tenant_id, wh_id, loc_code)

#### `mes_supplier` 供应商
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `supplier_code` | VARCHAR(64) | 是 |  | 供应商编码 |
| `supplier_name` | VARCHAR(128) | 是 |  | 供应商名称 |
| `contact` | VARCHAR(128) |  |  | 联系人 |
| `phone` | VARCHAR(64) |  |  | 电话 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_supplier_code`(tenant_id, supplier_code)

#### `mes_material_lot` 物料批
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_code` | VARCHAR(64) | 是 |  | 物料批号 |
| `material_id` | BIGINT | 是 |  | 物料 |
| `supplier_id` | BIGINT |  |  | 供应商 |
| `supplier_lot_no` | VARCHAR(64) |  |  | 供应商批号 |
| `mfg_date` | DATE |  |  | 生产日期 |
| `expire_date` | DATE |  |  | 到期日期 |
| `received_qty` | DECIMAL(18,4) | 是 | 0 | 收货数量 |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `status` | VARCHAR(16) | 是 | PENDING_QC | PENDING_QC/AVAILABLE/IN_USE/QUARANTINE/FROZEN/EXPIRED/CONSUMED |
| `qc_result` | VARCHAR(16) |  |  | 检验结论 PASS/FAIL/CONCESSION |
| `opened_at` | DATETIME |  |  | 开封时间 |
| `open_expire_at` | DATETIME |  |  | 开封后失效时间(M9) |
| `thaw_start_at` | DATETIME |  |  | 开始回温时间(M9) |
| `received_at` | DATETIME |  |  | 收货时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_lot_code`(tenant_id, lot_code);索引 `idx_material`(tenant_id, material_id);索引 `idx_status_expire`(tenant_id, status, expire_date);索引 `idx_supplier`(tenant_id, supplier_id)

#### `mes_inventory` 库存
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `material_lot_id` | BIGINT | 是 |  | 物料批 |
| `material_id` | BIGINT | 是 |  | 物料(冗余,便于汇总) |
| `location_id` | BIGINT | 是 |  | 库位 |
| `qty_on_hand` | DECIMAL(18,4) | 是 | 0 | 在库数量 |
| `qty_frozen` | DECIMAL(18,4) | 是 | 0 | 冻结数量 |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_lot_loc`(tenant_id, material_lot_id, location_id);索引 `idx_material`(tenant_id, material_id);索引 `idx_location`(tenant_id, location_id)

#### `hist_mes_inventory_txn` 库存流水
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `txn_type` | VARCHAR(16) | 是 |  | RECEIVE/ISSUE/RETURN/TRANSFER/ADJUST/FREEZE/UNFREEZE/CONSUME/SCRAP/COUNT_ADJ |
| `material_lot_id` | BIGINT | 是 |  | 物料批 |
| `material_id` | BIGINT | 是 |  | 物料 |
| `from_location_id` | BIGINT |  |  | 源库位 |
| `to_location_id` | BIGINT |  |  | 目标库位 |
| `qty` | DECIMAL(18,4) | 是 |  | 变动数量(正数) |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `balance_after` | DECIMAL(18,4) |  |  | 变动后该批该库位在库量 |
| `ref_type` | VARCHAR(16) |  |  | 关联单据 RECEIPT/ISSUE/WORK_ORDER/LOT/BINDING/STOCKTAKE |
| `ref_id` | BIGINT |  |  | 关联单据ID |
| `txn_id` | BIGINT | 是 |  | 事务ID(所有库存变动都经事务层) |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot_time`(tenant_id, material_lot_id, occurred_at);索引 `idx_material_time`(tenant_id, material_id, occurred_at);索引 `idx_ref`(tenant_id, ref_type, ref_id)

#### `mes_receipt` 收货单
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `receipt_no` | VARCHAR(64) | 是 |  | 收货单号 |
| `supplier_id` | BIGINT |  |  | 供应商 |
| `status` | VARCHAR(16) | 是 | DRAFT | DRAFT/CONFIRMED/CANCELLED |
| `received_by` | VARCHAR(64) |  |  | 收货人 |
| `received_at` | DATETIME |  |  | 收货时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_receipt_no`(tenant_id, receipt_no);索引 `idx_supplier`(tenant_id, supplier_id)

#### `mes_receipt_item` 收货明细
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `receipt_id` | BIGINT | 是 |  | 收货单 |
| `line_no` | INT | 是 |  | 行号 |
| `material_id` | BIGINT | 是 |  | 物料 |
| `material_lot_id` | BIGINT |  |  | 生成/关联的物料批 |
| `supplier_lot_no` | VARCHAR(64) |  |  | 供应商批号 |
| `mfg_date` | DATE |  |  | 生产日期 |
| `expire_date` | DATE |  |  | 到期日期 |
| `qty` | DECIMAL(18,4) | 是 |  | 收货数量 |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `location_id` | BIGINT | 是 |  | 入库库位 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_receipt_line`(tenant_id, receipt_id, line_no)

#### `mes_incoming_inspection` 来料检验
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `material_lot_id` | BIGINT | 是 |  | 物料批 |
| `result` | VARCHAR(16) | 是 |  | PASS/FAIL/CONCESSION |
| `inspector` | VARCHAR(64) |  |  | 检验人 |
| `inspected_at` | DATETIME | 是 |  | 检验时间 |
| `items_json` | JSON |  |  | 检验项与结果 |
| `remark` | VARCHAR(512) |  |  | 备注/特采说明 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_material_lot`(tenant_id, material_lot_id)

#### `mes_issue_order` 领退料/转移单
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `order_no` | VARCHAR(64) | 是 |  | 单号 |
| `order_type` | VARCHAR(16) | 是 | ISSUE | ISSUE/RETURN/TRANSFER |
| `wo_id` | BIGINT |  |  | 关联工单 |
| `step_id` | BIGINT |  |  | 关联站点 |
| `from_location_id` | BIGINT |  |  | 源库位 |
| `to_location_id` | BIGINT |  |  | 目标库位 |
| `status` | VARCHAR(16) | 是 | DRAFT | DRAFT/CONFIRMED/CANCELLED |
| `requested_by` | VARCHAR(64) |  |  | 申请人 |
| `confirmed_by` | VARCHAR(64) |  |  | 确认人 |
| `confirmed_at` | DATETIME |  |  | 确认时间 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_order_no`(tenant_id, order_no);索引 `idx_wo`(tenant_id, wo_id);索引 `idx_status`(tenant_id, status)

#### `mes_issue_item` 领退料明细
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `order_id` | BIGINT | 是 |  | 单据 |
| `line_no` | INT | 是 |  | 行号 |
| `material_id` | BIGINT | 是 |  | 物料 |
| `material_lot_id` | BIGINT | 是 |  | 物料批 |
| `qty` | DECIMAL(18,4) | 是 |  | 数量 |
| `uom_id` | BIGINT | 是 |  | 计量单位 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_order_line`(tenant_id, order_id, line_no);索引 `idx_material_lot`(tenant_id, material_lot_id)

#### `mes_incoming_wafer` 来料 Wafer 清单
所属模块:`app-mes-warehouse`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `material_lot_id` | BIGINT | 是 |  | 所属来料物料批 |
| `customer_id` | BIGINT |  |  | 客户 |
| `vendor_wafer_id` | VARCHAR(64) | 是 |  | 客户/来料原始 Wafer 编号 |
| `slot_no` | INT |  |  | 来料 Slot 号 |
| `status` | VARCHAR(16) | 是 | AVAILABLE | AVAILABLE/USED/SCRAPPED |
| `wafer_id` | BIGINT |  |  | 开批后对应的 Wafer |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_lot_vendor_wafer`(tenant_id, material_lot_id, vendor_wafer_id);索引 `idx_status`(tenant_id, status);索引 `idx_wafer`(tenant_id, wafer_id)

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/warehouses`、`/locations`、`/suppliers` CRUD
- `/api/v1/mes/material-lots` 查询/详情;POST `…/{id}/freeze`、`/unfreeze`、`/quarantine`
- `/api/v1/mes/receipts` CRUD;POST `…/{id}/confirm`;POST `/api/v1/mes/material-lots/{id}/inspection`
- `/api/v1/mes/issue-orders` CRUD;POST `…/{id}/confirm`(领料/退料/转移)
- GET `/api/v1/mes/inventory`(按物料/批/库位/状态);GET `/api/v1/mes/inventory/transactions`

## 6. 验收标准

1. 来料收货入库 → 检验通过 → 领到线边 → 退回库房,库存与流水一致
2. 不合格自动隔离,隔离批不可领用
3. 过期批自动标记,且产生内部事件
4. 冻结数量不可领用,解冻后可用;批级冻结与库位级部分冻结互不混用
5. 随货 WaferID 清单收货后,开批时可直接选取并自动带出,使用后状态为 USED
6. 同一单据并发或重复确认只入账一次

## 7. 待确认

1. 物料批号是否沿用供应商批号?当前设计:系统生成 `lot_code` + 保留 `supplier_lot_no`。
2. 线边库位是否需要细分到设备旁(如"设备 E01 旁暂存")?当前仅到库位粒度。
