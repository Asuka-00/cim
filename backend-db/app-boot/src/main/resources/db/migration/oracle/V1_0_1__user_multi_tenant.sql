-- 单用户多租户：成员资格表 + user_name 全局唯一。
-- 若同一未删除 user_name 已在多个租户各有一行，下面的 uk_user_name 会失败。
-- 预检查：
--   SELECT user_name, COUNT(*) AS cnt
--   FROM sys_user
--   WHERE deleted_at IS NULL
--   GROUP BY user_name
--   HAVING COUNT(*) > 1;

CREATE TABLE sys_user_tenant (
    id NUMBER(19) NOT NULL,
    tenant_id VARCHAR2(64) NOT NULL,
    user_id NUMBER(19) NOT NULL,
    is_default NUMBER(1) DEFAULT 0 NOT NULL,
    create_by VARCHAR2(64) NULL,
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_by VARCHAR2(64) NULL,
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT sys_user_tenant_uk_user_tenant UNIQUE (user_id, tenant_id),
    PRIMARY KEY (id)
);
CREATE INDEX sys_user_tenant_idx_tenant_user ON sys_user_tenant (tenant_id, user_id);

INSERT INTO sys_user_tenant (id, tenant_id, user_id, is_default, create_by, create_at, update_by, update_at)
SELECT id, tenant_id, id, 1, create_by, create_at, update_by, update_at
FROM sys_user
WHERE deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_tenant ut WHERE ut.user_id = sys_user.id AND ut.tenant_id = sys_user.tenant_id
  );

ALTER TABLE sys_user DROP CONSTRAINT sys_user_uk_tenant_user;

CREATE UNIQUE INDEX sys_user_uk_user_name ON sys_user (CASE WHEN deleted_at IS NULL THEN user_name END);
