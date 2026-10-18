-- 平台超管：跨租户切换 + 完整权限目录。
IF COL_LENGTH('sys_user', 'platform_admin') IS NULL
BEGIN
    ALTER TABLE sys_user ADD platform_admin BIT NOT NULL CONSTRAINT df_sys_user_platform_admin DEFAULT 0;
END;

UPDATE sys_user SET platform_admin = 1 WHERE id = 316239484125698;
