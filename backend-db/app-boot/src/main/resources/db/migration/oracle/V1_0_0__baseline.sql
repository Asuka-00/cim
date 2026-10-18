-- Oracle Baseline Schema (简化版，生产环境需完整适配)
-- 注意：Oracle 不支持 AUTO_INCREMENT，需使用 SEQUENCE + TRIGGER

-- hist_operation_log
CREATE TABLE hist_operation_log (
    id NUMBER(19) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR2(64) NOT NULL,
    trace_id VARCHAR2(64),
    user_name VARCHAR2(64),
    operation VARCHAR2(128) NOT NULL,
    class_name VARCHAR2(128) NOT NULL,
    method_name VARCHAR2(128) NOT NULL,
    request_uri VARCHAR2(512),
    request_method VARCHAR2(16),
    request_ip VARCHAR2(64),
    user_agent VARCHAR2(512),
    request_payload CLOB,
    response_payload CLOB,
    success NUMBER(1) NOT NULL,
    duration_ms NUMBER(19) NOT NULL,
    error_type VARCHAR2(128),
    error_message VARCHAR2(1024),
    create_by VARCHAR2(64),
    create_at TIMESTAMP NOT NULL,
    update_by VARCHAR2(64),
    update_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 其他表省略，实际使用时需完整转换
-- Oracle 特性：VARCHAR2、NUMBER、CLOB、TIMESTAMP
-- 自增主键需要 SEQUENCE + TRIGGER 组合
