package com.scaffold.mes.core.domain.entity.print;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * PrintLog 打印记录
 * <p>
 * 对应 hist_mes_print_log。只追加。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_print_log")
public class PrintLog extends AppendOnlyEntity {

    /** 模板。 */
    private Long tplId;

    /** 模板类型。 */
    private String tplType;

    /** 打印对象类型。 */
    private String bizType;

    /** 打印对象。 */
    private Long bizId;

    /** 对象编码。 */
    private String bizCode;

    /** 份数。 */
    private Integer copies;

    /** 1 表示重打。 */
    private Integer isReprint;

    /** 打印时间。 */
    private LocalDateTime printedAt;
}
