package com.scaffold.mes.base.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.share.integration.mes.base.provider.ProductSerialPort;

import lombok.RequiredArgsConstructor;

/**
 * ProductSerialPortImpl 产品序列号开关
 * <p>
 * track_serial 为 1 时允许写入 Unit 序列号。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class ProductSerialPortImpl implements ProductSerialPort {

    private final ProductRepository productRepository;

    @Override
    public boolean enabled(Long productId) {
        if (productId == null) {
            return false;
        }
        return productRepository.findById(productId)
                .map(product -> product.getTrackSerial() != null && product.getTrackSerial() == 1)
                .orElse(false);
    }
}
