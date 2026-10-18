-- M9 物料执行与自动反冲 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_9__mes_m9.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- mes_equipment_slot 设备物料位
CREATE TABLE IF NOT EXISTS `mes_equipment_slot` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `port_id` BIGINT NULL COMMENT 'Port/Chamber',
    `slot_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '槽位编码',
    `slot_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '槽位名称',
    `allowed_material_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '允许的物料类型,空表示不限',
    `allowed_material_id` BIGINT NULL COMMENT '限定的物料,空表示不限',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_eqp_slot`(`tenant_id` ASC,`equipment_id` ASC,`slot_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '设备物料位';

-- mes_equipment_material_binding 设备物料绑定
CREATE TABLE IF NOT EXISTS `mes_equipment_material_binding` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `slot_id` BIGINT NOT NULL COMMENT '槽位',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `material_lot_id` BIGINT NOT NULL COMMENT '物料批',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `initial_qty` DECIMAL(18,4) NOT NULL COMMENT '装载量',
    `remaining_qty` DECIMAL(18,4) NOT NULL COMMENT '剩余量',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `low_threshold` DECIMAL(18,4) NULL COMMENT '低位预警阈值',
    `life_limit_minutes` INT NULL COMMENT '寿命上限(分钟),空表示不按时间',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DEPLETED/UNMOUNTED',
    `unmount_disposition` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '下料剩余量处置 RETURN_LINESIDE/SCRAP',
    `unmounted_remaining_qty` DECIMAL(18,4) NULL COMMENT '下料时剩余量',
    `mounted_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '上料人',
    `mounted_at` DATETIME NOT NULL COMMENT '上料时间',
    `unmounted_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '下料人',
    `unmounted_at` DATETIME NULL COMMENT '下料时间',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_slot_status`(`tenant_id` ASC,`slot_id` ASC,`status` ASC) USING BTREE,
    INDEX `idx_equipment_material`(`tenant_id` ASC,`equipment_id` ASC,`material_id` ASC,`status` ASC) USING BTREE,
    INDEX `idx_material_lot`(`tenant_id` ASC,`material_lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '设备物料绑定';

-- hist_mes_material_consumption 物料消耗记录
CREATE TABLE IF NOT EXISTS `hist_mes_material_consumption` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NULL COMMENT 'Lot',
    `wafer_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'Wafer,0 表示 Lot 级消耗',
    `route_seq_no` INT NULL COMMENT '步骤序号',
    `step_id` BIGINT NULL COMMENT '站点',
    `pass_no` INT NULL COMMENT '第 N 次经过',
    `equipment_id` BIGINT NULL COMMENT '设备',
    `bom_item_id` BIGINT NOT NULL DEFAULT 0 COMMENT '来源 BOM 行,0 表示无',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `material_lot_id` BIGINT NOT NULL DEFAULT 0 COMMENT '物料批,0 表示无',
    `binding_id` BIGINT NOT NULL DEFAULT 0 COMMENT '设备端绑定,0 表示直耗线边',
    `location_id` BIGINT NOT NULL DEFAULT 0 COMMENT '直耗线边时被扣减的库位,设备端绑定为 0',
    `qty` DECIMAL(18,4) NOT NULL COMMENT '消耗数量;修正为差额,Undo/清偿冲销为负数',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `consume_mode` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '消耗方式',
    `source` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BACKFLUSH' COMMENT 'BACKFLUSH/OPEN_LOT/MANUAL/ADJUST/UNDO/SETTLE',
    `is_substitute` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用替代料',
    `is_shortage` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '欠账:库存/剩余量不足时仍放行的消耗',
    `adjust_ref_id` BIGINT NULL COMMENT '被修正/被清偿的原消耗记录',
    `txn_id` BIGINT NOT NULL COMMENT '事务ID(手工、修正、Undo 冲销都有自己的事务,不用 0 哨兵)',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX `uk_backflush`(`tenant_id` ASC,`txn_id` ASC,`bom_item_id` ASC,`wafer_id` ASC,`material_lot_id` ASC,`binding_id` ASC,`location_id` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC,`occurred_at` ASC) USING BTREE,
    INDEX `idx_wafer`(`tenant_id` ASC,`wafer_id` ASC) USING BTREE,
    INDEX `idx_material_lot`(`tenant_id` ASC,`material_lot_id` ASC) USING BTREE,
    INDEX `idx_binding`(`tenant_id` ASC,`binding_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '物料消耗记录';

-- mes_consumption_adjust 消耗修正申请
CREATE TABLE IF NOT EXISTS `mes_consumption_adjust` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `adjust_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '修正单号',
    `consumption_id` BIGINT NOT NULL COMMENT '原消耗记录',
    `qty_before` DECIMAL(18,4) NOT NULL COMMENT '修正前数量',
    `qty_after` DECIMAL(18,4) NOT NULL COMMENT '修正后数量',
    `reason` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '修正原因',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `applicant` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '申请人',
    `approver` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批人',
    `approved_at` DATETIME NULL COMMENT '审批时间',
    `comment` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批意见',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_adjust_no`(`tenant_id` ASC,`adjust_no` ASC) USING BTREE,
    INDEX `idx_consumption`(`tenant_id` ASC,`consumption_id` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '消耗修正申请';

-- mes_wo_material_requirement 工单物料需求
CREATE TABLE IF NOT EXISTS `mes_wo_material_requirement` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `wo_id` BIGINT NOT NULL COMMENT '工单',
    `bom_item_id` BIGINT NOT NULL DEFAULT 0 COMMENT 'BOM 行,0 表示无',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `step_id` BIGINT NULL COMMENT '消耗站点',
    `required_qty` DECIMAL(18,4) NOT NULL COMMENT '需求数量',
    `issued_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '已领到线边数量',
    `consumed_qty` DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '已消耗数量(由 MATERIAL_CONSUMED 事件异步汇总,不在 Track-Out 事务内更新,避免同工单热点)',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `kitting_status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'UNCHECKED' COMMENT 'UNCHECKED/OK/SHORT',
    `short_qty` DECIMAL(18,4) NULL COMMENT '缺料数量',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_wo_bom`(`tenant_id` ASC,`wo_id` ASC,`bom_item_id` ASC) USING BTREE,
    INDEX `idx_material`(`tenant_id` ASC,`material_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '工单物料需求';

-- 变更 mes_material
ALTER TABLE `mes_material`
    ADD COLUMN `open_life_hours` INT NULL COMMENT '开封后有效期(小时),空表示无',
    ADD COLUMN `thaw_minutes` INT NULL COMMENT '回温最短时间(分钟),空表示无';
