package com.scaffold.share.integration.mes.base.provider;

import com.scaffold.share.integration.mes.base.response.UnitConvertFacts;

/**
 * UnitConvertProvider 单位转换事实
 * <p>
 * 执行模块不能直接读产品和 Route。出站时用它判断当前步骤要不要换单位，以及系数和允许偏差。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UnitConvertProvider {

    /**
     * 读取当前步骤的换算。
     *
     * @param routeVersionId Route 版本
     * @param routeSeqNo 当前序号
     * @param productId 产品
     * @param fromUnitId 当前计数单位
     * @return 不是转换站点时 {@link UnitConvertFacts#none()}
     */
    UnitConvertFacts facts(Long routeVersionId, Integer routeSeqNo, Long productId, Long fromUnitId);
}
