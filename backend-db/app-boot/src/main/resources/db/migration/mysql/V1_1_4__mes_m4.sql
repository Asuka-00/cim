-- M4 拆批。仅 MySQL 8。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `hist_mes_lot_relation` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `parent_lot_id` BIGINT NOT NULL COMMENT '父 Lot / 源 Lot',
    `child_lot_id` BIGINT NOT NULL COMMENT '子 Lot / 目标 Lot',
    `relation_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SPLIT/MERGE/BONUS/REWORK_SPLIT/PACKAGE_INPUT/SPLIT_BY_BIN',
    `qty` BIGINT NULL COMMENT '涉及数量',
    `unit_id` BIGINT NULL COMMENT '计数单位',
    `txn_id` BIGINT NULL COMMENT '事务ID',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_parent`(`tenant_id` ASC,`parent_lot_id` ASC) USING BTREE,
    INDEX `idx_child`(`tenant_id` ASC,`child_lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot 关系';

ALTER TABLE `mes_lot`
    ADD COLUMN `parent_lot_id` BIGINT NULL COMMENT '父 Lot(Split 产生的子 Lot)',
    ADD COLUMN `root_lot_id` BIGINT NULL COMMENT '根 Lot',
    ADD COLUMN `split_seq` INT NULL COMMENT '拆批序号',
    ADD COLUMN `rework_id` BIGINT NULL COMMENT '所在返工分支,空表示主流程',
    ADD COLUMN `rework_seq_no` INT NULL COMMENT '返工分支内步骤序号',
    ADD INDEX `idx_parent`(`tenant_id` ASC,`parent_lot_id` ASC) USING BTREE,
    ADD INDEX `idx_root`(`tenant_id` ASC,`root_lot_id` ASC) USING BTREE;

CREATE TABLE IF NOT EXISTS `mes_lot_shipment` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `ship_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '出货单号',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `wo_id` BIGINT NULL COMMENT '工单',
    `customer_id` BIGINT NULL COMMENT '客户',
    `qty` BIGINT NOT NULL COMMENT '出货数量',
    `unit_id` BIGINT NOT NULL COMMENT '出货计数单位',
    `shipped_at` DATETIME NOT NULL COMMENT '出货时间',
    `shipped_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '出货人',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_ship_no`(`tenant_id` ASC,`ship_no` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    INDEX `idx_wo`(`tenant_id` ASC,`wo_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot 出货记录';

CREATE TABLE IF NOT EXISTS `mes_route_rework` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `rework_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '返工分支编码',
    `rework_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '返工分支名称',
    `entry_from_seq` INT NOT NULL COMMENT '允许进入的主流程步骤范围起',
    `entry_to_seq` INT NOT NULL COMMENT '允许进入的主流程步骤范围止',
    `return_seq_no` INT NOT NULL COMMENT '返工完成后回到的主流程步骤序号',
    `max_count` INT NOT NULL DEFAULT 1 COMMENT '同一根 Lot 在该分支的最大返工次数',
    `reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '默认返工原因码',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rework_code`(`tenant_id` ASC,`route_version_id` ASC,`rework_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 返工分支';

CREATE TABLE IF NOT EXISTS `mes_route_rework_step` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rework_id` BIGINT NOT NULL COMMENT '返工分支',
    `seq_no` INT NOT NULL COMMENT '返工内序号',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rework_seq`(`tenant_id` ASC,`rework_id` ASC,`seq_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 返工步骤';

CREATE TABLE IF NOT EXISTS `mes_future_action` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `route_version_id` BIGINT NOT NULL COMMENT '预约时的 Route 版本',
    `route_seq_no` INT NOT NULL COMMENT '触发的主流程步骤序号',
    `pass_no` INT NULL COMMENT '指定第几次经过触发,空表示该序号每次到达',
    `timing` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ON_ARRIVE' COMMENT 'ON_ARRIVE/AFTER_TRACK_OUT',
    `action_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HOLD/SPLIT/CHANGE_RECIPE/CHANGE_PRIORITY/NOTIFY',
    `action_params` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '动作参数',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED/CANCELLED',
    `executed_at` DATETIME NULL COMMENT '执行时间',
    `result_message` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '执行结果',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_lot_status`(`tenant_id` ASC,`lot_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '预约动作';

CREATE TABLE IF NOT EXISTS `mes_carrier` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `carrier_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '载具编码',
    `carrier_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FOUP' COMMENT 'FOUP/CASSETTE/MAGAZINE/OTHER',
    `capacity` INT NOT NULL DEFAULT 25 COMMENT '容量',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EMPTY' COMMENT 'EMPTY/OCCUPIED/CLEANING/MAINTENANCE/SCRAPPED',
    `current_lot_id` BIGINT NULL COMMENT '当前绑定 Lot',
    `use_count` INT NOT NULL DEFAULT 0 COMMENT '累计使用次数',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_carrier_code`(`tenant_id` ASC,`carrier_code` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`current_lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '载具';

CREATE TABLE IF NOT EXISTS `hist_mes_carrier_binding` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `carrier_id` BIGINT NOT NULL COMMENT '载具',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `bound_at` DATETIME NOT NULL COMMENT '绑定时间',
    `bound_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '绑定人',
    `unbound_at` DATETIME NULL COMMENT '解绑时间',
    `unbound_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '解绑人',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_carrier_time`(`tenant_id` ASC,`carrier_id` ASC,`bound_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '载具绑定历史';

CREATE TABLE IF NOT EXISTS `mes_dispatch_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
    `scope_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/STEP/AREA',
    `scope_id` BIGINT NOT NULL DEFAULT 0 COMMENT '范围对象,GLOBAL 时为 0',
    `sort_keys` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '排序键,如 IS_HOT:DESC,PRIORITY:ASC,STEP_ENTER_AT:ASC',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '默认规则',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_code`(`tenant_id` ASC,`rule_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '作业清单排序规则';

CREATE TABLE IF NOT EXISTS `mes_lot_note` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `note_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NOTE' COMMENT 'NOTE/WARNING/ENGINEERING/TAG',
    `content` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
    `pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 表示置顶',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot 备注';

CREATE TABLE IF NOT EXISTS `hist_mes_lot_route_change` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `from_product_version_id` BIGINT NULL COMMENT '原产品版本',
    `to_product_version_id` BIGINT NULL COMMENT '新产品版本',
    `from_route_version_id` BIGINT NULL COMMENT '原 Route 版本',
    `to_route_version_id` BIGINT NULL COMMENT '新 Route 版本',
    `from_seq_no` INT NULL COMMENT '变更前步骤序号',
    `to_seq_no` INT NULL COMMENT '变更后步骤序号',
    `reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原因码',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC,`occurred_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot 工艺变更记录';
