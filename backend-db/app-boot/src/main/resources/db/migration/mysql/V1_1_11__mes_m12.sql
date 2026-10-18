CREATE TABLE IF NOT EXISTS `mes_file` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `file_name` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始文件名',
    `content_type` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'MIME 类型',
    `size_bytes` BIGINT NOT NULL COMMENT '文件大小(字节)',
    `storage_path` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '存储相对路径',
    `sha256` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容摘要',
    `biz_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'LOT',
    `biz_id` BIGINT NOT NULL COMMENT 'Lot',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_biz`(`tenant_id` ASC,`biz_type` ASC,`biz_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '文件';

CREATE TABLE IF NOT EXISTS `mes_step_instruction` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `title` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
    `content` MEDIUMTEXT NOT NULL COMMENT '正文',
    `file_id` BIGINT NULL COMMENT '附件 mes_file.id',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_step_product`(`tenant_id` ASC,`step_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站点作业指导';

CREATE TABLE IF NOT EXISTS `mes_ecn` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `ecn_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ECN 编号',
    `title` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
    `change_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'ROUTE/RECIPE/BOM/PRODUCT/OTHER',
    `reason` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变更原因',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/REVIEWING/APPROVED',
    `applicant` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请人',
    `approver` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批人',
    `effective_at` DATETIME NULL COMMENT '生效时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_ecn_no`(`tenant_id` ASC,`ecn_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '变更申请';

CREATE TABLE IF NOT EXISTS `mes_handover` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `handover_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交接单号',
    `from_user` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '交班人',
    `to_user` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '接班人',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/CONFIRMED',
    `content_json` JSON NULL COMMENT '交接快照',
    `remark` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '交班备注',
    `submitted_at` DATETIME NULL COMMENT '提交时间',
    `confirmed_at` DATETIME NULL COMMENT '确认时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_handover_no`(`tenant_id` ASC,`handover_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '交接班';

CREATE TABLE IF NOT EXISTS `mes_shift` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `shift_code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班次编码',
    `shift_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '班次名称',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `cross_day` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否跨日',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_shift_code`(`tenant_id` ASC,`shift_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '班次';

CREATE TABLE IF NOT EXISTS `mes_calendar` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `cal_date` DATE NOT NULL COMMENT '日期',
    `is_workday` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否工作日',
    `shift_codes` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '当日班次编码列表,逗号分隔',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_cal_date`(`tenant_id` ASC,`cal_date` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '工厂日历';

CREATE TABLE IF NOT EXISTS `mes_notify_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
    `event_type` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '触发的领域事件类型',
    `title_template` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '标题模板',
    `content_template` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容模板',
    `level` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WARN' COMMENT '预警级别 INFO/WARN/ERROR',
    `receiver_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ROLE' COMMENT 'ROLE/USER/AREA/ALL',
    `receiver_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '接收对象。ALL 时为空',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_code`(`tenant_id` ASC,`rule_code` ASC) USING BTREE,
    INDEX `idx_event`(`tenant_id` ASC,`event_type` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '通知规则';

ALTER TABLE `mes_alert`
    ADD COLUMN `rule_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '来源通知规则编码',
    ADD UNIQUE INDEX `uk_alert_event_rule`(`tenant_id` ASC, `event_id` ASC, `rule_code` ASC) USING BTREE;

CREATE TABLE IF NOT EXISTS `mes_uda_def` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `object_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对象类型',
    `attr_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '属性编码',
    `attr_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '属性名称',
    `data_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/NUMBER/DATE/BOOL/ENUM',
    `options_json` JSON NULL COMMENT '枚举选项',
    `required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必填',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_object_attr`(`tenant_id` ASC,`object_type` ASC,`attr_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '用户自定义属性定义';

ALTER TABLE `mes_lot`
    ADD COLUMN `ext_json` JSON NULL COMMENT '用户自定义属性值',
    ADD COLUMN `source` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'IMPORT 表示期初导入';

CREATE TABLE IF NOT EXISTS `mes_print_template` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `tpl_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板编码',
    `tpl_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板名称',
    `tpl_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板类型',
    `content` MEDIUMTEXT NOT NULL COMMENT '模板内容',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '该类型默认模板',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_tpl_code`(`tenant_id` ASC,`tpl_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '打印模板';

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
    `printed_at` DATETIME NOT NULL COMMENT '打印时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '打印记录';

CREATE TABLE IF NOT EXISTS `mes_import_task` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `task_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '任务号',
    `import_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '导入类型',
    `mode` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'COMMIT' COMMENT 'VALIDATE_ONLY/COMMIT',
    `on_error` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SKIP' COMMENT 'SKIP/ROLLBACK',
    `file_ref` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文件引用',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/RUNNING/SUCCESS/PARTIAL/FAILED/CANCELLED',
    `started_at` DATETIME NULL COMMENT '开始时间',
    `finished_at` DATETIME NULL COMMENT '结束时间',
    `total_rows` INT NOT NULL DEFAULT 0 COMMENT '总行数',
    `success_rows` INT NOT NULL DEFAULT 0 COMMENT '成功行数',
    `fail_rows` INT NOT NULL DEFAULT 0 COMMENT '失败行数',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_task_no`(`tenant_id` ASC,`task_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '导入任务';

CREATE TABLE IF NOT EXISTS `hist_mes_import_error` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `task_id` BIGINT NOT NULL COMMENT '任务',
    `row_no` INT NOT NULL COMMENT '行号',
    `error_code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误码',
    `raw_data` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原始行内容',
    `message` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '错误信息',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_task_row`(`tenant_id` ASC, `task_id` ASC, `row_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '导入错误明细';
