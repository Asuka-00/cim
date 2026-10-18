package com.scaffold.mes.core.domain.entity.imports;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * ImportError 导入错误明细
 * <p>
 * 对应 hist_mes_import_error。只追加。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_import_error")
public class ImportError extends AppendOnlyEntity {

    /** 任务。 */
    private Long taskId;

    /** 行号。 */
    private Integer rowNo;

    /** 错误码。 */
    private String errorCode;

    /** 原始行内容。 */
    private String rawData;

    /** 错误信息。 */
    private String message;
}
