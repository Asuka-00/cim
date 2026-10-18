package com.scaffold.mes.core.domain.dto.txn;

import java.util.ArrayList;
import java.util.List;

import com.scaffold.mes.core.domain.service.txn.MesLockTarget;

import lombok.Getter;
import lombok.Setter;

/**
 * MesTxnCommand 统一事务命令
 * <p>
 * 描述一次业务写入。取号在调用事务层之前完成。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class MesTxnCommand {

    /** 事务类型,同时作为幂等键的前缀。 */
    private String txnType;

    /** 客户端请求号。为空时不做幂等。 */
    private String requestId;

    /** 主对象类型,如 LOT、WORK_ORDER。 */
    private String objectType;

    /** 主对象主键。 */
    private Long objectId;

    /** 主对象编码,便于按单号检索。 */
    private String objectCode;

    /** 来源:TERMINAL、ADMIN、IMPORT 或 SYSTEM。 */
    private String source = "ADMIN";

    /** 原因码,可空。 */
    private String reasonCode;

    /** 操作人。为空时取当前会话用户。 */
    private String operator;

    /** 本事务要锁的行。事务层会按工单、设备、Lot、Wafer 的顺序加 NOWAIT 锁。 */
    private List<MesLockTarget> locks = new ArrayList<>();
}
