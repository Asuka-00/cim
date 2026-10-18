package com.scaffold.share.integration.mes.base.provider;

/**
 * ToolLifePort 工具寿命
 * <p>
 * 执行模块不能直接读工具台账。没有上限时允许进站。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ToolLifePort {

    /**
     * 使用量是否还没到寿命上限。
     *
     * @param toolId 工具
     * @return 工具存在，且未达上限时为 true
     */
    boolean withinLife(Long toolId);

    /**
     * 使用量加 1。达到上限时改为保养。
     *
     * @param toolId 工具
     */
    void use(Long toolId);

    /**
     * 使用量减 1，并写冲销记录。
     *
     * @param toolId 工具
     */
    void reverse(Long toolId);
}
