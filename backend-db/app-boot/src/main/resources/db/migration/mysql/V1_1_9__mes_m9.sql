CREATE TABLE IF NOT EXISTS `hist_mes_material_consumption` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NULL COMMENT 'Lot',
    `step_id` BIGINT NULL COMMENT '站点',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `material_lot_id` BIGINT NOT NULL DEFAULT 0 COMMENT '物料批',
    `location_id` BIGINT NOT NULL DEFAULT 0 COMMENT '被扣减的库位',
    `binding_id` BIGINT NOT NULL DEFAULT 0 COMMENT '设备绑定,0 表示直耗线边',
    `qty` DECIMAL(18,4) NOT NULL COMMENT '消耗数量',
    `uom_id` BIGINT NOT NULL DEFAULT 0 COMMENT '计量单位',
    `consume_mode` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消耗方式',
    `source` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BACKFLUSH' COMMENT 'BACKFLUSH',
    `is_shortage` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 表示欠账，未扣库存',
    `adjust_ref_id` BIGINT NULL COMMENT '被清偿的欠账',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC,`occurred_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '物料消耗记录';

CREATE TABLE IF NOT EXISTS `mes_equipment_material_binding` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `material_lot_id` BIGINT NOT NULL COMMENT '物料批',
    `remaining_qty` DECIMAL(18,4) NOT NULL COMMENT '剩余量',
    `uom_id` BIGINT NOT NULL DEFAULT 0 COMMENT '计量单位',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DEPLETED/UNMOUNTED',
    `unmount_disposition` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'RETURN_LINESIDE/SCRAP',
    `unmounted_remaining_qty` DECIMAL(18,4) NULL COMMENT '下料时剩余量',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_equipment_material`(`tenant_id` ASC,`equipment_id` ASC,`material_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '设备物料绑定';

CREATE TABLE IF NOT EXISTS `mes_wo_material_requirement` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `wo_id` BIGINT NOT NULL COMMENT '工单',
    `bom_item_id` BIGINT NOT NULL COMMENT 'BOM 行',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `consume_mode` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消耗方式',
    `required_qty` DECIMAL(18,4) NULL COMMENT '需求数量，无法由计划数量推算时为空',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_wo`(`tenant_id` ASC,`wo_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '工单物料需求';
