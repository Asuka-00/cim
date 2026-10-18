package com.scaffold.share.context;

/**
 * UserContextHolder
 * <p>
 * 为异步/非Web线程提供当前用户上下文（如定时任务线程）
 */
public final class UserContextHolder {

    private static final ThreadLocal<String> USER_CODE_HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    /**
     * 设置用户编码
     */
    public static void setUserName(String userName) {
        USER_CODE_HOLDER.set(userName);
    }

    /**
     * 获取用户编码
     */
    public static String getUserName() {
        return USER_CODE_HOLDER.get();
    }

    /**
     * 清理数据
     */
    public static void clear() {
        USER_CODE_HOLDER.remove();
    }
}
