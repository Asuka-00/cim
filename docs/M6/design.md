# M6 封测后段:单位转换与载具

> 状态:**设计草案,待核对**(未实现)· 依赖:M4 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_6__mes_m6.sql`)

## 1. 目标

支持封测从 Wafer 到 Die、Strip、Unit 的计数与载具转换,数量守恒、追溯不断链;包含 Wafer Map 导入与 Bin 数量记录,以及 Wafer Lot→封装 Lot 的形态转换。

## 2. 范围

- 单位转换事务与历史
- Die Bin 数量、Wafer Map 导入(支持重测版本)、按 Bin 拆批
- Wafer Lot→封装 Lot 形态转换与合并投料
- Strip/Frame/Magazine 载具与 Tray/Reel/Tube/Box 包装容器
- Unit 序列号(可选)

## 3. 设计规则

- **D15 粒度**:Die 默认不建 ID,按数量与 Bin 管理;Strip 建 ID(`mes_strip`);Tray/Reel/Tube/Box 建包装容器(`mes_pack_container`);Unit 序列号由产品开关(`mes_product.track_serial`)控制是否落 `mes_unit_serial`。
- **单位转换站点的出站**:转换在 Track-Out 同一事务内完成(单一入口,不另设 `/convert` 端点)。转换站点 Track-Out 必须录入转换后数量:系统取 `mes_product_unit_conv` 计算参考值 = 转换前数量 × factor,偏差超过 `tolerance_pct` 需操作员录入原因确认,或被阻断(`mes.convert.deviation.block`)。
- **数量守恒与单位切换**:转换后 Lot 记录不变,`qty_unit_id` 切换为目标单位,`qty` 更新为实际转换数量;`current_scrap_qty` 的旧值写入转换历史后归零;原 Wafer 置 `CONVERTED`。`scrap_qty`(起始单位口径)与 `initial_qty` 保留用于工单。
- **不可逆**:单位转换、形态转换不允许 Undo(M4 纠错通则),错误只能通过转换历史审计后用 Scrap/调整处理。
- **工单完工口径**:工单按起始单位核算,按 M3 的终态分组累计:**完成态**(COMPLETED/SHIPPED/CONSUMED)的 Lot 把 `initial_qty - scrap_qty` 计入 `completed_qty`;**TERMINATED** 的 Lot 把 `initial_qty - scrap_qty` 计入 `terminated_qty`(不算完成);MERGED 的 Lot 因起始数量已并入目标 Lot,贡献为 0;所有 Lot 的 `scrap_qty` 计入工单 `scrap_qty`。转换后的 Die/Unit 级报废不计入工单报废。完工条件:所有 Lot 为终态且 `completed_qty + scrap_qty + terminated_qty ≥ plan_qty`(可由工单参数放宽)。
- **副数量**:封测 Lot 经 Strip 化后同时关心 Strip 数与 Unit 数:`mes_lot.sub_qty/sub_unit_id` 记录副数量(如以 Strip 为主单位时的 Unit 总数),`mes_strip` 逐条记录良品数(X-out)。
- **Wafer Map 导入**:解析文件得到每片的 Bin 分布与良/不良 Die 数,落 `mes_wafer_map` 与 `mes_lot_die_bin`;同一 Wafer 支持**多版本**(重测),`is_current=1` 的为当前有效版本;格式适配器可插拔(`map_format`),首期支持厂内自定义 CSV,标准格式(如 SEMI E142)待确认后追加。
- **Die Bin 记录口径**:`mes_lot_die_bin` 记录**当前有效**的 Bin 分布。逐片行(`wafer_id≠0`)与 Lot 汇总行(`wafer_id=0`)**不并存**:存在逐片明细时,汇总值一律由 SUM 计算,不存 `wafer_id=0` 行;无逐片明细(无 Wafer Map)时才允许录入汇总行,之后补导入 Wafer Map 时先迁移为逐片行并写流水。Wafer Map 导入后以其 Bin 结果**覆盖**该片的逐片行并写 `hist_mes_die_bin_log`。
- **Die 数量对账不变式**:Lot 计数单位为 Die 时,有 Bin 记录则 `mes_lot.qty` = 良品 Bin(`is_good=1`)的 `die_qty` 之和。转换站点录入的 `qty_after` 与 Bin 求和不一致,必须在转换历史 `confirm_reason` 登记偏差。**所有 Die 级事务**(Die 报废、按 Bin 拆批、Merge、部分投入封装)必须同时调整 `mes_lot_die_bin` 并写 `hist_mes_die_bin_log`;Wafer 在 Lot 间移动(Split/Merge)时,逐片行随之更换 `lot_id` 并写流水;只有汇总行的 Lot 不允许按 Wafer 拆分(需先导入逐片明细)。
- **按 Bin 拆批**(P2):按 Bin 把 Die 分到不同子 Lot,`hist_mes_lot_relation.bin_code` 记录;各子 Lot 数量之和等于父 Lot 的该 Bin 数。
- **形态转换(Wafer Lot→封装 Lot)**:封装 Lot 由一个或多个 Wafer Lot 投入生成(`hist_mes_lot_relation(PACKAGE_INPUT)`)。投入事务**扣减源 Lot 数量**(部分投入时源 Lot 保留余量);源 Lot 数量归零置 `CONSUMED`。**发生过 PACKAGE_INPUT 的源 Lot**,余量之后因 Die 报废归零时同样置 `CONSUMED`(不置 SCRAPPED),按起始单位计入完成态,工单才能完工。封装 Lot 必须绑定**封装产品的工单**与封装产品版本;"同产品/同 Bin 规则"按封装产品的投料规则校验。Wafer 产品 Route 的末站定义为 `STORE`(Die Bank)站点,表示待投入。`root_wafer_lot_id` 指向主来源,多来源在关系表。
- **Strip/容器**:Strip 属于一个 Lot;切筋成型时 Strip 置 `CONVERTED`,Lot 单位变为 Unit;装盘/装卷按容器记录数量,满后置 `FULL` 并可出货。
- **载具与包装容器的边界**:`mes_carrier`(M4)是可复用的过程载具,类型 FOUP/CASSETTE/FRAME/MAGAZINE;Strip 放入 Magazine 用 `mes_strip.magazine_carrier_id`;**Frame 逐片对应 Wafer**,以 `mes_wafer.frame_carrier_id` 为准(规则见 M4)。出货用 Tray/Reel/Tube/Box 只在 `mes_pack_container`。

## 4. 表设计

### 4.1 新增表(7 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `hist_mes_unit_conversion` | 单位转换历史 | `app-mes-exec` |
| `mes_wafer_map` | Wafer Map | `app-mes-exec` |
| `mes_lot_die_bin` | Lot/Wafer Die Bin 数量 | `app-mes-exec` |
| `hist_mes_die_bin_log` | Die Bin 变动流水 | `app-mes-exec` |
| `mes_strip` | Strip | `app-mes-exec` |
| `mes_pack_container` | 包装容器(Tray/Reel/Tube/Box) | `app-mes-exec` |
| `mes_unit_serial` | Unit 序列号 | `app-mes-exec` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `hist_mes_unit_conversion` 单位转换历史
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `txn_id` | BIGINT |  |  | 事务ID |
| `route_seq_no` | INT |  |  | 转换步骤序号 |
| `step_id` | BIGINT |  |  | 转换站点 |
| `from_unit_id` | BIGINT | 是 |  | 转换前单位 |
| `to_unit_id` | BIGINT | 是 |  | 转换后单位 |
| `qty_before` | BIGINT | 是 |  | 转换前数量 |
| `qty_reference` | BIGINT |  |  | 按系数计算的参考数量 |
| `qty_after` | BIGINT | 是 |  | 转换后实际数量 |
| `loss_qty` | BIGINT |  |  | 损耗数量 |
| `deviation_pct` | DECIMAL(10,4) |  |  | 偏差百分比 |
| `factor_used` | DECIMAL(18,6) |  |  | 使用的换算系数 |
| `scrap_archived_qty` | BIGINT |  |  | 转换前 current_scrap_qty 存档值 |
| `confirm_reason` | VARCHAR(256) |  |  | 偏差确认原因 |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot_time`(tenant_id, lot_id, occurred_at)

