-- M8 管控完整化 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_8__mes_m8.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- mes_tool_type 工具类型
CREATE TABLE IF NOT EXISTS `mes_tool_type` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `type_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型编码',
    `type_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型名称',
    `life_unit` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USES' COMMENT '寿命单位 USES/WAFERS/MINUTES',
    `default_life` INT NULL COMMENT '默认寿命上限',
    `clean_cycle` INT NULL COMMENT '默认清洗周期(同寿命单位)',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_type_code`(`tenant_id` ASC,`type_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '工具类型';

-- mes_tool 工具台账
CREATE TABLE IF NOT EXISTS `mes_tool` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `tool_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具编码',
    `tool_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '工具名称',
    `type_id` BIGINT NOT NULL COMMENT '工具类型',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE/IN_USE/CLEANING/MAINTENANCE/SCRAPPED',
    `used_count` BIGINT NOT NULL DEFAULT 0 COMMENT '累计使用量(按寿命单位)',
    `life_limit` BIGINT NULL COMMENT '寿命上限,空表示不限',
    `since_clean` BIGINT NOT NULL DEFAULT 0 COMMENT '距上次清洗使用量',
    `current_equipment_id` BIGINT NULL COMMENT '当前装载设备',
    `last_clean_at` DATETIME NULL COMMENT '最近清洗时间',
    `last_maint_at` DATETIME NULL COMMENT '最近保养时间',
    `location_text` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '存放位置',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_tool_code`(`tenant_id` ASC,`tool_code` ASC) USING BTREE,
    INDEX `idx_type_status`(`tenant_id` ASC,`type_id` ASC,`status` ASC) USING BTREE,
    INDEX `idx_equipment`(`tenant_id` ASC,`current_equipment_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '工具台账';

-- mes_step_tool 站点-工具要求(站点级)
CREATE TABLE IF NOT EXISTS `mes_step_tool` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `tool_type_id` BIGINT NOT NULL COMMENT '所需工具类型',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `required_qty` INT NOT NULL DEFAULT 1 COMMENT '所需数量',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_step_tool`(`tenant_id` ASC,`step_id` ASC,`tool_type_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站点-工具要求(站点级)';

-- mes_route_step_tool Route 步骤-工具覆盖(步骤级,随 Route 版本)
CREATE TABLE IF NOT EXISTS `mes_route_step_tool` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `route_step_id` BIGINT NOT NULL COMMENT 'Route 步骤',
    `tool_type_id` BIGINT NOT NULL COMMENT '所需工具类型(如每层不同 Reticle)',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `required_qty` INT NOT NULL DEFAULT 1 COMMENT '所需数量',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_route_step_tool`(`tenant_id` ASC,`route_step_id` ASC,`tool_type_id` ASC,`product_id` ASC) USING BTREE,
    INDEX `idx_version`(`tenant_id` ASC,`route_version_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 步骤-工具覆盖(步骤级,随 Route 版本)';

