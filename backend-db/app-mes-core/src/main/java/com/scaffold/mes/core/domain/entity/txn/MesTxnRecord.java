package com.scaffold.mes.core.domain.entity.txn;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.scaffold.share.entity.AppendOnlyEntity;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * MesTxnRecord 事务流水
 * <p>
 * 对应 hist_mes_transaction。只增不改,幂等键仅在成功时写入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
@FieldNameConstants
@TableName("hist_mes_transaction")
public class MesTxnRecord extends AppendOnlyEntity {

    /** 事务号(雪花ID字符串) */
    private String txnNo;

    /** 事务类型 CREATE_LOT/TRACK_IN/TRACK_OUT/HOLD/RELEASE/SCRAP/… */
    private String txnType;

    /** 主对象类型 */
    private String objectType;

    /** 主对象ID */
    private Long objectId;

    /** 主对象编码(冗余,便于检索) */
    private String objectCode;

    /** 父事务ID,Batch 等展开的子事务指向父事务 */
    private Long parentTxnId;

    /** 客户端请求ID(诊断用,不要求唯一) */
    private String requestId;

    /** 幂等键 = txn_type:request_id,仅 result=SUCCESS 时有值,否则为 NULL */
    private String idemKey;

    /** 关联原事务ID,纠错/撤销事务指向被纠正的事务 */
    private Long refTxnId;

    /** 来源 TERMINAL/ADMIN/IMPORT/SYSTEM */
    private String source;

    /** 原因码 */
    private String reasonCode;

    /** 操作人(用户名) */
    private String operator;

    /** 结果 SUCCESS/FAILED */
    private String result;

    /** 失败原因 */
    private String errorMessage;

    /** 请求与变更摘要 */
    private String payloadJson;

    /** 备注 */
    private String remark;

    /** 事务发生时间 */
    private LocalDateTime txnTime;

}
