-- SQL Server Baseline Schema (简化版，生产环境需完整适配)
-- 注意：SQL Server 使用 IDENTITY 自增，NVARCHAR 支持 Unicode

-- hist_operation_log
CREATE TABLE hist_operation_log (
    id BIGINT NOT NULL PRIMARY KEY,
    tenant_id NVARCHAR(64) NOT NULL,
    trace_id NVARCHAR(64),
    user_name NVARCHAR(64),
    operation NVARCHAR(128) NOT NULL,
    class_name NVARCHAR(128) NOT NULL,
    method_name NVARCHAR(128) NOT NULL,
    request_uri NVARCHAR(512),
    request_method NVARCHAR(16),
    request_ip NVARCHAR(64),
    user_agent NVARCHAR(512),
    request_payload NVARCHAR(MAX),
    response_payload NVARCHAR(MAX),
    success BIT NOT NULL,
    duration_ms BIGINT NOT NULL,
    error_type NVARCHAR(128),
    error_message NVARCHAR(1024),
    create_by NVARCHAR(64),
    create_at DATETIME2 NOT NULL,
    update_by NVARCHAR(64),
    update_at DATETIME2 NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 其他表省略，实际使用时需完整转换
-- SQL Server 特性：NVARCHAR、BIT、DATETIME2、NVARCHAR(MAX)
-- 自增主键：IDENTITY(1,1)