-- hist_mes_tool_event 工具使用与保养记录
CREATE TABLE IF NOT EXISTS `hist_mes_tool_event` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `tool_id` BIGINT NOT NULL COMMENT '工具',
    `event_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'CHECKOUT/RETURN/TRACK_IN_BIND/UNBIND/USE/USE_REVERSAL/CLEAN/MAINTAIN/SCRAP',
    `lot_id` BIGINT NULL COMMENT '关联 Lot',
    `equipment_id` BIGINT NULL COMMENT '关联设备',
    `step_id` BIGINT NULL COMMENT '关联站点',
    `usage_qty` BIGINT NULL COMMENT '本次使用量',
    `txn_id` BIGINT NULL COMMENT '产生本事件的事务(绑定为 Track-In,使用为 Track-Out,纠错按它查找)',
    `bind_txn_id` BIGINT NULL COMMENT 'USE/USE_REVERSAL 对应的 Track-In 绑定事务',
    `used_total_after` BIGINT NULL COMMENT '事件后累计使用量',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_tool_time`(`tenant_id` ASC,`tool_id` ASC,`occurred_at` ASC) USING BTREE,
    INDEX `idx_lot`(`tenant_id` ASC,`lot_id` ASC) USING BTREE,
    INDEX `idx_txn`(`tenant_id` ASC,`txn_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '工具使用与保养记录';

-- mes_user_qual 人员资质
CREATE TABLE IF NOT EXISTS `mes_user_qual` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `user_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名(sys_user.user_name,与 operator 等字段口径一致)',
    `target_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'EQUIPMENT/EQUIPMENT_GROUP/STEP',
    `target_id` BIGINT NOT NULL COMMENT '目标对象ID',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VALID' COMMENT 'VALID/EXPIRED/REVOKED',
    `valid_from` DATETIME NULL COMMENT '生效起',
    `valid_to` DATETIME NULL COMMENT '生效止',
    `certified_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '认证人',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_user_qual`(`tenant_id` ASC,`user_name` ASC,`target_type` ASC,`target_id` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC,`valid_to` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '人员资质';

-- mes_interlock_rule Interlock 规则
CREATE TABLE IF NOT EXISTS `mes_interlock_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码',
    `rule_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
    `rule_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'LOT_STATE/STEP_MATCH/EQP_ALLOWED/EQP_STATE/EQP_CAPACITY/RECIPE/TOOL/PERSON/QUEUE_TIME/TIME_LINK/MATERIAL',
    `scope_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'GLOBAL' COMMENT 'GLOBAL/STEP/PRODUCT/EQUIPMENT_GROUP/AREA',
    `scope_id` BIGINT NULL COMMENT '范围对象ID',
    `severity` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'BLOCK' COMMENT 'BLOCK/WARN',
    `is_builtin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '系统内置,不可删除/不可停用/不可降级',
    `params_json` JSON NULL COMMENT '规则参数,如严格模式、预警阈值',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '执行顺序',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_code`(`tenant_id` ASC,`rule_code` ASC) USING BTREE,
    INDEX `idx_type_scope`(`tenant_id` ASC,`rule_type` ASC,`scope_type` ASC,`scope_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Interlock 规则';

-- hist_mes_interlock_log Interlock 校验日志
CREATE TABLE IF NOT EXISTS `hist_mes_interlock_log` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `lot_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'Lot 号',
    `step_id` BIGINT NULL COMMENT '站点',
    `route_seq_no` INT NULL COMMENT '步骤序号',
    `equipment_id` BIGINT NULL COMMENT '设备',
    `action` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TRACK_IN' COMMENT '被校验动作 TRACK_IN/BATCH_TRACK_IN/PRECHECK',
    `result` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PASS/WARN/BLOCK',
    `details_json` JSON NULL COMMENT '逐条规则结果 [{ruleCode,type,result,message}]',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '操作人',
    `checked_at` DATETIME NOT NULL COMMENT '校验时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_lot_time`(`tenant_id` ASC,`lot_id` ASC,`checked_at` ASC) USING BTREE,
    INDEX `idx_result_time`(`tenant_id` ASC,`result` ASC,`checked_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Interlock 校验日志';

-- mes_lot_time_link_timer Lot Time Link 计时
CREATE TABLE IF NOT EXISTS `mes_lot_time_link_timer` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `time_link_id` BIGINT NOT NULL COMMENT 'Time Link 约束(mes_route_time_link)',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `from_seq_no` INT NOT NULL COMMENT '起点步骤序号',
    `to_seq_no` INT NOT NULL COMMENT '终点步骤序号',
    `start_at` DATETIME NOT NULL COMMENT '开始计时(起点步骤 Track-Out 时间)',
    `deadline_at` DATETIME NOT NULL COMMENT '超限时间 = start_at + max_minutes',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DONE/CANCELLED',
    `alert_level` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NONE' COMMENT 'NONE/WARNED/BREACHED,扫描任务据此不重复处理',
    `start_txn_id` BIGINT NOT NULL COMMENT '开始计时的出站事务',
    `end_txn_id` BIGINT NULL COMMENT '结束计时的事务(终点 Track-In 或取消)',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `uk_lot_link_start`(`tenant_id` ASC,`lot_id` ASC,`time_link_id` ASC,`start_txn_id` ASC) USING BTREE,
    INDEX `idx_status_deadline`(`tenant_id` ASC,`status` ASC,`deadline_at` ASC) USING BTREE,
    INDEX `idx_lot_status`(`tenant_id` ASC,`lot_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Lot Time Link 计时';

-- mes_user_scope 用户数据权限
CREATE TABLE IF NOT EXISTS `mes_user_scope` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `user_name` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名(sys_user.user_name)',
    `scope_type` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AREA/STEP/EQUIPMENT_GROUP',
    `scope_id` BIGINT NOT NULL COMMENT '范围对象ID',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_user_scope`(`tenant_id` ASC,`user_name` ASC,`scope_type` ASC,`scope_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '用户数据权限';

-- mes_equipment_fault 设备异常
CREATE TABLE IF NOT EXISTS `mes_equipment_fault` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `fault_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '异常单号',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `fault_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '异常码',
    `severity` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MINOR' COMMENT 'MINOR/MAJOR/CRITICAL',
    `description` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '异常描述',
    `occurred_at` DATETIME NOT NULL COMMENT '发生时间',
    `reported_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '登记人',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/HANDLING/CLOSED',
    `handled_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '处理人',
    `handled_at` DATETIME NULL COMMENT '处理完成时间',
    `solution` VARCHAR(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '处理结果',
    `affected_lot_count` INT NOT NULL DEFAULT 0 COMMENT '影响 Lot 数',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_fault_no`(`tenant_id` ASC,`fault_no` ASC) USING BTREE,
    INDEX `idx_equipment_time`(`tenant_id` ASC,`equipment_id` ASC,`occurred_at` ASC) USING BTREE,
    INDEX `idx_status`(`tenant_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '设备异常';

-- mes_equipment_fault_lot 设备异常影响 Lot
CREATE TABLE IF NOT EXISTS `mes_equipment_fault_lot` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `fault_id` BIGINT NOT NULL COMMENT '异常单',
    `lot_id` BIGINT NOT NULL COMMENT 'Lot',
    `impact` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '影响说明',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_fault_lot`(`tenant_id` ASC,`fault_id` ASC,`lot_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '设备异常影响 Lot';

-- mes_confirm_rule 二次确认规则
CREATE TABLE IF NOT EXISTS `mes_confirm_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `action_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作编码 HOLD_RELEASE/SCRAP/UNSCRAP/FORCE_MOVE/VERSION_ACTIVATE/…',
    `confirm_mode` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REAUTH' COMMENT '模式 NONE/REAUTH/SECOND_SIGN',
    `second_role_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '第二签名人需具备的角色',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_action`(`tenant_id` ASC,`action_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '二次确认规则';

-- hist_mes_esign 电子签名记录
CREATE TABLE IF NOT EXISTS `hist_mes_esign` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `action_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作编码',
    `biz_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '业务对象类型',
    `biz_id` BIGINT NULL COMMENT '业务对象ID',
    `signer` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '签名人',
    `signer_role` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '签名人角色',
    `sign_type` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'PRIMARY/SECOND',
    `meaning` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '签名含义/原因',
    `txn_id` BIGINT NULL COMMENT '事务ID',
    `signed_at` DATETIME NOT NULL COMMENT '签名时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_biz`(`tenant_id` ASC,`biz_type` ASC,`biz_id` ASC) USING BTREE,
    INDEX `idx_signer_time`(`tenant_id` ASC,`signer` ASC,`signed_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '电子签名记录';
