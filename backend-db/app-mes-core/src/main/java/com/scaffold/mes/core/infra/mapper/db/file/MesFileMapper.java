package com.scaffold.mes.core.infra.mapper.db.file;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.file.MesFile;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * MesFileMapper 文件元数据 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface MesFileMapper extends BatchBaseMapper<MesFile> {
}
