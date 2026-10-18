package com.scaffold.mes.core.infra.mapper.db.print;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.print.PrintTemplate;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * PrintTemplateMapper 打印模板 Mapper
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface PrintTemplateMapper extends BatchBaseMapper<PrintTemplate> {
}
