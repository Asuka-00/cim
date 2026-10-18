package com.scaffold.share.integration.mes.base.provider;

/**
 * UserScopePort 用户数据权限
 * <p>
 * 执行模块不能直接读范围表。某个类型没有记录时不限制。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UserScopePort {

    /**
     * 用户能否操作这个对象。
     *
     * @param userName 用户名
     * @param scopeType AREA、STEP 或 EQUIPMENT_GROUP
     * @param scopeId 对象
     * @return 没有该类型记录，或对象在名单内时为 true
     */
    boolean allowed(String userName, String scopeType, Long scopeId);
}