#### `mes_wafer_map` Wafer Map
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `wafer_id` | BIGINT | 是 |  | Wafer |
| `map_version` | INT | 是 | 1 | 版本号,重测递增 |
| `is_current` | TINYINT(1) | 是 | 1 | 是否当前有效版本(每片仅一个:导入时持 Wafer 行锁,先清旧标记再置新版本) |
| `lot_id` | BIGINT | 是 |  | Lot |
| `map_format` | VARCHAR(32) | 是 | CSV | 格式 CSV/SEMI_E142/CUSTOM |
| `file_name` | VARCHAR(256) |  |  | 源文件名 |
| `row_count` | INT |  |  | 行数 |
| `col_count` | INT |  |  | 列数 |
| `total_die` | INT |  |  | Die 总数 |
| `good_die` | INT |  |  | 良品 Die 数 |
| `bad_die` | INT |  |  | 不良 Die 数 |
| `map_data` | MEDIUMTEXT |  |  | Map 数据(紧凑文本) |
| `imported_by` | VARCHAR(64) |  |  | 导入人 |
| `imported_at` | DATETIME |  |  | 导入时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_wafer_version`(tenant_id, wafer_id, map_version);索引 `idx_lot`(tenant_id, lot_id);索引 `idx_wafer_current`(tenant_id, wafer_id, is_current)

#### `mes_lot_die_bin` Lot/Wafer Die Bin 数量
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `wafer_id` | BIGINT | 是 | 0 | Wafer;0 表示 Lot 汇总行(与逐片明细二选一) |
| `bin_code` | VARCHAR(32) | 是 |  | Bin 编码 |
| `bin_name` | VARCHAR(64) |  |  | Bin 名称 |
| `is_good` | TINYINT(1) | 是 | 0 | 是否良品 Bin |
| `die_qty` | BIGINT | 是 | 0 | Die 数量 |
| `source` | VARCHAR(16) | 是 | MANUAL | 来源 MANUAL/IMPORT |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_lot_wafer_bin`(tenant_id, lot_id, wafer_id, bin_code)

