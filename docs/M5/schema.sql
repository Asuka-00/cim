-- M5 Batch 组批 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_5__mes_m5.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- mes_batch Batch
CREATE TABLE IF NOT EXISTS `mes_batch` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `batch_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Batch 号',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `equipment_id` BIGINT NULL COMMENT '设备',
    `recipe_version_id` BIGINT NULL COMMENT 'Recipe 版本',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/READY/RUNNING/COMPLETED/CANCELLED',
    `lot_count` INT NOT NULL DEFAULT 0 COMMENT '当前 Lot 数',
    `wafer_count` INT NOT NULL DEFAULT 0 COMMENT '当前 Wafer 总数',
    `total_qty` BIGINT NOT NULL DEFAULT 0 COMMENT '总数量',
    `created_by_user` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `track_in_at` DATETIME NULL COMMENT '进站时间',
    `track_out_at` DATETIME NULL COMMENT '出站时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_batch_no`(`tenant_id` ASC,`batch_no` ASC) USING BTREE,
    INDEX `idx_step_status`(`tenant_id` ASC,`step_id` ASC,`status` ASC) USING BTREE,
    INDEX `idx_equipment`(`tenant_id` ASC,`equipment_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Batch';

-- mes_batch_lot Batch 内 Lot
CREATE TABLE IF NOT EXISTS `mes_batch_lot` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `batch_id` BIGINT NOT NULL COMMENT 'Batch',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `seq` INT NOT NULL DEFAULT 1 COMMENT '组内序号',
    `qty` BIGINT NULL COMMENT '加入时数量',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'IN' COMMENT 'IN/REMOVED',
    `added_at` DATETIME NOT NULL COMMENT '加入时间',
    `removed_at` DATETIME NULL COMMENT '移除时间',
    `remove_reason` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '移除原因',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_batch_lot`(`tenant_id` ASC,`batch_id` ASC,`lot_id` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Batch 内 Lot';

-- 变更 mes_step
ALTER TABLE `mes_step`
    ADD COLUMN `is_batch_step` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否批处理站点',
    ADD COLUMN `batch_min_lots` INT NULL COMMENT '最小批量(Lot 数)',
    ADD COLUMN `batch_max_lots` INT NULL COMMENT '最大批量(Lot 数)',
    ADD COLUMN `batch_max_wafers` INT NULL COMMENT '最大 Wafer 总数,空表示不限';

-- 变更 mes_equipment
ALTER TABLE `mes_equipment`
    ADD COLUMN `batch_max_lots` INT NULL COMMENT '设备级批量上限,覆盖站点设置;空表示取站点设置';

-- 变更 mes_lot
ALTER TABLE `mes_lot`
    ADD COLUMN `batch_id` BIGINT NULL COMMENT '当前所属 Batch',
    ADD INDEX `idx_batch`(`tenant_id` ASC,`batch_id` ASC) USING BTREE;
