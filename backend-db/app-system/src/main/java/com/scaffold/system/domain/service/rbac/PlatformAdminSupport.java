package com.scaffold.system.domain.service.rbac;

import com.scaffold.share.constants.ErrorCodeConstants;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.system.domain.entity.rbac.User;

/**
 * 平台超管判定。
 */
final class PlatformAdminSupport {

    private PlatformAdminSupport() {
    }

    static boolean isPlatformAdmin(User user) {
        return user != null && Boolean.TRUE.equals(user.getPlatformAdmin());
    }

    static void requirePlatformAdmin(User user) {
        if (!isPlatformAdmin(user)) {
            throw new BusinessException(ErrorCodeConstants.ACCESS_DENIED, "仅平台超管可执行此操作");
        }
    }
}
