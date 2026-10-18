-- M5 组批。仅 MySQL 8。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `mes_batch` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `batch_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Batch 号',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `equipment_id` BIGINT NULL COMMENT '进站设备',
    `recipe_version_id` BIGINT NULL COMMENT '本批 Recipe 版本，由第一个带版本的 Lot 定下',
    `track_in_at` DATETIME NULL COMMENT '进站时间',
    `track_out_at` DATETIME NULL COMMENT '出站时间',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/READY/RUNNING/COMPLETED/CANCELLED',
    `min_lots` INT NOT NULL DEFAULT 1 COMMENT '本批最少 Lot 数',
    `max_lots` INT NOT NULL DEFAULT 1 COMMENT '本批最多 Lot 数',
    `lot_count` INT NOT NULL DEFAULT 0 COMMENT '当前 Lot 数',
    `wafer_count` INT NOT NULL DEFAULT 0 COMMENT '当前 Wafer 总数',
    `total_qty` BIGINT NOT NULL DEFAULT 0 COMMENT '总数量',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_batch_no`(`tenant_id` ASC,`batch_no` ASC) USING BTREE,
    INDEX `idx_step_status`(`tenant_id` ASC,`step_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Batch';

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
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Batch 内 Lot';

ALTER TABLE `mes_lot`
    ADD COLUMN `batch_id` BIGINT NULL COMMENT '当前 Batch,空表示不在批内';
