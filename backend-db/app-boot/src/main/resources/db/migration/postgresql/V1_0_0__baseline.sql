-- PostgreSQL Baseline Schema
-- Simplified conversion from MySQL (production needs full adaptation)

CREATE TABLE IF NOT EXISTS hist_operation_log (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64),
    user_name VARCHAR(64),
    operation VARCHAR(128) NOT NULL,
    class_name VARCHAR(128) NOT NULL,
    method_name VARCHAR(128) NOT NULL,
    request_uri VARCHAR(512),
    request_method VARCHAR(16),
    request_ip VARCHAR(64),
    user_agent VARCHAR(512),
    request_payload TEXT,
    response_payload TEXT,
    success BOOLEAN NOT NULL,
    duration_ms BIGINT NOT NULL,
    error_type VARCHAR(128),
    error_message VARCHAR(1024),
    create_by VARCHAR(64),
    create_at TIMESTAMP NOT NULL,
    update_by VARCHAR(64),
    update_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL UNIQUE,
    tenant_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_by VARCHAR(64),
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(64) NOT NULL,
    display_name VARCHAR(128),
    password_hash VARCHAR(256) NOT NULL,
    salt VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    create_by VARCHAR(64),
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(tenant_id, user_name)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_by VARCHAR(64),
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(tenant_id, role_code)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    permission_name VARCHAR(100) NOT NULL,
    permission_description VARCHAR(200),
    type SMALLINT NOT NULL DEFAULT 1,
    path VARCHAR(200),
    path_name VARCHAR(100),
    meta JSONB,
    component VARCHAR(200),
    permission_order INT,
    visible BOOLEAN NOT NULL DEFAULT TRUE,
    parent_id BIGINT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_by VARCHAR(64),
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    UNIQUE(tenant_id, permission_code)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_by VARCHAR(64),
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    create_by VARCHAR(64),
    create_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_dict_type (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) DEFAULT '*',
    dict_type VARCHAR(64) NOT NULL,
    dict_name VARCHAR(128) NOT NULL,
    remark VARCHAR(256),
    enabled BOOLEAN DEFAULT TRUE,
    create_by VARCHAR(64),
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, dict_type)
);

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) DEFAULT '*',
    dict_type VARCHAR(64) NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_label VARCHAR(128) NOT NULL,
    attrs_json JSONB,
    sort_order INT DEFAULT 0,
    css_class VARCHAR(64),
    extra_json JSONB,
    enabled BOOLEAN DEFAULT TRUE,
    create_by VARCHAR(64),
    create_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64),
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, dict_type, item_code)
);

CREATE TABLE IF NOT EXISTS sys_system_config (
    id BIGINT NOT NULL PRIMARY KEY,
    config_code VARCHAR(60) NOT NULL,
    config_value TEXT NOT NULL,
    value_type VARCHAR(20) NOT NULL,
    group_code VARCHAR(100) NOT NULL,
    remark VARCHAR(200),
    state BOOLEAN NOT NULL DEFAULT TRUE,
    create_by VARCHAR(100),
    create_at TIMESTAMP(6),
    update_by VARCHAR(100),
    update_at TIMESTAMP(6),
    tenant_id VARCHAR(64),
    deleted_at TIMESTAMP,
    UNIQUE(config_code, tenant_id)
);
