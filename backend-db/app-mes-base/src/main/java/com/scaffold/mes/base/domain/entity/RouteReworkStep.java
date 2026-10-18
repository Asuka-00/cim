package com.scaffold.mes.base.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RouteReworkStep 返工步骤
 * <p>
 * 对应 mes_route_rework_step。序号在分支内唯一。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_route_rework_step")
public class RouteReworkStep extends NonSoftDeletableEntity {

    /** 返工分支。 */
    private Long reworkId;

    /** 返工内序号。 */
    private Integer seqNo;

    /** 站点。 */
    private Long stepId;
}
