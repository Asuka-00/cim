package com.scaffold.share.entity;

import java.time.LocalDateTime;

/**
 * BaseEntity 实体基础契约
 * <p>
 * 定义仓储基座所需的通用实体字段访问方法，避免普通实体与关联实体仓储重复实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-12
 */
public interface BaseEntity {

    /**
     * 获取实体主键
     */
    Long getId();

    /**
     * 设置实体主键
     */
    void setId(Long id);

    /**
     * 获取租户标识
     */
    String getTenantId();

    /**
     * 设置租户标识
     */
    void setTenantId(String tenantId);

    /**
     * 获取创建人
     */
    String getCreateBy();

    /**
     * 设置创建人
     */
    void setCreateBy(String createBy);

    /**
     * 获取创建时间
     */
    LocalDateTime getCreateAt();

    /**
     * 设置创建时间
     */
    void setCreateAt(LocalDateTime createAt);

    /**
     * 获取更新人
     */
    String getUpdateBy();

    /**
     * 设置更新人
     */
    void setUpdateBy(String updateBy);

    /**
     * 获取更新时间
     */
    LocalDateTime getUpdateAt();

    /**
     * 设置更新时间
     */
    void setUpdateAt(LocalDateTime updateAt);
}
