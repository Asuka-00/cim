package com.scaffold.share.integration.mes.base.provider;

import java.time.LocalDateTime;

/**
 * UserQualPort 人员资质
 * <p>
 * 执行模块不能直接读资质表。没有记录时允许。到期或无效时拒绝。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UserQualPort {

    /**
     * 当前是否允许操作该目标。
     *
     * @param userName 用户名
     * @param targetType EQUIPMENT、EQUIPMENT_GROUP 或 STEP
     * @param targetId 目标
     * @param now 当前时间
     * @return 允许时为 true
     */
    boolean allowed(String userName, String targetType, Long targetId, LocalDateTime now);
}
