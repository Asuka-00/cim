package com.scaffold.mes.core.domain;

/**
 * MesErrorCodes MES 通用错误码
 * <p>
 * 段 400000–409999,不写入 {@code ErrorCodeConstants}。400101 与 common 的
 * {@code updateWithVersion} 使用同一个数字。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public final class MesErrorCodes {

    /** 乐观锁冲突或记录已不存在。 */
    public static final int CONCURRENT_MODIFICATION = 400101;

    /** 聚合根行锁冲突,立即失败。 */
    public static final int OBJECT_BUSY = 400102;

    /** 库存或工具行锁等待超时。 */
    public static final int LOCK_WAIT_TIMEOUT = 400103;

    /** 编号规则不存在或已停用。 */
    public static final int SEQUENCE_RULE_NOT_FOUND = 400110;

    /** 编号规则编码已存在。 */
    public static final int SEQUENCE_RULE_EXISTS = 400111;

    /** 流水超出位数,或 Lot 号超过长度上限。 */
    public static final int SEQUENCE_OVERFLOW = 400112;

    /** 事务或事件投递不存在,或幂等响应无法还原。 */
    public static final int TXN_NOT_FOUND = 400120;

    /** 同一笔事务已经撤销过。 */
    public static final int TXN_ALREADY_REVERSED = 400121;

    /** 预警不存在。 */
    public static final int ALERT_NOT_FOUND = 400130;

    /** 文件名、大小或摘要不合法。 */
    public static final int FILE_INVALID = 400140;

    /** 文件不存在。 */
    public static final int FILE_NOT_FOUND = 400141;

    /** 班次编码已存在，或名称、时间为空。 */
    public static final int SHIFT_INVALID = 400150;

    /** 这一天已经有日历，或工作日标记不是 0 或 1。 */
    public static final int CALENDAR_INVALID = 400151;

    /** 规则编码已存在，名称或事件类型为空，级别或接收范围不合法。 */
    public static final int NOTIFY_RULE_INVALID = 400160;

    /** 同一对象类型下属性编码已存在，或对象类型、数据类型不合法。 */
    public static final int UDA_INVALID = 400170;

    /** 模板编码已存在，或类型、内容不合法。 */
    public static final int PRINT_TEMPLATE_INVALID = 400180;

    /** 任务号已存在，导入类型不合法，或当前状态不能提交。 */
    public static final int IMPORT_TASK_INVALID = 400190;

    private MesErrorCodes() {
    }
}
