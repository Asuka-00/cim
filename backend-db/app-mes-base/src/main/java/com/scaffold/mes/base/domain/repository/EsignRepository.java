package com.scaffold.mes.base.domain.repository;

import com.scaffold.mes.base.domain.entity.Esign;

/**
 * EsignRepository 电子签名仓储
 * <p>
 * 只追加 hist_mes_esign。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface EsignRepository {

    /**
     * 插入一条签名。
     *
     * @param esign 签名
     */
    void save(Esign esign);

    /**
     * 是否已有指定类型的签名。
     *
     * @param actionCode 动作
     * @param bizType 对象类型
     * @param bizId 对象
     * @param signType PRIMARY 或 SECOND
     * @return 已签名时为 true
     */
    boolean exists(String actionCode, String bizType, Long bizId, String signType);
}
