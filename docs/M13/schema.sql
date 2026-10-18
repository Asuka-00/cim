-- M13 质量 / 维保 / 高级分析 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_13__mes_m13.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- mes_defect_record 缺陷记录
CREATE TABLE IF NOT EXISTS `mes_defect_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `wafer_id` BIGINT NULL COMMENT 'Wafer',
    `step_id` BIGINT NULL COMMENT '发现站点',
    `defect_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '缺陷码(字典 MES_DEFECT_CODE)',
    `defect_qty` INT NOT NULL DEFAULT 1 COMMENT '缺陷数量',
    `pos_x` DECIMAL(12,4) NULL COMMENT '位置 X',
    `pos_y` DECIMAL(12,4) NULL COMMENT '位置 Y',
    `image_ref` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '图片 mes_file.id(M12)',
    `found_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '发现人',
    `found_at` DATETIME NOT NULL COMMENT '发现时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    INDEX `idx_code_time`(`tenant_id` ASC,`defect_code` ASC,`found_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '缺陷记录';

-- mes_ncr 不合格品报告 NCR
CREATE TABLE IF NOT EXISTS `mes_ncr` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `ncr_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'NCR 编号',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `step_id` BIGINT NULL COMMENT '发生站点',
    `ncr_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PROCESS' COMMENT 'PROCESS/MATERIAL/EQUIPMENT/INCOMING/OTHER',
    `qty` BIGINT NOT NULL COMMENT '不合格数量',
    `unit_id` BIGINT NULL COMMENT '计数单位',
    `description` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '问题描述',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/MRB/DISPOSED/CLOSED',
    `hold_id` BIGINT NULL COMMENT '首个关联的 Lot Hold(展示用;释放按 source_ref_id)',
    `reported_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '报告人',
    `reported_at` DATETIME NOT NULL COMMENT '报告时间',
    `closed_at` DATETIME NULL COMMENT '关闭时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_ncr_no`(`tenant_id` ASC,`ncr_no` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '不合格品报告 NCR';

-- mes_mrb MRB 评审
CREATE TABLE IF NOT EXISTS `mes_mrb` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `mrb_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MRB 编号',
    `ncr_id` BIGINT NOT NULL COMMENT 'NCR',
    `decision` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '处置 REWORK/SCRAP/USE_AS_IS/RETURN',
    `rework_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '返工分支编码(REWORK 时)',
    `decision_qty` BIGINT NULL COMMENT '处置数量',
    `decided_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评审人',
    `decided_at` DATETIME NULL COMMENT '评审时间',
    `executed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '处置是否已执行',
    `remark` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '评审意见',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_mrb_no`(`tenant_id` ASC,`mrb_no` ASC) USING BTREE,
    INDEX `idx_ncr`(`tenant_id` ASC,`ncr_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'MRB 评审';

-- hist_mes_yield_record 良率记录
CREATE TABLE IF NOT EXISTS `hist_mes_yield_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `wafer_id` BIGINT NULL COMMENT 'Wafer,空表示 Lot 级',
    `step_id` BIGINT NULL COMMENT '统计时点站点',
    `good_qty` BIGINT NOT NULL DEFAULT 0 COMMENT '良品数',
    `bad_qty` BIGINT NOT NULL DEFAULT 0 COMMENT '不良数',
    `yield_pct` DECIMAL(8,4) NULL COMMENT '良率(%)',
    `bin_summary` JSON NULL COMMENT 'Bin 汇总',
    `txn_id` BIGINT NULL COMMENT '来源事务,统计时经撤销登记排除',
    `recorded_at` DATETIME NOT NULL COMMENT '记录时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot_time`(`tenant_id` ASC,`lot_id` ASC,`recorded_at` ASC) USING BTREE,
    INDEX `idx_wafer`(`tenant_id` ASC,`wafer_id` ASC) USING BTREE,
    INDEX `idx_txn`(`tenant_id` ASC,`txn_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '良率记录';

-- mes_wafer_mark Wafer 标识
CREATE TABLE IF NOT EXISTS `mes_wafer_mark` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `wafer_id` BIGINT NOT NULL COMMENT 'Wafer',
    `mark_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'SCRIBE/LASER/BARCODE',
    `mark_value` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标识内容',
    `verified` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已扫码核对',
    `verified_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '核对人',
    `verified_at` DATETIME NULL COMMENT '核对时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_wafer_type`(`tenant_id` ASC,`wafer_id` ASC,`mark_type` ASC) USING BTREE,
    INDEX `idx_value`(`tenant_id` ASC,`mark_value` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Wafer 标识';

-- mes_wafer_sort_result Wafer Sort 结果
CREATE TABLE IF NOT EXISTS `mes_wafer_sort_result` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `wafer_id` BIGINT NOT NULL COMMENT 'Wafer',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `sort_grade` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '等级',
    `tested_at` DATETIME NULL COMMENT '测试时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_wafer`(`tenant_id` ASC,`wafer_id` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Wafer Sort 结果';

-- mes_pm_plan PM 计划
CREATE TABLE IF NOT EXISTS `mes_pm_plan` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `plan_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划编码',
    `plan_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '计划名称',
    `target_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EQUIPMENT' COMMENT 'EQUIPMENT/EQUIPMENT_GROUP',
    `target_id` BIGINT NOT NULL COMMENT '设备/设备组',
    `trigger_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'TIME/LOT_COUNT/WAFER_COUNT/USAGE',
    `interval_value` BIGINT NOT NULL COMMENT '触发间隔(天/Lot 数/Wafer 数/使用量)',
    `need_qual` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '完成后是否需要 Qual',
    `checklist_json` JSON NULL COMMENT '检查清单',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_plan_code`(`tenant_id` ASC,`plan_code` ASC) USING BTREE,
    INDEX `idx_target`(`tenant_id` ASC,`target_type` ASC,`target_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'PM 计划';

-- mes_pm_plan_equipment PM 计划-设备计数
CREATE TABLE IF NOT EXISTS `mes_pm_plan_equipment` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `plan_id` BIGINT NOT NULL COMMENT 'PM 计划',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `counter_value` BIGINT NOT NULL DEFAULT 0 COMMENT '距上次 PM 的累计计数',
    `last_done_at` DATETIME NULL COMMENT '上次完成时间',
    `next_due_at` DATETIME NULL COMMENT '下次到期时间(TIME 型)',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_plan_equipment`(`tenant_id` ASC,`plan_id` ASC,`equipment_id` ASC) USING BTREE,
    INDEX `idx_due`(`tenant_id` ASC,`next_due_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'PM 计划-设备计数';

-- hist_mes_pm_count_log PM 计数流水
CREATE TABLE IF NOT EXISTS `hist_mes_pm_count_log` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `plan_equipment_id` BIGINT NOT NULL COMMENT 'PM 计划-设备计数行',
    `equipment_id` BIGINT NOT NULL COMMENT '设备(冗余)',
    `txn_id` BIGINT NOT NULL COMMENT '计数来源事务;USAGE 型在 Batch 下取父事务',
    `direction` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'INCR/REVERSE',
    `delta` BIGINT NOT NULL COMMENT '计数量(正数)',
    `applied` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否实际改了计数;撤销跨 PM 周期时为 0',
    `counter_after` BIGINT NULL COMMENT '处理后计数',
    `event_id` BIGINT NULL COMMENT '来源领域事件',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX `uk_txn_plan_dir`(`tenant_id` ASC,`txn_id` ASC,`plan_equipment_id` ASC,`direction` ASC) USING BTREE,
    INDEX `idx_plan_time`(`tenant_id` ASC,`plan_equipment_id` ASC,`occurred_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'PM 计数流水';

-- mes_pm_order PM 工单
CREATE TABLE IF NOT EXISTS `mes_pm_order` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `pm_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PM 单号',
    `plan_id` BIGINT NULL COMMENT '来源计划',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/COMPLETED/QUAL_PENDING/CLOSED/CANCELLED',
    `scheduled_at` DATETIME NULL COMMENT '计划时间',
    `started_at` DATETIME NULL COMMENT '开始时间',
    `completed_at` DATETIME NULL COMMENT '完成时间',
    `executor` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '执行人',
    `result` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '执行结果',
    `qual_required` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要 Qual',
    `qual_result` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Qual 结论 PASS/FAIL',
    `qual_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Qual 确认人',
    `qual_at` DATETIME NULL COMMENT 'Qual 时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_pm_no`(`tenant_id` ASC,`pm_no` ASC) USING BTREE,
    INDEX `idx_equipment_status`(`tenant_id` ASC,`equipment_id` ASC,`status` ASC) USING BTREE,
    INDEX `idx_plan`(`tenant_id` ASC,`plan_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'PM 工单';

-- mes_repair_record 维修记录
CREATE TABLE IF NOT EXISTS `mes_repair_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `repair_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '维修单号',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `fault_id` BIGINT NULL COMMENT '关联异常单',
    `started_at` DATETIME NOT NULL COMMENT '开始时间',
    `finished_at` DATETIME NULL COMMENT '完成时间',
    `repaired_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '维修人',
    `parts_used` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更换部件',
    `result` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '维修结果',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_repair_no`(`tenant_id` ASC,`repair_no` ASC) USING BTREE,
    INDEX `idx_equipment_time`(`tenant_id` ASC,`equipment_id` ASC,`started_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '维修记录';

-- mes_equipment_daily_stat 设备日统计
CREATE TABLE IF NOT EXISTS `mes_equipment_daily_stat` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `productive_sec` BIGINT NOT NULL DEFAULT 0 COMMENT 'PRODUCTIVE 秒数',
    `standby_sec` BIGINT NOT NULL DEFAULT 0 COMMENT 'STANDBY 秒数',
    `engineering_sec` BIGINT NOT NULL DEFAULT 0 COMMENT 'ENGINEERING 秒数',
    `sched_down_sec` BIGINT NOT NULL DEFAULT 0 COMMENT 'SCHEDULED_DOWN 秒数',
    `unsched_down_sec` BIGINT NOT NULL DEFAULT 0 COMMENT 'UNSCHEDULED_DOWN 秒数',
    `non_sched_sec` BIGINT NOT NULL DEFAULT 0 COMMENT 'NON_SCHEDULED 秒数',
    `lot_count` INT NOT NULL DEFAULT 0 COMMENT '加工 Lot 数',
    `wafer_count` INT NOT NULL DEFAULT 0 COMMENT '加工 Wafer 数',
    `fault_count` INT NOT NULL DEFAULT 0 COMMENT '异常次数',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_date_equipment`(`tenant_id` ASC,`stat_date` ASC,`equipment_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '设备日统计';

-- mes_stocktake 盘点单
CREATE TABLE IF NOT EXISTS `mes_stocktake` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `stocktake_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '盘点单号',
    `wh_id` BIGINT NOT NULL COMMENT '库房',
    `location_id` BIGINT NULL COMMENT '库位,空表示整库',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/COUNTING/REVIEWING/POSTED/CANCELLED',
    `planned_at` DATETIME NULL COMMENT '计划时间',
    `counted_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '盘点人',
    `approved_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批人',
    `posted_at` DATETIME NULL COMMENT '过账时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_stocktake_no`(`tenant_id` ASC,`stocktake_no` ASC) USING BTREE,
    INDEX `idx_wh_status`(`tenant_id` ASC,`wh_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '盘点单';

-- mes_stocktake_item 盘点明细
CREATE TABLE IF NOT EXISTS `mes_stocktake_item` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `stocktake_id` BIGINT NOT NULL COMMENT '盘点单',
    `material_lot_id` BIGINT NOT NULL COMMENT '物料批',
    `location_id` BIGINT NOT NULL COMMENT '库位',
    `book_qty` DECIMAL(18,4) NOT NULL COMMENT '账面数量',
    `count_qty` DECIMAL(18,4) NULL COMMENT '实盘数量',
    `diff_qty` DECIMAL(18,4) NULL COMMENT '差异数量',
    `uom_id` BIGINT NOT NULL COMMENT '计量单位',
    `reason` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '差异原因',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_stocktake_lot_loc`(`tenant_id` ASC,`stocktake_id` ASC,`material_lot_id` ASC,`location_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '盘点明细';

-- mes_dashboard 自定义看板
CREATE TABLE IF NOT EXISTS `mes_dashboard` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `dashboard_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '看板编码',
    `dashboard_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '看板名称',
    `owner` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '所有者',
    `is_public` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否公开',
    `layout_json` JSON NOT NULL COMMENT '布局与组件配置',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_dashboard_code`(`tenant_id` ASC,`dashboard_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '自定义看板';
