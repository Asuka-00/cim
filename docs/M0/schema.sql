-- M0 地基与骨架 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_0__mes_m0.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- mes_sequence_rule 编号规则
CREATE TABLE IF NOT EXISTS `mes_sequence_rule` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则编码,如 WORK_ORDER/LOT/WAFER/BATCH/MATERIAL_LOT/STRIP/REEL',
    `rule_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '规则名称',
    `biz_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务对象类型',
    `prefix` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '前缀',
    `date_pattern` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '日期模板:yyyyMMdd/yyMM/yyMMdd/yyyy,空表示无',
    `seq_length` TINYINT NOT NULL DEFAULT 4 COMMENT '流水号长度,不足左补 0',
    `seq_start` BIGINT NOT NULL DEFAULT 1 COMMENT '流水起始值',
    `seq_step` INT NOT NULL DEFAULT 1 COMMENT '流水步长',
    `reset_cycle` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NEVER' COMMENT '重置周期 NEVER/DAY/MONTH/YEAR',
    `suffix` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '后缀',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_code`(`tenant_id` ASC,`rule_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '编号规则';

-- mes_sequence_counter 编号流水计数
CREATE TABLE IF NOT EXISTS `mes_sequence_counter` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `rule_id` BIGINT NOT NULL COMMENT '规则ID',
    `period_key` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '周期键:NEVER 时为 -,DAY 时为 yyyyMMdd,MONTH 为 yyyyMM,YEAR 为 yyyy',
    `current_value` BIGINT NOT NULL DEFAULT 0 COMMENT '当前已发放的最大流水值',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_rule_period`(`tenant_id` ASC,`rule_id` ASC,`period_key` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '编号流水计数';

-- mes_object_lock 对象逻辑锁(跨请求长占用)
CREATE TABLE IF NOT EXISTS `mes_object_lock` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `object_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '对象类型 BATCH/LOCATION/WAREHOUSE/STOCKTAKE/ROUTE_VERSION 等',
    `object_id` BIGINT NOT NULL COMMENT '对象ID',
    `lock_owner` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '持锁人/会话标识',
    `purpose` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '用途说明',
    `locked_at` DATETIME NOT NULL COMMENT '加锁时间',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_object`(`tenant_id` ASC,`object_type` ASC,`object_id` ASC) USING BTREE,
    INDEX `idx_expire`(`tenant_id` ASC,`expire_at` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '对象逻辑锁(跨请求长占用)';

-- hist_mes_transaction 事务流水
CREATE TABLE IF NOT EXISTS `hist_mes_transaction` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `txn_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事务号(雪花ID字符串)',
    `txn_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事务类型 CREATE_LOT/TRACK_IN/TRACK_OUT/HOLD/RELEASE/SCRAP/…',
    `object_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主对象类型',
    `object_id` BIGINT NULL COMMENT '主对象ID',
    `object_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '主对象编码(冗余,便于检索)',
    `parent_txn_id` BIGINT NULL COMMENT '父事务ID,Batch 等展开的子事务指向父事务',
    `request_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '客户端请求ID(诊断用,不要求唯一)',
    `idem_key` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '幂等键 = txn_type:request_id,仅 result=SUCCESS 时有值,否则为 NULL',
    `ref_txn_id` BIGINT NULL COMMENT '关联原事务ID,纠错/撤销事务指向被纠正的事务',
    `source` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ADMIN' COMMENT '来源 TERMINAL/ADMIN/IMPORT/SYSTEM',
    `reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原因码',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人(用户名)',
    `result` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '结果 SUCCESS/FAILED',
    `error_message` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '失败原因',
    `payload_json` JSON NULL COMMENT '请求与变更摘要',
    `remark` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `txn_time` DATETIME NOT NULL COMMENT '事务发生时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX `uk_txn_no`(`tenant_id` ASC,`txn_no` ASC) USING BTREE,
    UNIQUE INDEX `uk_idem_key`(`tenant_id` ASC,`idem_key` ASC) USING BTREE,
    INDEX `idx_object`(`tenant_id` ASC,`object_type` ASC,`object_id` ASC,`txn_time` ASC) USING BTREE,
    INDEX `idx_type_time`(`tenant_id` ASC,`txn_type` ASC,`txn_time` ASC) USING BTREE,
    INDEX `idx_operator_time`(`tenant_id` ASC,`operator` ASC,`txn_time` ASC) USING BTREE,
    INDEX `idx_parent`(`tenant_id` ASC,`parent_txn_id` ASC) USING BTREE,
    INDEX `idx_ref`(`tenant_id` ASC,`ref_txn_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '事务流水';

