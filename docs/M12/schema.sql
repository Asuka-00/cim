-- M12 上线准备与平台增强 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_12__mes_m12.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- mes_import_task 导入任务
CREATE TABLE IF NOT EXISTS `mes_import_task` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `task_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务号',
    `import_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '导入类型',
    `file_name` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文件名',
    `mode` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COMMIT' COMMENT '模式 VALIDATE_ONLY/COMMIT',
    `on_error` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SKIP' COMMENT '错误处理 SKIP/ROLLBACK',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/PARTIAL/FAILED',
    `total_rows` INT NOT NULL DEFAULT 0 COMMENT '总行数',
    `success_rows` INT NOT NULL DEFAULT 0 COMMENT '成功行数',
    `fail_rows` INT NOT NULL DEFAULT 0 COMMENT '失败行数',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `started_at` DATETIME NULL COMMENT '开始时间',
    `finished_at` DATETIME NULL COMMENT '结束时间',
    `file_ref` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '源文件 mes_file.id',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_task_no`(`tenant_id` ASC,`task_no` ASC) USING BTREE,
    INDEX `idx_type_status`(`tenant_id` ASC,`import_type` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '导入任务';

-- hist_mes_import_error 导入错误明细
CREATE TABLE IF NOT EXISTS `hist_mes_import_error` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `task_id` BIGINT NOT NULL COMMENT '任务',
    `row_no` INT NOT NULL COMMENT '行号',
    `error_code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误码',
    `message` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '错误信息',
    `raw_data` JSON NULL COMMENT '原始行数据',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_task_row`(`tenant_id` ASC,`task_id` ASC,`row_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '导入错误明细';

