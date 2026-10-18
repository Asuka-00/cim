-- 单用户多租户：成员资格表 + user_name 全局唯一。
-- 若同一未删除 user_name 已在多个租户各有一行，下面的 uk_user_name 会失败。
-- 预检查：
--   SELECT user_name, COUNT(*) AS cnt
--   FROM sys_user
--   WHERE deleted_at IS NULL
--   GROUP BY user_name
--   HAVING COUNT(*) > 1;
-- 请先手工合并这些账号，不要自动合并（密码/角色可能不是同一个人）。

CREATE TABLE IF NOT EXISTS `sys_user_tenant` (
    `id` BIGINT NOT NULL COMMENT '主键ID',
    `tenant_id` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认租户 0-否 1-是',
    `create_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `create_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_by` VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    `update_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP(0),
    UNIQUE INDEX `uk_user_tenant`(`user_id` ASC, `tenant_id` ASC) USING BTREE,
    INDEX `idx_tenant_user`(`tenant_id` ASC, `user_id` ASC) USING BTREE,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic COMMENT = '用户-租户成员资格';

INSERT INTO `sys_user_tenant` (`id`, `tenant_id`, `user_id`, `is_default`, `create_by`, `create_at`, `update_by`, `update_at`)
SELECT `id`, `tenant_id`, `id`, 1, `create_by`, `create_at`, `update_by`, `update_at`
FROM `sys_user`
WHERE `deleted_at` IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_tenant` ut WHERE ut.`user_id` = `sys_user`.`id` AND ut.`tenant_id` = `sys_user`.`tenant_id`
  );

ALTER TABLE `sys_user` DROP INDEX `uk_tenant_user`;

CREATE UNIQUE INDEX `uk_user_name` ON `sys_user` ((CASE WHEN `deleted_at` IS NULL THEN `user_name` ELSE NULL END));
