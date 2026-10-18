package com.scaffold.mes.exec.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Lot 在制批
 * <p>
 * 对应 mes_lot。开批时停在 Route 第一步，状态为 WAITING。工艺版本沿用工单固化值，不跟随后续升版。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_lot")
public class Lot extends NonSoftDeletableEntity {

    /** Lot 号，租户内唯一。 */
    private String lotNo;

    /** 类型。 */
    private String lotType;

    /** 工单。Monitor、Dummy 可以没有。 */
    private Long woId;

    /** 当前批次。空表示不在批内，不能单独进站。 */
    private Long batchId;

    /** 拆批产生的父 Lot。 */
    private Long parentLotId;

    /** 根 Lot。开批时就是自己，拆出去的子 Lot 沿用父 Lot 的根。 */
    private Long rootLotId;

    /** 在父 Lot 下的拆批序号。子 Lot 号用它拼在父 Lot 号后面。 */
    private Integer splitSeq;

    /** 所在返工分支。空表示主流程。 */
    private Long reworkId;

    /** 返工分支内步骤序号。 */
    private Integer reworkSeqNo;

    /** 产品。 */
    private Long productId;

    /** 产品版本。 */
    private Long productVersionId;

    /** Route 版本。 */
    private Long routeVersionId;

    /** BOM 版本。 */
    private Long bomVersionId;

    /** 当前步骤序号。 */
    private Integer routeSeqNo;

    /** 当前站点。 */
    private Long stepId;

    /** 当前站点第几次经过。开批时为 1。 */
    private Integer passNo;

    /** 状态。 */
    private String status;

    /** 未释放的 Hold 数。 */
    private Integer holdCount;

    /** 当前数量。 */
    private Long qty;

    /** 当前计数单位。 */
    private Long qtyUnitId;

    /** 副数量。主单位为 Strip 时，这里是 Unit 总数。 */
    private Long subQty;

    /** 开批数量。余量按这个数汇总。 */
    private Long initialQty;

    /** 累计报废数量。 */
    private Long scrapQty;

    /** 当前单位下的累计报废。 */
    private Long currentScrapQty;

    /** 存活 Wafer 数。 */
    private Integer waferCount;

    /** 优先级。 */
    private Integer priority;

    /** 1 表示 Hot Lot。 */
    private Integer isHot;

    /** 当前加工设备。Track-Out 时清空。 */
    private Long equipmentId;

    /** 当前 Port 或腔体。 */
    private Long portId;

    /** 当前 Recipe 版本。 */
    private Long recipeVersionId;

    /** 当前站点 Track-In 时间。 */
    private LocalDateTime trackInAt;

    /** Track-In 操作人。 */
    private String trackInBy;

    /** 载具编码。展示用，事实来源是载具上的当前 Lot。 */
    private String carrierCode;

    /** 位置类型。进站后为 EQUIPMENT，出站后为 AREA。 */
    private String locationType;

    /** 位置对象。出站后为下一站点所在区域。 */
    private Long locationId;

    /** 最近出站时间。 */
    private LocalDateTime lastTrackOutAt;

    /** 完成时间。 */
    private LocalDateTime finishedAt;

    /** 进入当前站点的时间。 */
    private LocalDateTime stepEnterAt;

    /** 开批时间。 */
    private LocalDateTime startedAt;

    /** 备注。 */
    private String remark;

    /** 自定义属性值，JSON 对象。 */
    private String extJson;

    /** IMPORT 表示期初导入。开批时为空。 */
    private String source;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