-- mes_print_template 打印模板
CREATE TABLE IF NOT EXISTS `mes_print_template` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `tpl_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
    `tpl_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
    `tpl_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'LOT_TRAVELER/CARRIER_LABEL/WAFER_LABEL/STRIP_LABEL/REEL_LABEL/MATERIAL_LABEL',
    `paper_size` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '纸张/标签尺寸',
    `content` MEDIUMTEXT NOT NULL COMMENT '模板内容(HTML + 变量)',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '该类型默认模板',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_tpl_code`(`tenant_id` ASC,`tpl_code` ASC) USING BTREE,
    INDEX `idx_type`(`tenant_id` ASC,`tpl_type` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '打印模板';

-- hist_mes_print_log 打印记录
CREATE TABLE IF NOT EXISTS `hist_mes_print_log` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `tpl_id` BIGINT NULL COMMENT '模板',
    `tpl_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '模板类型',
    `biz_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '打印对象类型',
    `biz_id` BIGINT NOT NULL COMMENT '打印对象ID',
    `biz_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '对象编码',
    `copies` INT NOT NULL DEFAULT 1 COMMENT '份数',
    `is_reprint` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否重打',
    `printed_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '打印人',
    `printed_at` DATETIME NOT NULL COMMENT '打印时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_biz`(`tenant_id` ASC,`biz_type` ASC,`biz_id` ASC) USING BTREE,
    INDEX `idx_time`(`tenant_id` ASC,`printed_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '打印记录';

-- mes_notify_rule 通知规则
CREATE TABLE IF NOT EXISTS `mes_notify_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
    `event_type` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发的领域事件类型',
    `condition_json` JSON NULL COMMENT '条件,如 {"severity":"CRITICAL"}',
    `level` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WARN' COMMENT '预警级别 INFO/WARN/ERROR',
    `receiver_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ROLE' COMMENT 'ROLE/USER/AREA/ALL',
    `receiver_ids` JSON NULL COMMENT '接收对象标识数组',
    `title_template` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '标题模板',
    `content_template` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容模板',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_code`(`tenant_id` ASC,`rule_code` ASC) USING BTREE,
    INDEX `idx_event`(`tenant_id` ASC,`event_type` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '通知规则';

-- mes_shift 班次
CREATE TABLE IF NOT EXISTS `mes_shift` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `shift_code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班次编码',
    `shift_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班次名称',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `cross_day` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否跨日',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_shift_code`(`tenant_id` ASC,`shift_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '班次';

-- mes_calendar 工厂日历
CREATE TABLE IF NOT EXISTS `mes_calendar` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `cal_date` DATE NOT NULL COMMENT '日期',
    `is_workday` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否工作日',
    `shift_codes` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '当日班次编码列表,逗号分隔',
    `remark` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_cal_date`(`tenant_id` ASC,`cal_date` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '工厂日历';

-- mes_handover 交接班
CREATE TABLE IF NOT EXISTS `mes_handover` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `handover_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交接单号',
    `shift_code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '班次',
    `area_id` BIGINT NULL COMMENT '区域',
    `from_user` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交班人',
    `to_user` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '接班人',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/CONFIRMED',
    `content_json` JSON NULL COMMENT '交接快照(在制/Hold/异常/预警/备注)',
    `remark` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '交班备注',
    `submitted_at` DATETIME NULL COMMENT '提交时间',
    `confirmed_at` DATETIME NULL COMMENT '确认时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_handover_no`(`tenant_id` ASC,`handover_no` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC) USING BTREE,
    INDEX `idx_from_time`(`tenant_id` ASC,`from_user` ASC,`submitted_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '交接班';

-- mes_step_instruction 站点作业指导
CREATE TABLE IF NOT EXISTS `mes_step_instruction` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `product_id` BIGINT NULL COMMENT '限定产品,空表示通用',
    `title` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
    `content` MEDIUMTEXT NULL COMMENT '内容(Markdown/富文本)',
    `attachments` JSON NULL COMMENT '附件 mes_file.id 数组',
    `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_step_product`(`tenant_id` ASC,`step_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站点作业指导';

-- mes_ecn 变更申请 ECN
CREATE TABLE IF NOT EXISTS `mes_ecn` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `ecn_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ECN 编号',
    `title` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
    `change_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ROUTE/RECIPE/BOM/PRODUCT/OTHER',
    `target_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '关联对象类型',
    `target_id` BIGINT NULL COMMENT '关联对象ID(新版本)',
    `reason` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变更原因',
    `impact` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '影响范围说明',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/REVIEWING/APPROVED/REJECTED/EFFECTIVE/CANCELLED',
    `applicant` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请人',
    `approver` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批人',
    `effective_at` DATETIME NULL COMMENT '生效时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_ecn_no`(`tenant_id` ASC,`ecn_no` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '变更申请 ECN';

-- mes_uda_def 用户自定义属性定义
CREATE TABLE IF NOT EXISTS `mes_uda_def` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `object_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对象类型 LOT/WAFER/PRODUCT/EQUIPMENT/MATERIAL/WORK_ORDER',
    `attr_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '属性编码',
    `attr_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '属性名称',
    `data_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/NUMBER/DATE/BOOL/ENUM',
    `required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填',
    `options_json` JSON NULL COMMENT '枚举选项',
    `default_value` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '默认值',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序号',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_object_attr`(`tenant_id` ASC,`object_type` ASC,`attr_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '用户自定义属性定义';

-- mes_file 文件
CREATE TABLE IF NOT EXISTS `mes_file` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `file_name` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
    `content_type` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'MIME 类型',
    `size_bytes` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `storage_path` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储相对路径(根目录 app.mes.file.root)',
    `sha256` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容摘要,用于去重与校验',
    `biz_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER' COMMENT 'INSTRUCTION/IMPORT/WAFER_MAP/DEFECT_IMAGE/OTHER',
    `biz_id` BIGINT NOT NULL DEFAULT 0 COMMENT '业务对象ID,0 表示未关联',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_biz`(`tenant_id` ASC,`biz_type` ASC,`biz_id` ASC) USING BTREE,
    INDEX `idx_sha`(`tenant_id` ASC,`sha256` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '文件';

-- 变更 mes_alert
ALTER TABLE `mes_alert`
    ADD COLUMN `rule_id` BIGINT NULL COMMENT '来源通知规则';

-- 变更 mes_lot
ALTER TABLE `mes_lot`
    ADD COLUMN `ext_json` JSON NULL COMMENT '用户自定义属性值 {attrCode: value}';

-- 变更 mes_wafer
ALTER TABLE `mes_wafer`
    ADD COLUMN `ext_json` JSON NULL COMMENT '用户自定义属性值 {attrCode: value}';

-- 变更 mes_product
ALTER TABLE `mes_product`
    ADD COLUMN `ext_json` JSON NULL COMMENT '用户自定义属性值 {attrCode: value}';

-- 变更 mes_equipment
ALTER TABLE `mes_equipment`
    ADD COLUMN `ext_json` JSON NULL COMMENT '用户自定义属性值 {attrCode: value}';

-- 变更 mes_material
ALTER TABLE `mes_material`
    ADD COLUMN `ext_json` JSON NULL COMMENT '用户自定义属性值 {attrCode: value}';

-- 变更 mes_work_order
ALTER TABLE `mes_work_order`
    ADD COLUMN `ext_json` JSON NULL COMMENT '用户自定义属性值 {attrCode: value}';
