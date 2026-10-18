package com.scaffold.share.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * SoftDeletableEntity 可软删除实体基类
 * <p>
 * 提供可软删除实体的公共字段（主键、删除标记、租户、审计字段），供配置类主数据表继承使用。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Getter
@Setter
@FieldNameConstants
public abstract class SoftDeletableEntity implements BaseEntity {

    /**
     * 主键ID（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 删除时间（NULL 表示未删除，有值表示已删除）
     */
    @TableLogic
    private LocalDateTime deletedAt;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private String tenantId;

    /**
     * 创建人员
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createAt;

    /**
     * 修改人员
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateAt;
}
