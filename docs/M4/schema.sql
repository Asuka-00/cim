-- M4 执行扩展 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_4__mes_m4.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- hist_mes_lot_relation Lot 关系
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

-- mes_route_rework Route 返工分支
CREATE TABLE IF NOT EXISTS `mes_route_rework` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `rework_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '返工分支编码',
    `rework_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '返工分支名称',
    `entry_from_seq` INT NOT NULL COMMENT '允许进入的主流程步骤范围起',
    `entry_to_seq` INT NOT NULL COMMENT '允许进入的主流程步骤范围止',
    `return_seq_no` INT NOT NULL COMMENT '返工完成后回到的主流程步骤序号',
    `max_count` INT NOT NULL DEFAULT 1 COMMENT '同一 Lot 在该分支的最大返工次数',
    `reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '默认返工原因码',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rework_code`(`tenant_id` ASC,`route_version_id` ASC,`rework_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 返工分支';

-- mes_route_rework_step Route 返工步骤
CREATE TABLE IF NOT EXISTS `mes_route_rework_step` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rework_id` BIGINT NOT NULL COMMENT '返工分支',
    `seq_no` INT NOT NULL COMMENT '返工内序号',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `is_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '必经',
    `recipe_id` BIGINT NULL COMMENT '步骤指定 Recipe',
    `equipment_group_id` BIGINT NULL COMMENT '步骤指定设备组',
    `count_unit_id` BIGINT NULL COMMENT '计数单位',
    `queue_time_max_min` INT NULL COMMENT '进入本站点到 Track-In 最长等待(分钟)',
    `queue_time_action` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '超时动作 HOLD/WARN',
    `std_process_min` DECIMAL(10,2) NULL COMMENT '标准加工时长(分钟)',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rework_seq`(`tenant_id` ASC,`rework_id` ASC,`seq_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 返工步骤';

-- mes_route_condition Route 条件跳转
CREATE TABLE IF NOT EXISTS `mes_route_condition` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `from_seq_no` INT NOT NULL COMMENT '在哪个主流程步骤 Track-Out 后判定',
    `to_seq_no` INT NOT NULL COMMENT '满足条件跳转到的步骤序号',
    `condition_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'LOT_TYPE/QTY/COLLECT_RESULT/FLAG/PRODUCT_ATTR',
    `condition_json` JSON NOT NULL COMMENT '条件表达式 {"op":"EQ","field":"…","value":"…"}',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '多条件命中时数小优先',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_from`(`tenant_id` ASC,`route_version_id` ASC,`from_seq_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 条件跳转';

-- mes_future_action 预约动作
CREATE TABLE IF NOT EXISTS `mes_future_action` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `route_version_id` BIGINT NOT NULL COMMENT '预约时的 Route 版本,改 Route 后旧版本预约取消',
    `route_seq_no` INT NOT NULL COMMENT '触发的主流程步骤序号',
    `pass_no` INT NULL COMMENT '指定第几次经过触发,空表示首次到达',
    `timing` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ON_ARRIVE' COMMENT '触发时机 ON_ARRIVE(上一站出站事务内到站时)/AFTER_TRACK_OUT',
    `action_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'HOLD/SPLIT/CHANGE_RECIPE/CHANGE_PRIORITY/NOTIFY',
    `action_params` JSON NULL COMMENT '动作参数',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED/CANCELLED',
    `created_by_user` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '预约人',
    `executed_at` DATETIME NULL COMMENT '执行时间',
    `result_message` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '执行结果/失败原因',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_lot_status`(`tenant_id` ASC,`lot_id` ASC,`status` ASC) USING BTREE,
    INDEX `idx_trigger`(`tenant_id` ASC,`route_seq_no` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '预约动作';

-- mes_carrier 载具(可复用的过程载具)
CREATE TABLE IF NOT EXISTS `mes_carrier` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `carrier_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '载具编码',
    `carrier_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FOUP' COMMENT 'FOUP/CASSETTE/FRAME/MAGAZINE/OTHER',
    `capacity` INT NOT NULL DEFAULT 25 COMMENT '容量(Slot 数或件数)',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'EMPTY' COMMENT 'EMPTY/OCCUPIED/CLEANING/MAINTENANCE/SCRAPPED',
    `current_lot_id` BIGINT NULL COMMENT '当前绑定 Lot(与 Lot 绑定关系的唯一事实来源)',
    `use_count` INT NOT NULL DEFAULT 0 COMMENT '累计使用次数',
    `clean_cycle_uses` INT NULL COMMENT '清洗周期(次),空表示不管控',
    `last_clean_at` DATETIME NULL COMMENT '最近清洗时间',
    `location_text` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '位置',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_carrier_code`(`tenant_id` ASC,`carrier_code` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`current_lot_id` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '载具(可复用的过程载具)';

-- hist_mes_carrier_binding 载具绑定历史(时间段表)
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
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '载具绑定历史(时间段表)';

-- hist_mes_slot_change Slot 变更历史
CREATE TABLE IF NOT EXISTS `hist_mes_slot_change` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `wafer_id` BIGINT NOT NULL COMMENT 'Wafer',
    `carrier_id` BIGINT NULL COMMENT '载具',
    `slot_before` INT NULL COMMENT '变更前 Slot',
    `slot_after` INT NULL COMMENT '变更后 Slot',
    `reason` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原因',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot_time`(`tenant_id` ASC,`lot_id` ASC,`occurred_at` ASC) USING BTREE,
    INDEX `idx_wafer`(`tenant_id` ASC,`wafer_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Slot 变更历史';

-- mes_lot_shipment Lot 出货记录
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

-- hist_mes_lot_route_change Lot 工艺变更记录
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
    `txn_id` BIGINT NULL COMMENT '事务ID',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC,`occurred_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot 工艺变更记录';

-- mes_lot_note Lot 备注/标签
CREATE TABLE IF NOT EXISTS `mes_lot_note` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `note_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NOTE' COMMENT 'NOTE/WARNING/ENGINEERING/TAG',
    `content` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容(TAG 类型为标签文本)',
    `pinned` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶/在终端醒目显示',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot 备注/标签';

-- mes_dispatch_rule 作业清单排序规则
CREATE TABLE IF NOT EXISTS `mes_dispatch_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
    `scope_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GLOBAL' COMMENT '适用范围 GLOBAL/STEP/EQUIPMENT_GROUP/AREA',
    `scope_id` BIGINT NOT NULL DEFAULT 0 COMMENT '范围对象ID,GLOBAL 时为 0',
    `sort_keys` JSON NOT NULL COMMENT '排序键数组 [{"key","dir"}]',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '默认规则',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_code`(`tenant_id` ASC,`rule_code` ASC) USING BTREE,
    INDEX `idx_scope`(`tenant_id` ASC,`scope_type` ASC,`scope_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '作业清单排序规则';

-- 变更 mes_lot
ALTER TABLE `mes_lot`
    ADD COLUMN `parent_lot_id` BIGINT NULL COMMENT '父 Lot(Split 产生的子 Lot)',
    ADD COLUMN `root_lot_id` BIGINT NULL COMMENT '根 Lot',
    ADD COLUMN `split_seq` INT NULL COMMENT '拆批序号',
    ADD COLUMN `rework_id` BIGINT NULL COMMENT '所在返工分支,空表示主流程',
    ADD COLUMN `rework_seq_no` INT NULL COMMENT '返工分支内步骤序号',
    ADD COLUMN `recipe_override_id` BIGINT NULL COMMENT 'Future Action CHANGE_RECIPE 指定的 Recipe',
    ADD COLUMN `recipe_override_seq_no` INT NULL COMMENT '该指定生效的主流程步骤序号',
    ADD INDEX `idx_parent`(`tenant_id` ASC,`parent_lot_id` ASC) USING BTREE,
    ADD INDEX `idx_root`(`tenant_id` ASC,`root_lot_id` ASC) USING BTREE;

-- 变更 mes_wafer
ALTER TABLE `mes_wafer`
    ADD COLUMN `max_use_count` INT NULL COMMENT 'Dummy/Monitor 最大使用次数,空表示取全局参数';

-- 变更 mes_hold_reason
ALTER TABLE `mes_hold_reason`
    ADD COLUMN `timeout_minutes` INT NULL COMMENT 'Hold 超时提醒阈值(分钟)';

-- 变更 mes_work_order
ALTER TABLE `mes_work_order`
    ADD COLUMN `parent_wo_id` BIGINT NULL COMMENT '父工单(拆分产生)',
    ADD COLUMN `merged_into_wo_id` BIGINT NULL COMMENT '已合并到的工单',
    ADD INDEX `idx_parent`(`tenant_id` ASC,`parent_wo_id` ASC) USING BTREE;
