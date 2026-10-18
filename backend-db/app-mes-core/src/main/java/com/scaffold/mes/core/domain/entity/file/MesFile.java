package com.scaffold.mes.core.domain.entity.file;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MesFile 文件元数据
 * <p>
 * 对应 mes_file。本阶段只登记挂在 Lot 上的文件，不保存文件本体。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_file")
public class MesFile extends NonSoftDeletableEntity {

    /** 原始文件名。 */
    private String fileName;

    /** MIME 类型。 */
    private String contentType;

    /** 文件大小，字节。 */
    private Long sizeBytes;

    /** 存储相对路径。 */
    private String storagePath;

    /** 内容摘要。 */
    private String sha256;

    /** LOT。 */
    private String bizType;

    /** Lot。 */
    private Long bizId;
}
