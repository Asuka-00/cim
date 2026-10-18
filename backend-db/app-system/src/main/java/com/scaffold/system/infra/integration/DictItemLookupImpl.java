package com.scaffold.system.infra.integration;

import org.springframework.stereotype.Component;

import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.integration.system.DictItemLookup;
import com.scaffold.system.domain.repository.system.DictItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * DictItemLookupImpl 字典项查询
 * <p>
 * 停用的选项视为不存在。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Component
@RequiredArgsConstructor
public class DictItemLookupImpl implements DictItemLookup {

    private final DictItemRepository repository;

    @Override
    public boolean enabled(String dictType, String itemCode) {
        return repository.findByDictTypeAndItemCode(dictType, itemCode)
                .filter(item -> item.getEnabled() == StateEnum.ENABLED)
                .isPresent();
    }
}
