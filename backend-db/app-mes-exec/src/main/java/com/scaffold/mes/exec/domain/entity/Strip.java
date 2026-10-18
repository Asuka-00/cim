package com.scaffold.mes.exec.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Strip 基板条
 * <p>
 * 对应 mes_strip。一条 Strip 只属于一个 Lot。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_strip")
public class Strip extends NonSoftDeletableEntity {

    /** Strip 编号。 */
    private String stripCode;

    /** 所属 Lot。 */
    private Long lotId;

    /** NORMAL、SCRAPPED 或 CONVERTED。 */
    private String status;

    /** 含 Unit 数。 */
    private Integer unitQty;

    /** 良品数。 */
    private Integer goodQty;

    /** 所在 Magazine。 */
    private Long magazineCarrierId;

    /** 乐观锁版本号。 */
    @Version
    private Integer version;
}