#### `hist_mes_die_bin_log` Die Bin 变动流水
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `lot_id` | BIGINT | 是 |  | Lot |
| `wafer_id` | BIGINT | 是 | 0 | Wafer,0 表示 Lot 汇总行 |
| `bin_code` | VARCHAR(32) | 是 |  | Bin 编码 |
| `delta` | BIGINT | 是 |  | 变动量(可为负) |
| `qty_after` | BIGINT |  |  | 变动后该 Bin 数量 |
| `reason` | VARCHAR(24) | 是 |  | CONVERT/SCRAP/UNSCRAP/SPLIT/SPLIT_BY_BIN/MERGE/MIGRATE/PACKAGE_INPUT/WAFER_MAP/MANUAL |
| `txn_id` | BIGINT |  |  | 事务ID |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `occurred_at` | DATETIME | 是 |  | 发生时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_lot_time`(tenant_id, lot_id, occurred_at);索引 `idx_txn`(tenant_id, txn_id)

说明:只增表。`mes_lot_die_bin` 的任何增减、覆盖、迁移都必须写本表,保证 Die 数量可对账、可追溯。

#### `mes_strip` Strip
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `strip_code` | VARCHAR(64) | 是 |  | Strip ID |
| `lot_id` | BIGINT | 是 |  | 所属 Lot |
| `status` | VARCHAR(16) | 是 | NORMAL | NORMAL/SCRAPPED/CONVERTED |
| `unit_qty` | INT |  |  | 含 Unit/Die 数 |
| `good_qty` | INT |  |  | 良品数(X-out 后) |
| `magazine_carrier_id` | BIGINT |  |  | 所在 Magazine 载具 |
| `scrap_reason` | VARCHAR(64) |  |  | 报废原因 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_strip_code`(tenant_id, strip_code);索引 `idx_lot`(tenant_id, lot_id)

