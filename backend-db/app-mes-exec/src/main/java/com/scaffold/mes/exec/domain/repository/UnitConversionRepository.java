package com.scaffold.mes.exec.domain.repository;

import com.scaffold.mes.exec.domain.entity.UnitConversion;

/**
 * UnitConversionRepository 单位转换历史仓储
 * <p>
 * 只追加 hist_mes_unit_conversion。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UnitConversionRepository {

    /**
     * 插入一条转换历史。
     *
     * @param conversion 历史
     */
    void save(UnitConversion conversion);
}
