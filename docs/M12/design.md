# M12 上线准备与平台增强

> 状态:**设计草案,待核对**(未实现)· 依赖:M3 及之后 · 草案 DDL:[`schema.sql`](./schema.sql)(实现时转为 Flyway `V1_1_12__mes_m12.sql`)

## 1. 目标

具备真实上线所需的外围能力:主数据导入导出、WIP 初始化、标签打印、通知规则、班别日历、交接班、作业指导、ECN、UDA。

## 2. 范围

- Excel 导入导出
- WIP 初始化导入
- 标签/流程卡打印
- 通知规则与站内预警
- 班别/日历
- 交接班
- 站点作业指导
- 变更管理 ECN
- 用户自定义属性 UDA
- 国际化

## 3. 设计规则

- **模块归属**:导入框架、打印模板、通知规则、班次日历、UDA 定义放 `app-mes-core`(被 base/warehouse/exec/maint 共用,避免反向依赖);ECN 与作业指导属于主数据范畴放 `app-mes-base`;交接班依赖在制数据放 `app-mes-exec`。
- **导入任务**:异步任务模型 `mes_import_task`;逐行校验,失败行写 `hist_mes_import_error`,支持"仅校验不入库"与"整批回滚/跳过错误行"。导入类型:`PRODUCT/ROUTE/STEP/EQUIPMENT/MATERIAL/BOM/RECIPE/MATERIAL_LOT/WIP_INIT`。
- **WIP 初始化**:每行描述一个在制 Lot,**必须包含**:Lot 号、产品版本、Route 版本、当前步骤序号、状态、数量与计数单位、Wafer 列表(含 Slot/状态)、位置、**各已过站点的经过次数(`mes_lot_step_pass`)**、返工位置(`rework_id/rework_seq_no`)、`step_enter_at`、`last_track_out_at`、尚未释放的 Hold 清单、指定的 Recipe 覆盖、ACTIVE 的 Time Link 计时起点(M8)。通过统一事务层写 `CREATE`+"迁移初始化"历史,标记 `source=IMPORT`;导入时校验经过次数与当前步骤自洽;仅在系统切换期使用,开关 `mes.wip.import.enabled`。
- **打印**:模板内容为 HTML + 变量占位(`{{lot.lotNo}}`),条码/二维码在前端渲染;`hist_mes_print_log` 记录打印对象与份数,可重打留痕。
- **通知规则**:在 `mes_alert`(M0)基础上,`mes_notify_rule` 定义"事件类型 + 条件 + 接收人范围 + 渠道(站内/SSE)",领域事件订阅者(`ALERT_RULE`)按规则生成预警,经 M0 投递表保证每条事件只生成一次;不接入邮件/企业微信(单系统独立运行)。
- **班别与日历**:`mes_shift` 定义班次时段(支持跨日),`mes_calendar` 定义日期的班次安排与是否工作日;报表按班次汇总依赖此表。
- **交接班**:交班人提交时系统自动抓取快照(在制 Lot 统计、Hold 清单、设备异常、未关闭预警)存入 `content_json`,接班人确认。
- **作业指导**:`mes_step_instruction` 与站点(可限产品)绑定,终端进入 Lot 时展示;内容为富文本/Markdown,附件为 `mes_file.id` 数组。
- **文件存储**:框架现有只有发布包上传与日志文件,没有通用文件服务。M12 在 core 新建 `mes_file`(元数据)与上传/下载接口,文件本体存本地目录 `app.mes.file.root`(多实例需共享存储);作业指导附件、导入源文件、缺陷图片(M13)都引用 `mes_file.id`。
- **ECN**:记录变更申请与关联对象(Route/Recipe/BOM/产品版本/其他),审批沿用 `hist_mes_approval_record`;生效动作仍由各版本自身的状态机完成,ECN 仅作为流程与留痕入口。
- **UDA**:`mes_uda_def` 定义对象扩展字段(类型、必填、选项),值存入对象表的 `ext_json`;不为每个扩展字段建列,查询需求高的字段再单独加列。

## 4. 表设计

### 4.1 新增表(12 张)