#### `mes_pack_container` 包装容器(Tray/Reel/Tube/Box)
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `container_no` | VARCHAR(64) | 是 |  | 容器号 |
| `container_type` | VARCHAR(16) | 是 |  | TRAY/REEL/TUBE/BOX |
| `lot_id` | BIGINT |  |  | 所属 Lot |
| `product_id` | BIGINT |  |  | 产品 |
| `qty` | INT | 是 | 0 | 当前装载数量 |
| `capacity` | INT |  |  | 容量 |
| `status` | VARCHAR(16) | 是 | OPEN | OPEN/FULL/SHIPPED/SCRAPPED |
| `packed_at` | DATETIME |  |  | 封装/封口时间 |
| `ship_id` | BIGINT |  |  | 出货记录 |
| `remark` | VARCHAR(256) |  |  | 备注 |
| `version` | INT | 是 | 0 | 乐观锁版本号 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_container_no`(tenant_id, container_no);索引 `idx_lot`(tenant_id, lot_id);索引 `idx_status`(tenant_id, status)

#### `mes_unit_serial` Unit 序列号
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `serial_no` | VARCHAR(64) | 是 |  | 序列号 |
| `lot_id` | BIGINT | 是 |  | Lot |
| `strip_id` | BIGINT |  |  | Strip |
| `container_id` | BIGINT |  |  | 容器 |
| `position` | VARCHAR(32) |  |  | 在 Strip/容器内位置 |
| `status` | VARCHAR(16) | 是 | NORMAL | NORMAL/SCRAPPED/SHIPPED |
| `bin_code` | VARCHAR(32) |  |  | Bin |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_serial`(tenant_id, serial_no);索引 `idx_lot`(tenant_id, lot_id);索引 `idx_container`(tenant_id, container_id)

### 4.2 变更表(4 处)

#### 变更 `mes_lot` Lot

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `root_wafer_lot_id` | BIGINT |  |  | 主来源 Wafer Lot |
| `source_wafer_qty` | BIGINT |  |  | 来源 Wafer 数 |
| `lot_form` | VARCHAR(16) |  |  | 形态 WAFER/DIE/STRIP/UNIT/PACKED |
| `sub_qty` | BIGINT |  |  | 副数量(如主单位为 Strip 时的 Unit 总数) |
| `sub_unit_id` | BIGINT |  |  | 副数量计数单位 |

新增索引:索引 `idx_root_wafer_lot`(tenant_id, root_wafer_lot_id)

#### 变更 `mes_wafer` Wafer

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `frame_carrier_id` | BIGINT |  |  | 所在 Frame 载具(逐片对应) |

#### 变更 `hist_mes_lot_relation` Lot 关系

`relation_type` 增用 PACKAGE_INPUT/SPLIT_BY_BIN。

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `bin_code` | VARCHAR(32) |  |  | 按 Bin 拆批时的 Bin 编码 |

#### 变更 `mes_product` 产品/料号

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `track_serial` | TINYINT(1) | 是 | 0 | 是否管控 Unit 序列号 |

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- POST `/api/v1/mes/lots/{id}/track-out`(转换站点出站时携带 `convert` 载荷,与普通出站同一入口)
- POST `/api/v1/mes/lots/{id}/die-bins`;POST `/api/v1/mes/wafers/{id}/wafer-map/import`
- POST `/api/v1/mes/lots/{id}/split-by-bin`
- POST `/api/v1/mes/lots/package`(一个或多个 Wafer Lot 投入生成封装 Lot,需指定封装工单)
- `/api/v1/mes/strips` CRUD;POST `/api/v1/mes/lots/{id}/strips/generate`
- `/api/v1/mes/pack-containers` CRUD;POST `…/{id}/pack`、`/seal`
- `/api/v1/mes/unit-serials`(可选)

## 6. 验收标准

1. 一个 Wafer Lot 经切割转换为 Die,装片成 Strip,切筋成型成 Unit 并装入 Reel,全程数量与历史连续
2. 任一 Reel 可反查到原 Wafer(反查来料物料批需 M9 完成)
3. 转换偏差超过容差时被阻断或要求录入原因,并记入历史;转换后不能被 Undo
4. Wafer Map 导入后 Bin 分布与 Die 总数、良品数一致;重测生成新版本且当前版本唯一
5. 部分投入封装后,源 Wafer Lot 数量扣减正确,全部投入后状态为 CONSUMED,工单进度不重复统计
6. Die 报废/按 Bin 拆批/合并后,良品 Bin 求和与 Lot 数量一致,`hist_mes_die_bin_log` 可逐笔对账
7. Wafer Map 重导入(重测)后,当前版本唯一,Bin 变动有流水

## 7. 待确认

1. Wafer Map 文件格式:是否有既定标准(SEMI E142/厂内格式)?首期仅支持 CSV 的假设是否可行?
2. `product.track_serial`(Unit 序列号)是否 P2 才做,首期保留字段不落表?
