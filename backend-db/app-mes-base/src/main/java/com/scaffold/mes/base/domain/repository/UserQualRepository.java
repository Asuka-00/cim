package com.scaffold.mes.base.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.scaffold.mes.base.domain.entity.UserQual;

/**
 * UserQualRepository 人员资质仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UserQualRepository {

    /**
     * 按用户和目标查询。
     *
     * @param userName 用户名
     * @param targetType 目标类型
     * @param targetId 目标
     * @return 资质，不存在时为空
     */
    Optional<UserQual> find(String userName, String targetType, Long targetId);

    /**
     * 列出仍标为有效、但生效止已过的资质。
     *
     * @param now 当前时间
     * @return 待过期记录
     */
    List<UserQual> listDue(LocalDateTime now);

    /**
     * 列出全部资质，按用户名。
     *
     * @return 资质
     */
    List<UserQual> list();

    /**
     * 插入。
     *
     * @param qual 资质
     */
    void save(UserQual qual);

    /**
     * 更新。到期时改为 EXPIRED。
     *
     * @param qual 已加载的资质
     */
    void update(UserQual qual);
}
