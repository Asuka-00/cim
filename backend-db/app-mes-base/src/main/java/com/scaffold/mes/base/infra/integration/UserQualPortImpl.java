package com.scaffold.mes.base.infra.integration;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.service.UserQualApplicationService;
import com.scaffold.share.integration.mes.base.provider.UserQualPort;

import lombok.RequiredArgsConstructor;

/**
 * UserQualPortImpl 人员资质
 * <p>
 * 转给资质服务判断。没有记录时允许。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class UserQualPortImpl implements UserQualPort {

    private final UserQualApplicationService qualifications;

    @Override
    public boolean allowed(String userName, String targetType, Long targetId, LocalDateTime now) {
        return qualifications.allowed(userName, targetType, targetId, now);
    }
}
