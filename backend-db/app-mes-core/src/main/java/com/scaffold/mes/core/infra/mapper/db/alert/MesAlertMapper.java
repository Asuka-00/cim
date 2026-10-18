package com.scaffold.mes.core.infra.mapper.db.alert;

import org.apache.ibatis.annotations.Mapper;

import com.scaffold.mes.core.domain.entity.alert.MesAlert;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * MesAlertMapper 预警 Mapper
 * <p>
 * 映射 mes_alert。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Mapper
public interface MesAlertMapper extends BatchBaseMapper<MesAlert> {
}
