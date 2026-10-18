package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.entity.RouteStep;
import com.scaffold.mes.base.domain.repository.ProductUnitConvRepository;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.share.integration.mes.base.provider.UnitConvertProvider;
import com.scaffold.share.integration.mes.base.response.UnitConvertFacts;

import lombok.RequiredArgsConstructor;

/**
 * UnitConvertProviderImpl 单位转换事实
 * <p>
 * 当前 Route 步骤标记了单位转换时，按产品和源单位、目标单位读取换算系数。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class UnitConvertProviderImpl implements UnitConvertProvider {

    private final RouteStepRepository routeStepRepository;
    private final ProductUnitConvRepository conversionRepository;

    @Override
    public UnitConvertFacts facts(Long routeVersionId, Integer routeSeqNo, Long productId, Long fromUnitId) {
        if (routeVersionId == null || routeSeqNo == null) {
            return UnitConvertFacts.none();
        }
        RouteStep step = routeStepRepository.listByVersion(routeVersionId).stream()
                .filter(row -> routeSeqNo.equals(row.getSeqNo()))
                .findFirst()
                .orElse(null);
        if (step == null || step.getIsUnitConvert() == null || step.getIsUnitConvert() != 1) {
            return UnitConvertFacts.none();
        }
        Long toUnitId = step.getConvertToUnitId();
        var conversion = productId == null || fromUnitId == null || toUnitId == null
                ? null
                : conversionRepository.findExact(productId, fromUnitId, toUnitId).orElse(null);
        if (conversion == null) {
            return new UnitConvertFacts(true, toUnitId, null, null);
        }
        return new UnitConvertFacts(true, toUnitId, conversion.getFactor(), conversion.getTolerancePct());
    }
}