-- mes_domain_event 内部领域事件
CREATE TABLE IF NOT EXISTS `mes_domain_event` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `event_no` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件号(雪花ID字符串)',
    `event_type` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '事件类型,如 LOT_HOLD/LOT_TRACK_OUT/MATERIAL_LOW',
    `aggregate_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '聚合类型',
    `aggregate_id` BIGINT NULL COMMENT '聚合ID',
    `txn_id` BIGINT NULL COMMENT '来源事务ID',
    `payload_json` JSON NULL COMMENT '事件内容',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NEW' COMMENT '分发状态 NEW/DISPATCHED/FAILED(订阅者处理状态见投递表)',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `next_retry_at` DATETIME NULL COMMENT '下次重试时间',
    `processed_at` DATETIME NULL COMMENT '处理完成时间',
    `error_message` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '最近失败原因',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_event_no`(`tenant_id` ASC,`event_no` ASC) USING BTREE,
    INDEX `idx_status_retry`(`tenant_id` ASC,`status` ASC,`next_retry_at` ASC) USING BTREE,
    INDEX `idx_aggregate`(`tenant_id` ASC,`aggregate_type` ASC,`aggregate_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '内部领域事件';

-- mes_domain_event_delivery 领域事件投递
CREATE TABLE IF NOT EXISTS `mes_domain_event_delivery` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `event_id` BIGINT NOT NULL COMMENT '领域事件ID',
    `subscriber` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订阅者编码,如 WO_PROGRESS/ALERT_RULE/PM_COUNTER/MATERIAL_REQ',
    `aggregate_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '聚合类型(冗余,用于同聚合顺序投递)',
    `aggregate_id` BIGINT NULL COMMENT '聚合ID(冗余)',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED/SKIPPED',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `next_retry_at` DATETIME NULL COMMENT '下次重试时间',
    `processed_at` DATETIME NULL COMMENT '处理完成时间',
    `error_message` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '最近失败原因',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `uk_event_subscriber`(`tenant_id` ASC,`event_id` ASC,`subscriber` ASC) USING BTREE,
    INDEX `idx_status_retry`(`tenant_id` ASC,`status` ASC,`next_retry_at` ASC) USING BTREE,
    INDEX `idx_aggregate`(`tenant_id` ASC,`subscriber` ASC,`aggregate_type` ASC,`aggregate_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '领域事件投递';

-- hist_mes_txn_reversal 事务撤销登记
CREATE TABLE IF NOT EXISTS `hist_mes_txn_reversal` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `orig_txn_id` BIGINT NOT NULL COMMENT '被撤销的原事务',
    `reversal_txn_id` BIGINT NOT NULL COMMENT '执行撤销的纠错事务',
    `object_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '主对象类型',
    `object_id` BIGINT NULL COMMENT '主对象ID',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人',
    `reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原因码',
    `reversed_at` DATETIME NOT NULL COMMENT '撤销时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX `uk_orig_txn`(`tenant_id` ASC,`orig_txn_id` ASC) USING BTREE,
    INDEX `idx_reversal_txn`(`tenant_id` ASC,`reversal_txn_id` ASC) USING BTREE,
    INDEX `idx_object`(`tenant_id` ASC,`object_type` ASC,`object_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '事务撤销登记';

-- mes_alert 站内预警
CREATE TABLE IF NOT EXISTS `mes_alert` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `alert_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'MATERIAL_LOW/MATERIAL_EXPIRED/BINDING_DEPLETED/QUEUE_TIME/TIME_LINK/HOLD_TIMEOUT/TOOL_LIFE/EQP_FAULT/PM_DUE/SYSTEM',
    `level` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'WARN' COMMENT 'INFO/WARN/ERROR',
    `subject_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '对象类型',
    `subject_id` BIGINT NULL COMMENT '对象ID',
    `title` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
    `content` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '内容',
    `status` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NEW' COMMENT 'NEW/READ/CLOSED',
    `receiver_scope` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '接收范围 ALL/ROLE:xxx/USER:xxx/AREA:xxx',
    `read_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '已读人',
    `read_at` DATETIME NULL COMMENT '已读时间',
    `event_id` BIGINT NULL COMMENT '来源领域事件',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    INDEX `idx_status_time`(`tenant_id` ASC,`status` ASC,`create_at` ASC) USING BTREE,
    INDEX `idx_subject`(`tenant_id` ASC,`subject_type` ASC,`subject_id` ASC) USING BTREE,
    INDEX `idx_type_status`(`tenant_id` ASC,`alert_type` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站内预警';

-- shedlock ShedLock 分布式任务锁(标准结构,无通用审计列)
CREATE TABLE IF NOT EXISTS `shedlock` (
    `name` VARCHAR(64) NOT NULL COMMENT '锁名称',
    `lock_until` TIMESTAMP(3) NOT NULL COMMENT '锁定截止时间',
    `locked_at` TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加锁时间',
    `locked_by` VARCHAR(255) NOT NULL COMMENT '持锁实例',
    PRIMARY KEY (`name`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'ShedLock 分布式任务锁';