| 表 | 说明 | 模块 |
|---|---|---|
| `mes_import_task` | 导入任务 | `app-mes-core` |
| `hist_mes_import_error` | 导入错误明细 | `app-mes-core` |
| `mes_print_template` | 打印模板 | `app-mes-core` |
| `hist_mes_print_log` | 打印记录 | `app-mes-core` |
| `mes_notify_rule` | 通知规则 | `app-mes-core` |
| `mes_shift` | 班次 | `app-mes-core` |
| `mes_calendar` | 工厂日历 | `app-mes-core` |
| `mes_handover` | 交接班 | `app-mes-exec` |
| `mes_step_instruction` | 站点作业指导 | `app-mes-base` |
| `mes_ecn` | 变更申请 ECN | `app-mes-base` |
| `mes_uda_def` | 用户自定义属性定义 | `app-mes-core` |
| `mes_file` | 文件 | `app-mes-core` |

> 通用字段(`id` 雪花ID、`tenant_id`、`create_by/create_at`、`update_by/update_at`)见 [总览约定](../README.md#2-表设计约定);历史/流水表(`hist_*`)只含 `create_*`。

#### `mes_import_task` 导入任务
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `task_no` | VARCHAR(64) | 是 |  | 任务号 |
| `import_type` | VARCHAR(32) | 是 |  | 导入类型 |
| `file_name` | VARCHAR(256) |  |  | 文件名 |
| `mode` | VARCHAR(16) | 是 | COMMIT | 模式 VALIDATE_ONLY/COMMIT |
| `on_error` | VARCHAR(16) | 是 | SKIP | 错误处理 SKIP/ROLLBACK |
| `status` | VARCHAR(16) | 是 | PENDING | PENDING/RUNNING/SUCCESS/PARTIAL/FAILED |
| `total_rows` | INT | 是 | 0 | 总行数 |
| `success_rows` | INT | 是 | 0 | 成功行数 |
| `fail_rows` | INT | 是 | 0 | 失败行数 |
| `operator` | VARCHAR(64) |  |  | 操作人 |
| `started_at` | DATETIME |  |  | 开始时间 |
| `finished_at` | DATETIME |  |  | 结束时间 |
| `file_ref` | VARCHAR(256) |  |  | 源文件 `mes_file.id` |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_task_no`(tenant_id, task_no);索引 `idx_type_status`(tenant_id, import_type, status)

#### `hist_mes_import_error` 导入错误明细
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `task_id` | BIGINT | 是 |  | 任务 |
| `row_no` | INT | 是 |  | 行号 |
| `error_code` | VARCHAR(32) |  |  | 错误码 |
| `message` | VARCHAR(512) | 是 |  | 错误信息 |
| `raw_data` | JSON |  |  | 原始行数据 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_task_row`(tenant_id, task_id, row_no)

#### `mes_print_template` 打印模板
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `tpl_code` | VARCHAR(64) | 是 |  | 模板编码 |
| `tpl_name` | VARCHAR(128) | 是 |  | 模板名称 |
| `tpl_type` | VARCHAR(24) | 是 |  | LOT_TRAVELER/CARRIER_LABEL/WAFER_LABEL/STRIP_LABEL/REEL_LABEL/MATERIAL_LABEL |
| `paper_size` | VARCHAR(32) |  |  | 纸张/标签尺寸 |
| `content` | MEDIUMTEXT | 是 |  | 模板内容(HTML + 变量) |
| `is_default` | TINYINT(1) | 是 | 0 | 该类型默认模板 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_tpl_code`(tenant_id, tpl_code);索引 `idx_type`(tenant_id, tpl_type)

#### `hist_mes_print_log` 打印记录
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `tpl_id` | BIGINT |  |  | 模板 |
| `tpl_type` | VARCHAR(24) |  |  | 模板类型 |
| `biz_type` | VARCHAR(32) | 是 |  | 打印对象类型 |
| `biz_id` | BIGINT | 是 |  | 打印对象ID |
| `biz_code` | VARCHAR(64) |  |  | 对象编码 |
| `copies` | INT | 是 | 1 | 份数 |
| `is_reprint` | TINYINT(1) | 是 | 0 | 是否重打 |
| `printed_by` | VARCHAR(64) |  |  | 打印人 |
| `printed_at` | DATETIME | 是 |  | 打印时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

索引:索引 `idx_biz`(tenant_id, biz_type, biz_id);索引 `idx_time`(tenant_id, printed_at)

#### `mes_notify_rule` 通知规则
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `rule_code` | VARCHAR(64) | 是 |  | 规则编码 |
| `rule_name` | VARCHAR(128) | 是 |  | 规则名称 |
| `event_type` | VARCHAR(64) | 是 |  | 触发的领域事件类型 |
| `condition_json` | JSON |  |  | 条件,如 {"severity":"CRITICAL"} |
| `level` | VARCHAR(8) | 是 | WARN | 预警级别 INFO/WARN/ERROR |
| `receiver_type` | VARCHAR(16) | 是 | ROLE | ROLE/USER/AREA/ALL |
| `receiver_ids` | JSON |  |  | 接收对象标识数组 |
| `title_template` | VARCHAR(128) |  |  | 标题模板 |
| `content_template` | VARCHAR(512) |  |  | 内容模板 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_rule_code`(tenant_id, rule_code);索引 `idx_event`(tenant_id, event_type)

#### `mes_shift` 班次
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `shift_code` | VARCHAR(32) | 是 |  | 班次编码 |
| `shift_name` | VARCHAR(64) | 是 |  | 班次名称 |
| `start_time` | TIME | 是 |  | 开始时间 |
| `end_time` | TIME | 是 |  | 结束时间 |
| `cross_day` | TINYINT(1) | 是 | 0 | 是否跨日 |
| `sort_no` | INT | 是 | 0 | 排序号 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_shift_code`(tenant_id, shift_code)

#### `mes_calendar` 工厂日历
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `cal_date` | DATE | 是 |  | 日期 |
| `is_workday` | TINYINT(1) | 是 | 1 | 是否工作日 |
| `shift_codes` | VARCHAR(128) |  |  | 当日班次编码列表,逗号分隔 |
| `remark` | VARCHAR(128) |  |  | 备注 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_cal_date`(tenant_id, cal_date)

#### `mes_handover` 交接班
所属模块:`app-mes-exec`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `handover_no` | VARCHAR(64) | 是 |  | 交接单号 |
| `shift_code` | VARCHAR(32) |  |  | 班次 |
| `area_id` | BIGINT |  |  | 区域 |
| `from_user` | VARCHAR(64) | 是 |  | 交班人 |
| `to_user` | VARCHAR(64) |  |  | 接班人 |
| `status` | VARCHAR(16) | 是 | DRAFT | DRAFT/SUBMITTED/CONFIRMED |
| `content_json` | JSON |  |  | 交接快照(在制/Hold/异常/预警/备注) |
| `remark` | VARCHAR(1024) |  |  | 交班备注 |
| `submitted_at` | DATETIME |  |  | 提交时间 |
| `confirmed_at` | DATETIME |  |  | 确认时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_handover_no`(tenant_id, handover_no);索引 `idx_status`(tenant_id, status);索引 `idx_from_time`(tenant_id, from_user, submitted_at)

#### `mes_step_instruction` 站点作业指导
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `step_id` | BIGINT | 是 |  | 站点 |
| `product_id` | BIGINT |  |  | 限定产品,空表示通用 |
| `title` | VARCHAR(128) | 是 |  | 标题 |
| `content` | MEDIUMTEXT |  |  | 内容(Markdown/富文本) |
| `attachments` | JSON |  |  | 附件 `mes_file.id` 数组 |
| `version_no` | INT | 是 | 1 | 版本 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_step_product`(tenant_id, step_id, product_id)

#### `mes_ecn` 变更申请 ECN
所属模块:`app-mes-base`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `ecn_no` | VARCHAR(64) | 是 |  | ECN 编号 |
| `title` | VARCHAR(128) | 是 |  | 标题 |
| `change_type` | VARCHAR(16) | 是 |  | ROUTE/RECIPE/BOM/PRODUCT/OTHER |
| `target_type` | VARCHAR(32) |  |  | 关联对象类型 |
| `target_id` | BIGINT |  |  | 关联对象ID(新版本) |
| `reason` | VARCHAR(1024) | 是 |  | 变更原因 |
| `impact` | VARCHAR(1024) |  |  | 影响范围说明 |
| `status` | VARCHAR(16) | 是 | DRAFT | DRAFT/REVIEWING/APPROVED/REJECTED/EFFECTIVE/CANCELLED |
| `applicant` | VARCHAR(64) |  |  | 申请人 |
| `approver` | VARCHAR(64) |  |  | 审批人 |
| `effective_at` | DATETIME |  |  | 生效时间 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_ecn_no`(tenant_id, ecn_no);索引 `idx_status`(tenant_id, status)

#### `mes_uda_def` 用户自定义属性定义
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `object_type` | VARCHAR(32) | 是 |  | 对象类型 LOT/WAFER/PRODUCT/EQUIPMENT/MATERIAL/WORK_ORDER |
| `attr_code` | VARCHAR(64) | 是 |  | 属性编码 |
| `attr_name` | VARCHAR(128) | 是 |  | 属性名称 |
| `data_type` | VARCHAR(16) | 是 | TEXT | TEXT/NUMBER/DATE/BOOL/ENUM |
| `required` | TINYINT(1) | 是 | 0 | 是否必填 |
| `options_json` | JSON |  |  | 枚举选项 |
| `default_value` | VARCHAR(256) |  |  | 默认值 |
| `sort_no` | INT | 是 | 0 | 排序号 |
| `enabled` | TINYINT(1) | 是 | 1 | 启用状态 0-禁用 1-启用 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:唯一 `uk_object_attr`(tenant_id, object_type, attr_code)

#### `mes_file` 文件
所属模块:`app-mes-core`

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT | 是 |  | 主键ID |
| `tenant_id` | VARCHAR(64) | 是 |  | 租户ID |
| `file_name` | VARCHAR(256) | 是 |  | 原始文件名 |
| `content_type` | VARCHAR(128) |  |  | MIME 类型 |
| `size_bytes` | BIGINT | 是 |  | 文件大小(字节) |
| `storage_path` | VARCHAR(512) | 是 |  | 存储相对路径(根目录 app.mes.file.root) |
| `sha256` | VARCHAR(64) | 是 |  | 内容摘要,用于去重与校验 |
| `biz_type` | VARCHAR(32) | 是 | OTHER | INSTRUCTION/IMPORT/WAFER_MAP/DEFECT_IMAGE/OTHER |
| `biz_id` | BIGINT | 是 | 0 | 业务对象ID,0 表示未关联 |
| `create_by` | VARCHAR(64) |  |  | 创建人 |
| `create_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| `update_by` | VARCHAR(64) |  |  | 更新人 |
| `update_at` | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

索引:索引 `idx_biz`(tenant_id, biz_type, biz_id);索引 `idx_sha`(tenant_id, sha256)

说明:框架没有通用文件服务,本表只存元数据,文件本体存本地目录(多实例部署需挂共享存储);被引用的文件不物理删除。

### 4.2 变更表(7 处)

#### 变更 `mes_alert` 站内预警

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `rule_id` | BIGINT |  |  | 来源通知规则 |

#### 变更 `mes_lot` Lot

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `ext_json` | JSON |  |  | 用户自定义属性值 {attrCode: value} |

#### 变更 `mes_wafer` Wafer

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `ext_json` | JSON |  |  | 用户自定义属性值 {attrCode: value} |

#### 变更 `mes_product` 产品/料号

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `ext_json` | JSON |  |  | 用户自定义属性值 {attrCode: value} |

#### 变更 `mes_equipment` 设备台账

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `ext_json` | JSON |  |  | 用户自定义属性值 {attrCode: value} |

#### 变更 `mes_material` 物料主数据

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `ext_json` | JSON |  |  | 用户自定义属性值 {attrCode: value} |

#### 变更 `mes_work_order` 工单

| 新增字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `ext_json` | JSON |  |  | 用户自定义属性值 {attrCode: value} |

## 5. 接口清单(草案)

统一前缀 `/api/v1/mes`,响应沿用 `Result<T>`,分页 `PageDTO`;写接口带 `@AuditLog`。

- `/api/v1/mes/import-tasks` 创建/查询/下载错误明细;GET `/api/v1/mes/import-templates/{type}`
- GET `/api/v1/mes/export/{type}`
- `/api/v1/mes/print-templates` CRUD;POST `/api/v1/mes/print/render`、`/print/log`
- `/api/v1/mes/notify-rules` CRUD
- `/api/v1/mes/shifts`、`/calendar` CRUD
- `/api/v1/mes/handovers` CRUD;POST `…/{id}/submit`、`/confirm`
- `/api/v1/mes/step-instructions` CRUD
- `/api/v1/mes/ecns` CRUD;POST `…/{id}/submit`、`/approve`
- `/api/v1/mes/uda-defs` CRUD

## 6. 验收标准

1. Excel 一次性导入主数据与在制 WIP,错误行明确定位并可下载
2. 流程卡与各类标签可打印、可重打并留痕
3. 符合规则的 Hold 超时/物料低位在站内收到预警
4. 交班快照与当时系统数据一致,接班确认后不可修改
5. 上传的附件可下载,同一事件重复投递不产生重复预警

## 7. 待确认

1. 标签打印是浏览器直接打印,还是需要对接标签打印机(ZPL/TSPL)?当前按浏览器打印设计,打印机直连需另行评估。
2. 导入文件格式限定 xlsx,还是同时支持 csv?
