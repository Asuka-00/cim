package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.service.ProductVersionApplicationService;
import com.scaffold.share.integration.mes.base.provider.ActiveProcessProvider;
import com.scaffold.share.integration.mes.base.response.FrozenProcess;

import lombok.RequiredArgsConstructor;

/**
 * ActiveProcessProviderImpl 生效工艺查询实现
 * <p>
 * 把产品版本上的 Route、BOM 主数据解析成当时的 ACTIVE 版本。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class ActiveProcessProviderImpl implements ActiveProcessProvider {

    private final ProductVersionApplicationService productVersionService;

    @Override
    public FrozenProcess freeze(Long productVersionId) {
        return productVersionService.freeze(productVersionId);
    }
}
