package com.scaffold.mes.base.infra.repository.db;

import org.springframework.stereotype.Repository;

import com.scaffold.mes.base.domain.entity.ToolEvent;
import com.scaffold.mes.base.domain.repository.ToolEventRepository;
import com.scaffold.mes.base.infra.mapper.db.ToolEventMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * ToolEventDbRepositoryImpl 工具事件仓储实现
 * <p>
 * 只插入，不更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class ToolEventDbRepositoryImpl implements ToolEventRepository {

    private final ToolEventMapper mapper;

    @Override
    public void save(ToolEvent event) {
        new AppendOnlyRepositorySupport<>(mapper, ToolEvent.class).save(event);
    }
}
