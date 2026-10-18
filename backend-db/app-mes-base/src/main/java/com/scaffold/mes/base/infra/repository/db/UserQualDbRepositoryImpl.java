package com.scaffold.mes.base.infra.repository.db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.base.domain.entity.UserQual;
import com.scaffold.mes.base.domain.repository.UserQualRepository;
import com.scaffold.mes.base.infra.mapper.db.UserQualMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * UserQualDbRepositoryImpl 人员资质仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class UserQualDbRepositoryImpl implements UserQualRepository {

    private final UserQualMapper mapper;

    private DbRepositorySupport<UserQual> support() {
        return new DbRepositorySupport<>(mapper, UserQual.class);
    }

    @Override
    public Optional<UserQual> find(String userName, String targetType, Long targetId) {
        return support().findOne(new LambdaQueryWrapper<UserQual>()
                .eq(UserQual::getUserName, userName)
                .eq(UserQual::getTargetType, targetType)
                .eq(UserQual::getTargetId, targetId));
    }

    @Override
    public List<UserQual> listDue(LocalDateTime now) {
        return mapper.selectList(new LambdaQueryWrapper<UserQual>()
                .eq(UserQual::getStatus, "VALID")
                .isNotNull(UserQual::getValidTo)
                .lt(UserQual::getValidTo, now));
    }

    @Override
    public List<UserQual> list() {
        return mapper.selectList(new LambdaQueryWrapper<UserQual>().orderByAsc(UserQual::getUserName));
    }

    @Override
    public void save(UserQual qual) {
        support().save(qual);
    }

    @Override
    public void update(UserQual qual) {
        support().update(qual);
    }
}
