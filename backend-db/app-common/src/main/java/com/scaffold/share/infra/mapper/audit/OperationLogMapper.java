package com.scaffold.share.infra.mapper.audit;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scaffold.share.audit.OperationLog;

/**
 * OperationLogMapper 操作日志数据库映射
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-08
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
