-- M7 轻量库房 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_7__mes_m7.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- mes_warehouse 库房
CREATE TABLE IF NOT EXISTS `mes_warehouse` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `wh_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '库房编码',
    `wh_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '库房名称',
    `wh_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RAW' COMMENT '类型 RAW/LINESIDE/QUARANTINE/OTHER',
    `area_id` BIGINT NULL COMMENT '所属区域',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_wh_code`(`tenant_id` ASC,`wh_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '库房';

-- mes_location 库位
CREATE TABLE IF NOT EXISTS `mes_location` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `wh_id` BIGINT NOT NULL COMMENT '库房',
    `loc_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '库位编码',
    `loc_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '库位名称',
    `parent_id` BIGINT NULL COMMENT '上级库位',
    `loc_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SHELF' COMMENT '类型 SHELF/CABINET/FLOOR/BIN/EQUIPMENT_SIDE',
    `area_id` BIGINT NULL COMMENT '关联区域(线边库位)',
    `equipment_id` BIGINT NULL COMMENT '关联设备(设备旁库位)',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_wh_loc`(`tenant_id` ASC,`wh_id` ASC,`loc_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '库位';

-- mes_supplier 供应商
CREATE TABLE IF NOT EXISTS `mes_supplier` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `supplier_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商编码',
    `supplier_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '供应商名称',
    `contact` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '联系人',
    `phone` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '电话',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_supplier_code`(`tenant_id` ASC,`supplier_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '供应商';

-- mes_material_lot 物料批
CREATE TABLE IF NOT EXISTS `mes_material_lot` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '物料批号',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `supplier_id` BIGINT NULL COMMENT '供应商',
    `supplier_lot_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '供应商批号',
    `mfg_date` DATE NULL COMMENT '生产日期',
    `expire_date` DATE NULL COMMENT '到期日期',
    `received_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '收货数量',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING_QC' COMMENT 'PENDING_QC/AVAILABLE/IN_USE/QUARANTINE/FROZEN/EXPIRED/CONSUMED',
    `qc_result` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '检验结论 PASS/FAIL/CONCESSION',
    `opened_at` DATETIME NULL COMMENT '开封时间',
    `open_expire_at` DATETIME NULL COMMENT '开封后失效时间(M9)',
    `thaw_start_at` DATETIME NULL COMMENT '开始回温时间(M9)',
    `received_at` DATETIME NULL COMMENT '收货时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_lot_code`(`tenant_id` ASC,`lot_code` ASC) USING BTREE,
    INDEX `idx_material`(`tenant_id` ASC,`material_id` ASC) USING BTREE,
    INDEX `idx_status_expire`(`tenant_id` ASC,`status` ASC,`expire_date` ASC) USING BTREE,
    INDEX `idx_supplier`(`tenant_id` ASC,`supplier_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '物料批';

-- mes_inventory 库存
CREATE TABLE IF NOT EXISTS `mes_inventory` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `material_lot_id` BIGINT NOT NULL COMMENT '物料批',
    `material_id` BIGINT NOT NULL COMMENT '物料(冗余,便于汇总)',
    `location_id` BIGINT NOT NULL COMMENT '库位',
    `qty_on_hand` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '在库数量',
    `qty_frozen` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '冻结数量',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_lot_loc`(`tenant_id` ASC,`material_lot_id` ASC,`location_id` ASC) USING BTREE,
    INDEX `idx_material`(`tenant_id` ASC,`material_id` ASC) USING BTREE,
    INDEX `idx_location`(`tenant_id` ASC,`location_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '库存';

-- hist_mes_inventory_txn 库存流水
CREATE TABLE IF NOT EXISTS `hist_mes_inventory_txn` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `txn_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'RECEIVE/ISSUE/RETURN/TRANSFER/ADJUST/FREEZE/UNFREEZE/CONSUME/SCRAP/COUNT_ADJ',
    `material_lot_id` BIGINT NOT NULL COMMENT '物料批',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `from_location_id` BIGINT NULL COMMENT '源库位',
    `to_location_id` BIGINT NULL COMMENT '目标库位',
    `qty` DECIMAL(18,4) NOT NULL COMMENT '变动数量(正数)',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `balance_after` DECIMAL(18,4) NULL COMMENT '变动后该批该库位在库量',
    `ref_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '关联单据 RECEIPT/ISSUE/WORK_ORDER/LOT/BINDING/STOCKTAKE',
    `ref_id` BIGINT NULL COMMENT '关联单据ID',
    `txn_id` BIGINT NOT NULL COMMENT '事务ID(所有库存变动都经事务层)',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot_time`(`tenant_id` ASC,`material_lot_id` ASC,`occurred_at` ASC) USING BTREE,
    INDEX `idx_material_time`(`tenant_id` ASC,`material_id` ASC,`occurred_at` ASC) USING BTREE,
    INDEX `idx_ref`(`tenant_id` ASC,`ref_type` ASC,`ref_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '库存流水';

-- mes_receipt 收货单
CREATE TABLE IF NOT EXISTS `mes_receipt` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `receipt_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '收货单号',
    `supplier_id` BIGINT NULL COMMENT '供应商',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    `received_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '收货人',
    `received_at` DATETIME NULL COMMENT '收货时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_receipt_no`(`tenant_id` ASC,`receipt_no` ASC) USING BTREE,
    INDEX `idx_supplier`(`tenant_id` ASC,`supplier_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '收货单';

-- mes_receipt_item 收货明细
CREATE TABLE IF NOT EXISTS `mes_receipt_item` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `receipt_id` BIGINT NOT NULL COMMENT '收货单',
    `line_no` INT NOT NULL COMMENT '行号',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `material_lot_id` BIGINT NULL COMMENT '生成/关联的物料批',
    `supplier_lot_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '供应商批号',
    `mfg_date` DATE NULL COMMENT '生产日期',
    `expire_date` DATE NULL COMMENT '到期日期',
    `qty` DECIMAL(18,4) NOT NULL COMMENT '收货数量',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `location_id` BIGINT NOT NULL COMMENT '入库库位',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_receipt_line`(`tenant_id` ASC,`receipt_id` ASC,`line_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '收货明细';

-- mes_incoming_inspection 来料检验
CREATE TABLE IF NOT EXISTS `mes_incoming_inspection` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `material_lot_id` BIGINT NOT NULL COMMENT '物料批',
    `result` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PASS/FAIL/CONCESSION',
    `inspector` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '检验人',
    `inspected_at` DATETIME NOT NULL COMMENT '检验时间',
    `items_json` JSON NULL COMMENT '检验项与结果',
    `remark` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注/特采说明',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_material_lot`(`tenant_id` ASC,`material_lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '来料检验';

-- mes_issue_order 领退料/转移单
CREATE TABLE IF NOT EXISTS `mes_issue_order` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `order_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '单号',
    `order_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ISSUE' COMMENT 'ISSUE/RETURN/TRANSFER',
    `wo_id` BIGINT NULL COMMENT '关联工单',
    `step_id` BIGINT NULL COMMENT '关联站点',
    `from_location_id` BIGINT NULL COMMENT '源库位',
    `to_location_id` BIGINT NULL COMMENT '目标库位',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELLED',
    `requested_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请人',
    `confirmed_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '确认人',
    `confirmed_at` DATETIME NULL COMMENT '确认时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_order_no`(`tenant_id` ASC,`order_no` ASC) USING BTREE,
    INDEX `idx_wo`(`tenant_id` ASC,`wo_id` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '领退料/转移单';

-- mes_issue_item 领退料明细
CREATE TABLE IF NOT EXISTS `mes_issue_item` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `order_id` BIGINT NOT NULL COMMENT '单据',
    `line_no` INT NOT NULL COMMENT '行号',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `material_lot_id` BIGINT NOT NULL COMMENT '物料批',
    `qty` DECIMAL(18,4) NOT NULL COMMENT '数量',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_order_line`(`tenant_id` ASC,`order_id` ASC,`line_no` ASC) USING BTREE,
    INDEX `idx_material_lot`(`tenant_id` ASC,`material_lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '领退料明细';

-- mes_incoming_wafer 来料 Wafer 清单
CREATE TABLE IF NOT EXISTS `mes_incoming_wafer` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `material_lot_id` BIGINT NOT NULL COMMENT '所属来料物料批',
    `customer_id` BIGINT NULL COMMENT '客户',
    `vendor_wafer_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户/来料原始 Wafer 编号',
    `slot_no` INT NULL COMMENT '来料 Slot 号',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE/USED/SCRAPPED',
    `wafer_id` BIGINT NULL COMMENT '开批后对应的 Wafer',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_lot_vendor_wafer`(`tenant_id` ASC,`material_lot_id` ASC,`vendor_wafer_id` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC) USING BTREE,
    INDEX `idx_wafer`(`tenant_id` ASC,`wafer_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '来料 Wafer 清单';
