-- M6 单位转换历史。仅 MySQL 8。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `hist_mes_unit_conversion` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `txn_id` BIGINT NULL COMMENT '事务ID',
    `route_seq_no` INT NULL COMMENT '转换步骤序号',
    `step_id` BIGINT NULL COMMENT '转换站点',
    `from_unit_id` BIGINT NOT NULL COMMENT '转换前单位',
    `to_unit_id` BIGINT NOT NULL COMMENT '转换后单位',
    `qty_before` BIGINT NOT NULL COMMENT '转换前数量',
    `qty_reference` BIGINT NULL COMMENT '按系数计算的参考数量',
    `qty_after` BIGINT NOT NULL COMMENT '转换后实际数量',
    `loss_qty` BIGINT NULL COMMENT '损耗数量',
    `deviation_pct` DECIMAL(10,4) NULL COMMENT '偏差百分比',
    `factor_used` DECIMAL(18,6) NULL COMMENT '使用的换算系数',
    `scrap_archived_qty` BIGINT NULL COMMENT '转换前 current_scrap_qty 存档值',
    `confirm_reason` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '偏差确认原因',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot_time`(`tenant_id` ASC,`lot_id` ASC,`occurred_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '单位转换历史';

CREATE TABLE IF NOT EXISTS `mes_wafer_map` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `wafer_id` BIGINT NOT NULL COMMENT 'Wafer',
    `map_version` INT NOT NULL DEFAULT 1 COMMENT '版本号,重测递增',
    `is_current` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否当前有效版本',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `map_format` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CSV' COMMENT '格式',
    `file_name` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '源文件名',
    `total_die` INT NULL COMMENT 'Die 总数',
    `good_die` INT NULL COMMENT '良品 Die 数',
    `bad_die` INT NULL COMMENT '不良 Die 数',
    `map_data` MEDIUMTEXT NULL COMMENT 'Map 数据',
    `imported_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '导入人',
    `imported_at` DATETIME NULL COMMENT '导入时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_wafer_version`(`tenant_id` ASC,`wafer_id` ASC,`map_version` ASC) USING BTREE,
    INDEX `idx_wafer_current`(`tenant_id` ASC,`wafer_id` ASC,`is_current` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Wafer Map';

CREATE TABLE IF NOT EXISTS `mes_lot_die_bin` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `wafer_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Wafer;0 表示 Lot 汇总行',
    `bin_code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Bin 编码',
    `bin_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Bin 名称',
    `is_good` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否良品 Bin',
    `die_qty` BIGINT NOT NULL DEFAULT 0 COMMENT 'Die 数量',
    `source` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANUAL' COMMENT '来源 MANUAL/IMPORT',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_lot_wafer_bin`(`tenant_id` ASC,`lot_id` ASC,`wafer_id` ASC,`bin_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot/Wafer Die Bin 数量';

CREATE TABLE IF NOT EXISTS `hist_mes_die_bin_log` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `wafer_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Wafer,0 表示 Lot 汇总行',
    `bin_code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Bin 编码',
    `delta` BIGINT NOT NULL COMMENT '变动量',
    `qty_after` BIGINT NULL COMMENT '变动后该 Bin 数量',
    `reason` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '变动原因',
    `txn_id` BIGINT NULL COMMENT '事务ID',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot_time`(`tenant_id` ASC,`lot_id` ASC,`occurred_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Die Bin 变动流水';

CREATE TABLE IF NOT EXISTS `mes_strip` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `strip_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Strip ID',
    `lot_id` BIGINT NOT NULL COMMENT '所属 Lot',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/SCRAPPED/CONVERTED',
    `unit_qty` INT NULL COMMENT '含 Unit/Die 数',
    `good_qty` INT NULL COMMENT '良品数',
    `magazine_carrier_id` BIGINT NULL COMMENT '所在 Magazine 载具',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_strip_code`(`tenant_id` ASC,`strip_code` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Strip';

ALTER TABLE `mes_lot`
    ADD COLUMN `sub_qty` BIGINT NULL COMMENT '副数量，Strip 的 Unit 总数';

CREATE TABLE IF NOT EXISTS `mes_pack_container` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `container_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '容器号',
    `container_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TRAY/REEL/TUBE/BOX',
    `lot_id` BIGINT NULL COMMENT '所属 Lot',
    `qty` INT NOT NULL DEFAULT 0 COMMENT '当前装载数量',
    `capacity` INT NULL COMMENT '容量',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/FULL/SHIPPED/SCRAPPED',
    `packed_at` DATETIME NULL COMMENT '装满时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_container_no`(`tenant_id` ASC,`container_no` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '包装容器';

CREATE TABLE IF NOT EXISTS `mes_unit_serial` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `serial_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '序列号',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `strip_id` BIGINT NULL COMMENT 'Strip',
    `container_id` BIGINT NULL COMMENT '容器',
    `position` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '位置',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/SCRAPPED/SHIPPED',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_serial`(`tenant_id` ASC,`serial_no` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Unit 序列号';

ALTER TABLE `mes_product`
    ADD COLUMN `track_serial` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否管控 Unit 序列号';
