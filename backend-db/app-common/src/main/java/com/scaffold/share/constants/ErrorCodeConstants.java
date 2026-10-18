package com.scaffold.share.constants;

/**
 * ErrorCodeConstants类
 * <p>
 * 负责处理ErrorCodeConstants相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
public final class ErrorCodeConstants {

    private ErrorCodeConstants() {
    }

    /**
     * 系统错误
     */
    public static final int SYSTEM_ERROR = 100000;
    public static final int INVALID_PARAM = 100001;

    // ==================== 系统级错误码 100002-100099 ====================
    // 请求格式错误（JSON解析失败、未知字段等）
    public static final int INVALID_REQUEST = 100002;
    // 缺少必需参数
    public static final int MISSING_PARAM = 100003;
    // 参数类型不匹配
    public static final int PARAM_TYPE_MISMATCH = 100004;
    // 约束校验失败
    public static final int CONSTRAINT_VIOLATION = 100005;
    // 请求绑定失败
    public static final int BINDING_ERROR = 100006;
    // HTTP方法不支持
    public static final int METHOD_NOT_SUPPORTED = 100007;
    // 参数校验失败
    public static final int PARAM_VALIDATION_ERROR = 100008;
    // 未认证或登录已失效
    public static final int AUTHENTICATION_REQUIRED = 100009;
    // 已认证但无访问权限
    public static final int ACCESS_DENIED = 100010;
    // 发布包格式非法
    public static final int RELEASE_PACKAGE_INVALID = 100020;
    // 发布包校验和不匹配
    public static final int RELEASE_CHECKSUM_MISMATCH = 100021;
    // 版本号格式非法（需 MAJOR.MINOR.PATCH）
    public static final int RELEASE_VERSION_INVALID = 100022;
    // 禁止覆盖当前运行版本
    public static final int RELEASE_CURRENT_OVERWRITE_FORBIDDEN = 100023;
    // 版本不存在
    public static final int RELEASE_VERSION_NOT_FOUND = 100024;
    // 无 previous 可回滚
    public static final int RELEASE_PREVIOUS_NOT_FOUND = 100025;
    // 发布目录不可写或不可用
    public static final int RELEASE_HOME_UNAVAILABLE = 100026;
    // 版本激活冲突（并发）
    public static final int RELEASE_ACTIVATE_CONFLICT = 100027;
    // 目标版本已是当前版本
    public static final int RELEASE_ALREADY_CURRENT = 100028;
    // 未知系统异常（兜底）
    public static final int UNKNOWN_ERROR = 100099;

    // ==================== Auth相关错误码 200000-209999 ====================

    // 用户不存在
    public static final int USER_NOT_FOUND = 200001;
    // 用户被禁用
    public static final int USER_DISABLED = 200002;
    // 密码错误
    public static final int USER_PASSWORD_INCORRECT = 200003;
    // 用户已存在
    public static final int USER_ALREADY_EXISTS = 200004;
    // 旧密码错误
    public static final int USER_OLD_PASSWORD_INCORRECT = 200005;
    // 角色不存在
    public static final int ROLE_NOT_FOUND = 200006;
    // 角色编码已存在
    public static final int ROLE_CODE_EXISTS = 200007;
    // 权限不存在
    public static final int PERMISSION_NOT_FOUND = 200008;
    // 权限编码已存在
    public static final int PERMISSION_CODE_EXISTS = 200009;
    // Token类型错误
    public static final int TOKEN_TYPE_INVALID = 200010;
    // Token在黑名单
    public static final int TOKEN_BLACKLISTED = 200011;
    // Token不是当前有效会话
    public static final int TOKEN_SESSION_INVALID = 200012;
    // Token无效或过期
    public static final int TOKEN_INVALID_OR_EXPIRED = 200013;
    // 会话不存在
    public static final int SESSION_NOT_FOUND = 200014;
    // 租户不存在
    public static final int TENANT_NOT_FOUND = 200015;
    // 租户编码已存在
    public static final int TENANT_CODE_EXISTS = 200016;
    // 权限已被角色绑定
    public static final int PERMISSION_HAS_BEEN_BOUND = 200017;
    // 角色已被用户绑定
    public static final int ROLE_HAS_BEEN_BOUND = 200018;
    // 租户已被业务数据使用
    public static final int TENANT_HAS_BEEN_BOUND = 200019;
    // OAuth2 未启用或未配置
    public static final int OAUTH2_NOT_ENABLED = 200020;
    // OAuth2 登录票据无效或已过期
    public static final int OAUTH2_TICKET_INVALID = 200021;
    // OAuth2 身份与本地用户无法映射
    public static final int OAUTH2_USER_MAPPING_FAILED = 200022;
    // OAuth2 无法连接 IdP（Discovery / endpoint 不可达或配置错误）
    public static final int OAUTH2_IDP_UNAVAILABLE = 200023;
    // 用户不属于指定租户
    public static final int USER_NOT_IN_TENANT = 200024;

    // ==================== Config相关错误码 300000-309999 ====================

    // 系统配置不存在
    public static final int SYSTEM_CONFIG_NOT_FOUND = 300001;
    // 系统配置编码已存在
    public static final int SYSTEM_CONFIG_CODE_EXISTS = 300002;
    public static final int DICT_TYPE_NOT_FOUND = 300003;
    public static final int DICT_TYPE_EXISTS = 300004;
    public static final int DICT_ITEM_NOT_FOUND = 300005;
    public static final int DICT_ITEM_EXISTS = 300006;
    public static final int DICT_TYPE_HAS_BEEN_BOUND = 300007;
}
