package com.scaffold.mes.base.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.UserQualCreateRequest;
import com.scaffold.mes.base.domain.entity.UserQual;
import com.scaffold.mes.base.domain.repository.UserQualRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * UserQualApplicationService 人员资质
 * <p>
 * 没有记录表示不限制。有记录时，到期或不是 VALID 都视为无效。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class UserQualApplicationService {

    private static final Set<String> TYPES = Set.of("EQUIPMENT", "EQUIPMENT_GROUP", "STEP");

    private final UserQualRepository repository;

    /**
     * 列出全部资质。
     *
     * @return 按用户名排列
     */
    public List<UserQual> list() {
        return repository.list();
    }

    /**
     * 新建有效资质。
     *
     * @param request 用户、目标和生效止
     * @throws BusinessException 类型不合法或同一目标已有记录
     */
    @Transactional
    public void create(UserQualCreateRequest request) {
        if (!TYPES.contains(request.getTargetType())) {
            throw new BusinessException(MesBaseErrorCodes.USER_QUAL_INVALID, "资质目标类型不合法");
        }
        if (repository.find(request.getUserName(), request.getTargetType(), request.getTargetId()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.USER_QUAL_INVALID, "该用户在这个目标上已有资质");
        }
        UserQual qual = new UserQual();
        qual.setUserName(request.getUserName());
        qual.setTargetType(request.getTargetType());
        qual.setTargetId(request.getTargetId());
        qual.setStatus("VALID");
        qual.setValidTo(request.getValidTo());
        repository.save(qual);
    }

    /**
     * 当前是否允许操作该目标。没有资质记录时允许。
     *
     * @param userName 用户名
     * @param targetType 目标类型
     * @param targetId 目标
     * @param now 当前时间
     * @return 允许时为 true
     */
    public boolean allowed(String userName, String targetType, Long targetId, LocalDateTime now) {
        UserQual qual = repository.find(userName, targetType, targetId).orElse(null);
        if (qual == null) {
            return true;
        }
        if (!"VALID".equals(qual.getStatus())) {
            return false;
        }
        return qual.getValidTo() == null || !qual.getValidTo().isBefore(now);
    }

    /**
     * 把已过生效止的有效资质标成过期。
     *
     * @param now 当前时间
     * @return 改了几条
     */
    @Transactional
    public int expireDue(LocalDateTime now) {
        int count = 0;
        for (UserQual qual : repository.listDue(now)) {
            qual.setStatus("EXPIRED");
            repository.update(qual);
            count++;
        }
        return count;
    }
}
