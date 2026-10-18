package com.scaffold.share.integration.system;

/**
 * DictItemLookup 字典项是否存在且启用
 * <p>
 * 由系统模块实现。MES 用它校验报废原因码，不直接读字典表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface DictItemLookup {

    /**
     * 字典项是否可用。
     *
     * @param dictType 字典类型
     * @param itemCode 选项编码
     * @return 存在且启用时为 true
     */
    boolean enabled(String dictType, String itemCode);
}
