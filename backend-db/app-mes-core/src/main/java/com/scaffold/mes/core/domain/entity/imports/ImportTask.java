package com.scaffold.mes.core.domain.entity.imports;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.NonSoftDeletableEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ImportTask 导入任务
 * <p>
 * 对应 mes_import_task。新建时状态固定为 DRAFT。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("mes_import_task")
public class ImportTask extends NonSoftDeletableEntity {

    /** 任务号。 */
    private String taskNo;

    /** PRODUCT、ROUTE、STEP、EQUIPMENT、MATERIAL、BOM、RECIPE、MATERIAL_LOT 或 WIP_INIT。 */
    private String importType;

    /** VALIDATE_ONLY 或 COMMIT。 */
    private String mode;

    /** SKIP 或 ROLLBACK。 */
    private String onError;

    /** 文件引用。 */
    private String fileRef;

    /** 操作人。 */
    private String operator;

    /** DRAFT、PENDING、RUNNING、SUCCESS、PARTIAL、FAILED 或 CANCELLED。 */
    private String status;

    /** 开始时间。 */
    private LocalDateTime startedAt;

    /** 结束时间。 */
    private LocalDateTime finishedAt;

    /** 总行数。 */
    private Integer totalRows;

    /** 成功行数。 */
    private Integer successRows;

    /** 失败行数。 */
    private Integer failRows;
}
