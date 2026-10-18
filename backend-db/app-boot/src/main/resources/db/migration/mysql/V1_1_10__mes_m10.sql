CREATE TABLE IF NOT EXISTS `mes_sampling_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
    `sample_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EVERY_N_LOT' COMMENT 'EVERY_N_LOT/PERCENT/ALL',
    `n_value` INT NULL COMMENT '每 N 个 Lot 抽 1 个',
    `percent` DECIMAL(6,2) NULL COMMENT '抽检百分比',
    `wafer_count` INT NULL COMMENT '每 Lot 抽取片数,空表示全部',
    `slot_list` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '指定槽位,逗号分隔',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_code`(`tenant_id` ASC,`rule_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '抽检规则';

CREATE TABLE IF NOT EXISTS `mes_step_sampling` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `step_id` BIGINT NOT NULL COMMENT '量测站点',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `rule_id` BIGINT NOT NULL COMMENT '抽检规则',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_step_product`(`tenant_id` ASC,`step_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站点抽检绑定';

CREATE TABLE IF NOT EXISTS `mes_route_step_sampling` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `route_step_id` BIGINT NOT NULL COMMENT 'Route 步骤',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `rule_id` BIGINT NOT NULL COMMENT '抽检规则',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_route_step_product`(`tenant_id` ASC,`route_step_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 步骤抽检覆盖';

CREATE TABLE IF NOT EXISTS `mes_sampling_counter` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `route_step_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Route 步骤,0 表示不区分步骤',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '产品,0 表示通用',
    `counter` INT NOT NULL DEFAULT 0 COMMENT '当前计数',
    `last_sampled_lot_id` BIGINT NULL COMMENT '最近被抽中的 Lot',
    `last_sampled_at` DATETIME NULL COMMENT '最近抽中时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_step_product`(`tenant_id` ASC,`rule_id` ASC,`step_id` ASC,`route_step_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '抽检计数器';

CREATE TABLE IF NOT EXISTS `mes_lot_sampling_plan` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `route_seq_no` INT NOT NULL COMMENT '量测步骤序号',
    `pass_no` INT NOT NULL DEFAULT 1 COMMENT '第 N 次经过',
    `step_id` BIGINT NOT NULL COMMENT '量测站点',
    `rule_id` BIGINT NULL COMMENT '依据的规则',
    `need_measure` TINYINT(1) NOT NULL COMMENT '本 Lot 是否需要量测',
    `wafer_count` INT NULL COMMENT '抽中时要抽的片数,空表示全部或未抽中',
    `wafer_slots` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '抽中的槽位,逗号分隔',
    `decided_at` DATETIME NOT NULL COMMENT '决定时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_lot_seq_pass`(`tenant_id` ASC,`lot_id` ASC,`route_seq_no` ASC,`pass_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot 抽检决定';

ALTER TABLE `mes_route_step`
    ADD COLUMN `is_sampling_step` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '抽检站点:未被抽中的 Lot 自动跳过';

CREATE TABLE IF NOT EXISTS `hist_mes_collect_result` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `record_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ORIGINAL' COMMENT 'ORIGINAL/CORRECTION/VOID',
    `ref_id` BIGINT NULL COMMENT '被作废的原记录',
    `product_id` BIGINT NULL COMMENT '产品',
    `route_seq_no` INT NULL COMMENT '步骤序号',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `pass_no` INT NULL COMMENT '第 N 次经过',
    `item_id` BIGINT NOT NULL COMMENT '采集项',
    `value_num` DECIMAL(24,8) NULL COMMENT '数值',
    `judge_result` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NA' COMMENT 'OK/OOS_LOW/OOS_HIGH/NA',
    `lsl` DECIMAL(24,8) NULL COMMENT '判定时规格下限',
    `usl` DECIMAL(24,8) NULL COMMENT '判定时规格上限',
    `collected_at` DATETIME NOT NULL COMMENT '录入时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot_step`(`tenant_id` ASC,`lot_id` ASC,`step_id` ASC,`collected_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '采集结果';

CREATE TABLE IF NOT EXISTS `mes_step_collect_item` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `item_id` BIGINT NOT NULL COMMENT '采集项',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `lsl` DECIMAL(24,8) NULL COMMENT '规格下限',
    `usl` DECIMAL(24,8) NULL COMMENT '规格上限',
    `auto_hold_on_oos` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'OOS 自动 Hold',
    `hold_reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '自动 Hold 原因码',
    `is_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必采',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_step_item_product`(`tenant_id` ASC,`step_id` ASC,`item_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站点采集规格';

CREATE TABLE IF NOT EXISTS `mes_route_step_collect_item` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `route_step_id` BIGINT NOT NULL COMMENT 'Route 步骤',
    `item_id` BIGINT NOT NULL COMMENT '采集项',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `lsl` DECIMAL(24,8) NULL COMMENT '规格下限',
    `usl` DECIMAL(24,8) NULL COMMENT '规格上限',
    `auto_hold_on_oos` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'OOS 自动 Hold',
    `hold_reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '自动 Hold 原因码',
    `is_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必采',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_route_step_item_product`(`tenant_id` ASC,`route_step_id` ASC,`item_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 步骤采集规格覆盖';
