-- 平台超管：跨租户切换 + 完整权限目录。
ALTER TABLE sys_user ADD platform_admin NUMBER(1) DEFAULT 0 NOT NULL;

UPDATE sys_user SET platform_admin = 1 WHERE id = 316239484125698;
