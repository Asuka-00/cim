-- 平台超管：跨租户切换 + 完整权限目录。
ALTER TABLE `sys_user`
    ADD COLUMN `platform_admin` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '平台超管 0-否 1-是' AFTER `enabled`;

UPDATE `sys_user` SET `platform_admin` = 1 WHERE `id` = 316239484125698;
