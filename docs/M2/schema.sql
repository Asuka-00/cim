-- M2 主数据 II:制程与版本 · 草案 DDL(MySQL 8)
-- 实现时转为 app-boot/src/main/resources/db/migration/mysql/V1_1_2__mes_m2.sql
-- 首期只支持 MySQL 8(D20),不生成其他数据库脚本;请勿直接执行本草案。

SET NAMES utf8mb4;

-- hist_mes_approval_record 版本审批记录
CREATE TABLE IF NOT EXISTS `hist_mes_approval_record` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `biz_type` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型 PRODUCT_VERSION/ROUTE_VERSION/BOM_VERSION/RECIPE_VERSION/CONSUMPTION_ADJUST/ECN',
    `biz_id` BIGINT NOT NULL COMMENT '业务对象ID',
    `action` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '动作 SUBMIT/APPROVE/REJECT/REVOKE',
    `from_status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '变更前状态',
    `to_status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '变更后状态',
    `assignee` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '指定审批人',
    `operator` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作人',
    `comment` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '意见',
    `action_at` DATETIME NOT NULL COMMENT '动作时间',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_biz`(`tenant_id` ASC,`biz_type` ASC,`biz_id` ASC,`action_at` ASC) USING BTREE,
    INDEX `idx_assignee`(`tenant_id` ASC,`assignee` ASC,`action` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '版本审批记录';

-- mes_step_group 站点组(工艺模块)
CREATE TABLE IF NOT EXISTS `mes_step_group` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `group_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点组编码,如 PHOTO/ETCH/DIFF',
    `group_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '站点组名称',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_group_code`(`tenant_id` ASC,`group_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站点组(工艺模块)';

-- mes_route Route 主数据
CREATE TABLE IF NOT EXISTS `mes_route` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Route 编码',
    `route_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Route 名称',
    `description` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '说明',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_route_code`(`tenant_id` ASC,`route_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 主数据';

-- mes_route_version Route 版本
CREATE TABLE IF NOT EXISTS `mes_route_version` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_id` BIGINT NOT NULL COMMENT 'Route',
    `version_no` INT NOT NULL COMMENT '版本号',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/ACTIVE/INACTIVE',
    `copy_from_version_id` BIGINT NULL COMMENT '复制来源版本',
    `effective_at` DATETIME NULL COMMENT '生效时间',
    `expire_at` DATETIME NULL COMMENT '失效时间',
    `submitted_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '提交人',
    `approved_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批人',
    `approved_at` DATETIME NULL COMMENT '审批时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_route_version`(`tenant_id` ASC,`route_id` ASC,`version_no` ASC) USING BTREE,
    INDEX `idx_route_status`(`tenant_id` ASC,`route_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 版本';

-- mes_route_step Route 步骤
CREATE TABLE IF NOT EXISTS `mes_route_step` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `seq_no` INT NOT NULL COMMENT '步骤序号,版本内唯一,建议 10 步进',
    `step_id` BIGINT NOT NULL COMMENT '站点;允许在同一版本内重复出现',
    `step_alias` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '步骤别名(同站点多次出现时区分,如"光刻-层1")',
    `is_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '必经站点;否则可 Skip 或被抽检规则跳过',
    `equipment_group_id` BIGINT NULL COMMENT '步骤指定设备组,空表示取站点级绑定',
    `count_unit_id` BIGINT NULL COMMENT '进入本步骤时的计数单位,空表示沿用上一步/产品基础单位',
    `is_unit_convert` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否单位转换站点',
    `convert_to_unit_id` BIGINT NULL COMMENT '转换后的计数单位',
    `std_process_min` DECIMAL(10,2) NULL COMMENT '标准加工时长(分钟)',
    `queue_time_max_min` INT NULL COMMENT '进入本站点到 Track-In 最长等待(分钟),空表示不限',
    `queue_time_warn_min` INT NULL COMMENT 'Queue Time 预警阈值(分钟)',
    `queue_time_action` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '超时动作 HOLD/WARN',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_version_seq`(`tenant_id` ASC,`route_version_id` ASC,`seq_no` ASC) USING BTREE,
    INDEX `idx_step`(`tenant_id` ASC,`step_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 步骤';

-- mes_route_time_link Route Time Link 约束
CREATE TABLE IF NOT EXISTS `mes_route_time_link` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `from_seq_no` INT NOT NULL COMMENT '起点步骤序号(其 Track-Out 开始计时)',
    `to_seq_no` INT NOT NULL COMMENT '终点步骤序号(其 Track-In 结束计时)',
    `max_minutes` INT NOT NULL COMMENT '最长间隔(分钟)',
    `warn_minutes` INT NULL COMMENT '预警阈值(分钟)',
    `action` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'HOLD' COMMENT '超时动作 HOLD/WARN',
    `reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '自动 Hold 使用的原因码',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_link`(`tenant_id` ASC,`route_version_id` ASC,`from_seq_no` ASC,`to_seq_no` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route Time Link 约束';

-- mes_step_equipment_group 站点-设备组绑定
CREATE TABLE IF NOT EXISTS `mes_step_equipment_group` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `group_id` BIGINT NOT NULL COMMENT '设备组',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级,数小优先',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_step_group`(`tenant_id` ASC,`step_id` ASC,`group_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站点-设备组绑定';

-- mes_step_recipe 站点-Recipe 绑定(站点级)
CREATE TABLE IF NOT EXISTS `mes_step_recipe` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `step_id` BIGINT NOT NULL COMMENT '站点',
    `recipe_id` BIGINT NOT NULL COMMENT 'Recipe',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认 Recipe',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_step_recipe`(`tenant_id` ASC,`step_id` ASC,`recipe_id` ASC,`product_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '站点-Recipe 绑定(站点级)';

-- mes_route_step_recipe Route 步骤-Recipe 覆盖(步骤级,随 Route 版本)
CREATE TABLE IF NOT EXISTS `mes_route_step_recipe` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `route_version_id` BIGINT NOT NULL COMMENT 'Route 版本',
    `route_step_id` BIGINT NOT NULL COMMENT 'Route 步骤',
    `recipe_id` BIGINT NOT NULL COMMENT 'Recipe',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示通用',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认 Recipe',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_route_step_recipe`(`tenant_id` ASC,`route_step_id` ASC,`recipe_id` ASC,`product_id` ASC) USING BTREE,
    INDEX `idx_version`(`tenant_id` ASC,`route_version_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'Route 步骤-Recipe 覆盖(步骤级,随 Route 版本)';

-- mes_equipment_port 设备 Port/Chamber
CREATE TABLE IF NOT EXISTS `mes_equipment_port` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `port_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Port/Chamber 编码',
    `port_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '名称',
    `kind` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PORT' COMMENT '类型 PORT/CHAMBER',
    `max_concurrent` INT NOT NULL DEFAULT 1 COMMENT '同时加工 Lot 数上限',
    `e10_state` VARCHAR(24) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'STANDBY' COMMENT 'Chamber 级 E10 状态(Port 一般沿用设备)',
    `status_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '细分状态码',
    `reason_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '原因码(停机类)',
    `status_source` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MANUAL' COMMENT '状态设置来源 MANUAL/TRACK/SYSTEM',
    `status_since` DATETIME NULL COMMENT '当前状态起始时间',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_eqp_port`(`tenant_id` ASC,`equipment_id` ASC,`port_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '设备 Port/Chamber';

-- mes_equipment_capability 设备能力与资格
CREATE TABLE IF NOT EXISTS `mes_equipment_capability` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `equipment_id` BIGINT NOT NULL COMMENT '设备',
    `step_id` BIGINT NOT NULL COMMENT '可加工站点',
    `recipe_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定 Recipe,0 表示该站点所有 Recipe',
    `product_id` BIGINT NOT NULL DEFAULT 0 COMMENT '限定产品,0 表示所有产品',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'VALID' COMMENT 'VALID/SUSPENDED/EXPIRED/REVOKED;SUSPENDED 即设备 Inhibit',
    `valid_from` DATETIME NULL COMMENT '资格生效起',
    `valid_to` DATETIME NULL COMMENT '资格生效止',
    `qualified_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '认证人',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_capability`(`tenant_id` ASC,`equipment_id` ASC,`step_id` ASC,`recipe_id` ASC,`product_id` ASC) USING BTREE,
    INDEX `idx_step`(`tenant_id` ASC,`step_id` ASC) USING BTREE,
    INDEX `idx_status_valid`(`tenant_id` ASC,`status` ASC,`valid_to` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '设备能力与资格';

-- mes_bom BOM 主数据
CREATE TABLE IF NOT EXISTS `mes_bom` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `bom_code` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BOM 编码',
    `bom_name` VARCHAR(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'BOM 名称',
    `description` VARCHAR(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '说明',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '启用状态 0-禁用 1-启用',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_bom_code`(`tenant_id` ASC,`bom_code` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'BOM 主数据';

-- mes_bom_version BOM 版本
CREATE TABLE IF NOT EXISTS `mes_bom_version` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `bom_id` BIGINT NOT NULL COMMENT 'BOM',
    `version_no` INT NOT NULL COMMENT '版本号',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/ACTIVE/INACTIVE',
    `copy_from_version_id` BIGINT NULL COMMENT '复制来源版本',
    `effective_at` DATETIME NULL COMMENT '生效时间',
    `expire_at` DATETIME NULL COMMENT '失效时间',
    `submitted_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '提交人',
    `approved_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批人',
    `approved_at` DATETIME NULL COMMENT '审批时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_bom_version`(`tenant_id` ASC,`bom_id` ASC,`version_no` ASC) USING BTREE,
    INDEX `idx_bom_status`(`tenant_id` ASC,`bom_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'BOM 版本';

-- mes_bom_item BOM 行
CREATE TABLE IF NOT EXISTS `mes_bom_item` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `bom_version_id` BIGINT NOT NULL COMMENT 'BOM 版本',
    `line_no` INT NOT NULL COMMENT '行号',
    `material_id` BIGINT NOT NULL COMMENT '物料',
    `qty_per` DECIMAL(18,6) NOT NULL COMMENT '单位用量',
    `uom_id` BIGINT NOT NULL COMMENT '用量单位',
    `scrap_rate` DECIMAL(8,4) NOT NULL DEFAULT 0 COMMENT '允许损耗率(%),仅用于需求展开与差异判定,不参与实际扣减',
    `consume_mode` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PER_WAFER' COMMENT 'PER_WAFER/PER_DIE/PER_STRIP/PER_UNIT/PER_LOT/PER_RUN/PER_TIME',
    `step_id` BIGINT NOT NULL COMMENT '消耗站点',
    `route_seq_no` INT NOT NULL DEFAULT 0 COMMENT '指定主流程步骤序号,0 表示该站点每次经过都消耗',
    `backflush` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Track-Out 自动反冲',
    `is_critical` TINYINT(1) NULL COMMENT '覆盖物料的关键料标识,空表示沿用物料设置',
    `substitute_group` VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '替代组编码,同组互为替代',
    `substitute_priority` INT NULL COMMENT '组内优先级,数小优先',
    `substitute_policy` VARCHAR(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALLOW' COMMENT '使用非首选替代的策略 ALLOW/WARN/BLOCK',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_version_line`(`tenant_id` ASC,`bom_version_id` ASC,`line_no` ASC) USING BTREE,
    INDEX `idx_step`(`tenant_id` ASC,`step_id` ASC) USING BTREE,
    INDEX `idx_material`(`tenant_id` ASC,`material_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = 'BOM 行';

-- mes_product_version 产品版本
CREATE TABLE IF NOT EXISTS `mes_product_version` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `product_id` BIGINT NOT NULL COMMENT '产品',
    `version_no` INT NOT NULL COMMENT '版本号',
    `status` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/ACTIVE/INACTIVE',
    `route_id` BIGINT NULL COMMENT '绑定 Route(主数据);下工单时解析当前 ACTIVE 版本并固化到工单',
    `bom_id` BIGINT NULL COMMENT '绑定 BOM(主数据);下工单时解析当前 ACTIVE 版本并固化到工单',
    `copy_from_version_id` BIGINT NULL COMMENT '复制来源版本',
    `effective_at` DATETIME NULL COMMENT '生效时间',
    `expire_at` DATETIME NULL COMMENT '失效时间',
    `submitted_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '提交人',
    `approved_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批人',
    `approved_at` DATETIME NULL COMMENT '审批时间',
    `remark` VARCHAR(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '备注',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '创建人',
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '更新人',
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
    UNIQUE INDEX `uk_product_version`(`tenant_id` ASC,`product_id` ASC,`version_no` ASC) USING BTREE,
    INDEX `idx_product_status`(`tenant_id` ASC,`product_id` ASC,`status` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '产品版本';

-- 变更 mes_recipe_version
ALTER TABLE `mes_recipe_version`
    ADD COLUMN `submitted_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '提交人',
    ADD COLUMN `approved_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '审批人',
    ADD COLUMN `approved_at` DATETIME NULL COMMENT '审批时间';

-- 变更 mes_step
ALTER TABLE `mes_step`
    ADD COLUMN `step_group_id` BIGINT NULL COMMENT '所属站点组',
    ADD INDEX `idx_step_group`(`tenant_id` ASC,`step_group_id` ASC) USING BTREE;

-- 变更 mes_equipment
ALTER TABLE `mes_equipment`
    ADD COLUMN `max_concurrent_lots` INT NOT NULL DEFAULT 1 COMMENT '设备同时加工 Lot 数上限';
