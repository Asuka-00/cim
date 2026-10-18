-- 平台超管：跨租户切换 + 完整权限目录。
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS platform_admin SMALLINT NOT NULL DEFAULT 0;

UPDATE sys_user SET platform_admin = 1 WHERE id = 316239484125698;
