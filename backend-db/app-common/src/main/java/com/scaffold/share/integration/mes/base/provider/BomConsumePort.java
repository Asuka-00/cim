package com.scaffold.share.integration.mes.base.provider;

import java.util.List;

import com.scaffold.share.integration.mes.base.response.BomConsumeLine;

/**
 * BomConsumePort 出站反冲的 BOM 行
 * <p>
 * 执行模块不能直接读 BOM。没有匹配行时返回空列表。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface BomConsumePort {

    /**
     * 列出这个站点上要按批扣的行。
     *
     * @param bomVersionId BOM 版本
     * @param stepId 当前站点
     * @return PER_LOT 反冲行
     */
    List<BomConsumeLine> perLot(Long bomVersionId, Long stepId);

    /**
     * 列出这个站点上要按片扣的行。
     *
     * @param bomVersionId BOM 版本
     * @param stepId 当前站点
     * @return PER_WAFER 反冲行，数量是每片用量
     */
    List<BomConsumeLine> perWafer(Long bomVersionId, Long stepId);

    /**
     * 列出这个站点上要按 die 扣的行。
     *
     * @param bomVersionId BOM 版本
     * @param stepId 当前站点
     * @return PER_DIE 反冲行，数量是每颗用量
     */
    List<BomConsumeLine> perDie(Long bomVersionId, Long stepId);

    /**
     * 列出这个站点上要按条扣的行。
     *
     * @param bomVersionId BOM 版本
     * @param stepId 当前站点
     * @return PER_STRIP 反冲行，数量是每条用量
     */
    List<BomConsumeLine> perStrip(Long bomVersionId, Long stepId);

    /**
     * 列出这个站点上要按加工时间扣的行。
     *
     * @param bomVersionId BOM 版本
     * @param stepId 当前站点
     * @return PER_TIME 反冲行，数量是每分钟用量
     */
    List<BomConsumeLine> perTime(Long bomVersionId, Long stepId);

    /**
     * 列出这个站点上按一次加工扣的行。
     *
     * @param bomVersionId BOM 版本
     * @param stepId 当前站点
     * @return PER_RUN 反冲行，一批次只扣一次
     */
    List<BomConsumeLine> perRun(Long bomVersionId, Long stepId);
}
