-- M11 追溯与报表 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_11__mes_m11.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- mes_report_daily 生产日报汇总
CREATE TABLE IF NOT EXISTS `mes_report_daily` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `shift_code` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALL' COMMENT '班次编码,ALL 表示全天',
    `area_id` BIGINT NOT NULL DEFAULT 0 COMMENT '区域,0 表示合计',
    `step_id` BIGINT NOT NULL DEFAULT 0 COMMENT '站点,0 表示合计',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '产品,0 表示合计',
    `unit_id` BIGINT NOT NULL DEFAULT 0 COMMENT '计数单位,0 表示合计(不同单位的数量一般不应合计)',
    `in_qty` BIGINT NOT NULL DEFAULT 0 COMMENT 'Track-In 数量',
    `out_qty` BIGINT NOT NULL DEFAULT 0 COMMENT 'Track-Out 数量',
    `scrap_qty` BIGINT NOT NULL DEFAULT 0 COMMENT '报废数量',
    `rework_qty` BIGINT NOT NULL DEFAULT 0 COMMENT '返工数量',
    `hold_count` INT NOT NULL DEFAULT 0 COMMENT 'Hold 次数',
    `avg_queue_min` DECIMAL(12,2) NULL COMMENT '平均 Queue 时长(分钟)',
    `avg_process_min` DECIMAL(12,2) NULL COMMENT '平均加工时长(分钟)',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_daily`(`tenant_id` ASC,`stat_date` ASC,`shift_code` ASC,`area_id` ASC,`step_id` ASC,`product_id` ASC,`unit_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '生产日报汇总';

-- hist_mes_wip_snapshot WIP 快照
CREATE TABLE IF NOT EXISTS `hist_mes_wip_snapshot` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `snapshot_at` DATETIME NOT NULL COMMENT '快照时间',
    `area_id` BIGINT NULL COMMENT '区域',
    `step_id` BIGINT NULL COMMENT '站点',
    `product_id` BIGINT NULL COMMENT '产品',
    `lot_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Lot 类型,空表示合计',
    `unit_id` BIGINT NULL COMMENT '计数单位(转换前后的数量不可直接相加,按单位分行)',
    `lot_count` INT NOT NULL DEFAULT 0 COMMENT 'Lot 数',
    `wafer_count` INT NOT NULL DEFAULT 0 COMMENT 'Wafer 数',
    `qty` BIGINT NOT NULL DEFAULT 0 COMMENT '数量(unit_id 口径)',
    `hold_lot_count` INT NOT NULL DEFAULT 0 COMMENT 'Hold Lot 数',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_time`(`tenant_id` ASC,`snapshot_at` ASC) USING BTREE,
    INDEX `idx_step_time`(`tenant_id` ASC,`step_id` ASC,`snapshot_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'WIP 快照';
