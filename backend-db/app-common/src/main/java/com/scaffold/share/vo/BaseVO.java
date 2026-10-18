package com.scaffold.share.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * BaseVO视图对象
 * <p>
 * 负责承载BaseVO相关展示与返回数据
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
public abstract class BaseVO {

    /**
     * 主键ID（雪花算法）
     */
    private String id;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 创建人员
     */
    private String createBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;

    /**
     * 修改人员
     */
    private String updateBy;

    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateAt;
}
