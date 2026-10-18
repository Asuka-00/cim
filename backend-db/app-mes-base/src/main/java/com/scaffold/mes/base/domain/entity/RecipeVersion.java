package com.scaffold.mes.base.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * RecipeVersion Recipe 版本
 * <p>
 * 对应 mes_recipe_version。生效后内容不可改，同一 Recipe 同时只能有一个 ACTIVE。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_recipe_version")
public class RecipeVersion extends NonSoftDeletableEntity {

    /** 所属 Recipe。 */
    private Long recipeId;

    /** 版本号，从 1 递增。 */
    private Integer versionNo;

    /** 状态：DRAFT、PENDING、ACTIVE、INACTIVE。 */
    private String status;

    /** 参数说明。JSON 文本，只给人查阅和校验。 */
    private String paramDesc;

    /** 生效时间。 */
    private LocalDateTime effectiveAt;

    /** 失效时间。 */
    private LocalDateTime expireAt;

    /** 提交人。 */
    private String submittedBy;

    /** 最近审批人。 */
    private String approvedBy;

    /** 最近审批时间。 */
    private LocalDateTime approvedAt;

    /** 备注。 */
    private String remark;
}
