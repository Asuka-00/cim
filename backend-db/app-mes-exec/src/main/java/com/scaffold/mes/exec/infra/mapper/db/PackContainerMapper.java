package com.scaffold.mes.exec.infra.mapper.db;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.exec.domain.entity.PackContainer;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * PackContainerMapper 包装容器 Mapper
 * <p>
 * 映射 mes_pack_container。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface PackContainerMapper extends BatchBaseMapper<PackContainer> {
}
