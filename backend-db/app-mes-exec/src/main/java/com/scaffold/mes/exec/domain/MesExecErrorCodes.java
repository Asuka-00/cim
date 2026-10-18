package com.scaffold.mes.exec.domain;

/**
 * MesExecErrorCodes 执行模块错误码
 * <p>
 * 号段 430000–439999。不要写入框架的 ErrorCodeConstants。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public final class MesExecErrorCodes {

    /** Hold 原因不存在。 */
    public static final int HOLD_REASON_NOT_FOUND = 430001;

    /** Hold 原因码已存在。 */
    public static final int HOLD_REASON_CODE_EXISTS = 430002;

    /** Hold 类别不在允许取值中。 */
    public static final int HOLD_REASON_TYPE_INVALID = 430003;

    /** Hold 原因仍被 Lot Hold 引用。 */
    public static final int HOLD_REASON_IN_USE = 430004;

    /** 工单不存在。 */
    public static final int WORK_ORDER_NOT_FOUND = 430011;

    /** 工单号已存在。 */
    public static final int WORK_ORDER_NO_EXISTS = 430012;

    /** 计划数量必须大于 0。 */
    public static final int WORK_ORDER_QTY_INVALID = 430013;

    /** 优先级必须在 1 到 9。 */
    public static final int WORK_ORDER_PRIORITY_INVALID = 430014;

    /** 当前状态不允许该操作。 */
    public static final int WORK_ORDER_STATUS_INVALID = 430015;

    /** Lot 不存在。 */
    public static final int LOT_NOT_FOUND = 430021;

    /** Lot 号已存在。 */
    public static final int LOT_NO_EXISTS = 430022;

    /** 开批数量必须大于 0。 */
    public static final int LOT_QTY_INVALID = 430023;

    /** 只有已下达的工单可以开批。 */
    public static final int LOT_WO_NOT_RELEASED = 430024;

    /** 开批后会超过工单计划量。 */
    public static final int LOT_OVER_PLAN = 430025;

    /** Lot 类型不在允许取值中。 */
    public static final int LOT_TYPE_INVALID = 430026;

    /** 批号不能为空。 */
    public static final int LOT_NO_REQUIRED = 430027;

    /** 该站点没有可回退的 Track-Out 次数。 */
    public static final int LOT_PASS_NOTHING_TO_UNDO = 430031;

    /** Wafer 内部编号已存在。 */
    public static final int WAFER_CODE_EXISTS = 430041;

    /** Wafer 片数与 Lot 数量不一致。 */
    public static final int WAFER_QTY_MISMATCH = 430042;

    /** 同一请求里的 Wafer 编号重复。 */
    public static final int WAFER_CODE_DUPLICATE = 430043;

    /** Wafer 用途不在允许取值中。 */
    public static final int WAFER_PURPOSE_INVALID = 430044;

    /** 该 Lot 已经绑定过 Wafer。 */
    public static final int WAFER_ALREADY_BOUND = 430045;

    /** Hold 原因已停用。 */
    public static final int LOT_HOLD_REASON_DISABLED = 430051;

    /** 终态 Lot 不能 Hold。 */
    public static final int LOT_HOLD_LOT_CLOSED = 430052;

    /** Hold 记录不存在。 */
    public static final int LOT_HOLD_NOT_FOUND = 430053;

    /** Hold 已经释放。 */
    public static final int LOT_HOLD_ALREADY_RELEASED = 430054;

    /** 当前操作人没有该 Hold 要求的 Release 角色。 */
    public static final int LOT_HOLD_ROLE_DENIED = 430055;

    /** Hold 来源不在允许取值中。 */
    public static final int LOT_HOLD_SOURCE_INVALID = 430056;

    /** 历史事务类型不能为空。 */
    public static final int LOT_HISTORY_TYPE_REQUIRED = 430061;

    /** Lot 级 Track 不能逐片写 Wafer 历史。 */
    public static final int WAFER_HISTORY_LOT_TRACK = 430071;

    /** Wafer 历史事务类型不能为空。 */
    public static final int WAFER_HISTORY_TYPE_REQUIRED = 430072;

    /** 追溯上游类型不合法。 */
    public static final int GENEALOGY_PARENT_TYPE_INVALID = 430081;

    /** 追溯下游类型不合法。 */
    public static final int GENEALOGY_CHILD_TYPE_INVALID = 430082;

    /** 追溯关系类型不合法。 */
    public static final int GENEALOGY_LINK_TYPE_INVALID = 430083;

    /** Interlock 有规则阻断了进站。 */
    public static final int INTERLOCK_BLOCK = 430091;

    /** 只有加工中的 Lot 可以出站。 */
    public static final int LOT_NOT_RUNNING = 430101;

    /** 报废原因不在 MES_SCRAP_REASON 中或已停用。 */
    public static final int SCRAP_REASON_INVALID = 430111;

    /** 终态 Lot 不能报废。 */
    public static final int LOT_SCRAP_CLOSED = 430112;

    /** Wafer 不存在或不属于该 Lot。 */
    public static final int WAFER_NOT_FOUND = 430113;

    /** Wafer 已经不能报废。 */
    public static final int WAFER_NOT_SCRAPPABLE = 430114;

    /** 不追踪个体时，报废数量必须大于 0 且不超过当前数量。 */
    public static final int SCRAP_QTY_INVALID = 430115;

    /** 优先级必须在 1 到 9。 */
    public static final int LOT_PRIORITY_INVALID = 430121;

    /** 终态 Lot 不能改优先级。 */
    public static final int LOT_PRIORITY_CLOSED = 430122;

    /** 只有等待中的 Lot 可以拆批。 */
    public static final int LOT_SPLIT_NOT_WAITING = 430131;

    /** 至少拆出一片，并且父 Lot 至少留下一片。 */
    public static final int LOT_SPLIT_QTY_INVALID = 430132;

    /** 两个 Lot 不满足合批条件。 */
    public static final int LOT_MERGE_REJECTED = 430141;

    /** 终态 Lot 不能 Bonus。 */
    public static final int LOT_BONUS_CLOSED = 430151;

    /** Bonus 数量必须大于 0。 */
    public static final int LOT_BONUS_QTY_INVALID = 430152;

    /** Bonus 后会超过工单计划量。 */
    public static final int LOT_BONUS_OVER_PLAN = 430153;

    /** 只有已完成的 Lot 可以出货。 */
    public static final int LOT_SHIP_STATUS = 430161;

    /** Hold 中的 Lot 不能出货。 */
    public static final int LOT_SHIP_HOLD = 430162;

    /** 出货单号已存在。 */
    public static final int SHIP_NO_EXISTS = 430163;

    /** 终态 Lot 不能终止。 */
    public static final int LOT_TERMINATE_CLOSED = 430171;

    /** 终止必须填写原因。 */
    public static final int LOT_TERMINATE_REASON_REQUIRED = 430172;

    /** 只有等待中的 Lot 可以跳站。 */
    public static final int LOT_SKIP_NOT_WAITING = 430181;

    /** 已经没有下一步可以跳。 */
    public static final int LOT_SKIP_NO_NEXT = 430182;

    /** 跳站必须填写原因。 */
    public static final int LOT_SKIP_REASON_REQUIRED = 430183;

    /** 这片 Wafer 现在不能回收。 */
    public static final int WAFER_RECLAIM_REJECTED = 430191;

    /** 只有已回收的片可以再入厂。 */
    public static final int WAFER_REENTER_REJECTED = 430192;

    /** 只有等待中且在主流程上的 Lot 可以进入返工。 */
    public static final int LOT_REWORK_NOT_READY = 430201;

    /** 当前步骤不在返工允许进入的范围内。 */
    public static final int LOT_REWORK_OUT_OF_RANGE = 430202;

    /** 该返工分支已达到次数上限。 */
    public static final int LOT_REWORK_MAX = 430203;

    /** 返工分支没有可去的下一步，也没有回归步骤。 */
    public static final int LOT_REWORK_NOWHERE = 430204;

    /** 预约动作类型目前只支持 HOLD 和 CHANGE_PRIORITY。 */
    public static final int FUTURE_ACTION_TYPE_INVALID = 430211;

    /** 只有加工中的 Lot 可以取消进站。 */
    public static final int LOT_CANCEL_TRACK_IN_STATUS = 430221;

    /** 取消进站必须填写原因。 */
    public static final int LOT_CANCEL_TRACK_IN_REASON = 430222;

    /** 没有可撤销的出站。 */
    public static final int LOT_UNDO_TRACK_OUT_MISSING = 430231;

    /** 当前状态不能撤销出站。 */
    public static final int LOT_UNDO_TRACK_OUT_STATUS = 430232;

    /** 原设备已经没有空位，不能撤销出站。 */
    public static final int LOT_UNDO_TRACK_OUT_CAPACITY = 430233;

    /** 这片 Wafer 不是报废状态，不能取消报废。 */
    public static final int WAFER_UNSCRAP_STATUS = 430241;

    /** 只有主流程上等待中的 Lot 可以强制跳站。 */
    public static final int LOT_FORCE_MOVE_NOT_READY = 430251;

    /** 目标步骤不在当前 Route 上。 */
    public static final int LOT_FORCE_MOVE_STEP_MISSING = 430252;

    /** 强制跳站必须填写原因。 */
    public static final int LOT_FORCE_MOVE_REASON = 430253;

    /** 只有主流程上等待中的 Lot 可以退回。 */
    public static final int LOT_MOVE_BACK_NOT_READY = 430261;

    /** 已经是第一步，不能再退。 */
    public static final int LOT_MOVE_BACK_NO_PREVIOUS = 430262;

    /** 退回必须填写原因。 */
    public static final int LOT_MOVE_BACK_REASON = 430263;

    /** 载具不存在。 */
    public static final int CARRIER_NOT_FOUND = 430271;

    /** 载具编码已存在。 */
    public static final int CARRIER_CODE_EXISTS = 430272;

    /** 载具类型不能做整批绑定。 */
    public static final int CARRIER_TYPE_INVALID = 430273;

    /** 载具已经绑着别的 Lot，或状态不是空闲。 */
    public static final int CARRIER_OCCUPIED = 430274;

    /** Lot 已经绑在别的载具上。 */
    public static final int LOT_ALREADY_ON_CARRIER = 430275;

    /** 排序规则编码已存在。 */
    public static final int DISPATCH_RULE_CODE_EXISTS = 430281;

    /** 备注类型不在允许取值中。 */
    public static final int LOT_NOTE_TYPE_INVALID = 430291;

    /** 备注内容不能为空。 */
    public static final int LOT_NOTE_CONTENT_REQUIRED = 430292;

    /** 只有主流程上等待中的 Lot 可以改 Route。 */
    public static final int LOT_ROUTE_CHANGE_NOT_READY = 430301;

    /** 新 Route 上没有目标步骤。 */
    public static final int LOT_ROUTE_CHANGE_STEP_MISSING = 430302;

    /** 改 Route 必须填写原因。 */
    public static final int LOT_ROUTE_CHANGE_REASON = 430303;

    /** 片级进出站必须选择属于该 Lot 的存活片。 */
    public static final int WAFER_TRACK_SELECTION = 430311;

    /** 批次不存在。 */
    public static final int BATCH_NOT_FOUND = 430321;

    /** 批次号已存在。 */
    public static final int BATCH_NO_EXISTS = 430322;

    /** 只有等待且未 Hold 的 Lot 可以加入批次。 */
    public static final int BATCH_LOT_NOT_READY = 430323;

    /** Lot 的站点与批次不一致。 */
    public static final int BATCH_STEP_MISMATCH = 430324;

    /** Lot 已经在某个批次里。 */
    public static final int BATCH_LOT_BUSY = 430325;

    /** 批次已满，或当前状态不能再改成员。 */
    public static final int BATCH_CLOSED = 430326;

    /** Recipe 版本与批次已定下的版本不同。 */
    public static final int BATCH_RECIPE_MISMATCH = 430327;

    /** 转换站点出站必须填写转换后数量。 */
    public static final int UNIT_CONVERT_QTY_REQUIRED = 430331;

    /** 转换数量偏差超过允许范围且没有确认原因。 */
    public static final int UNIT_CONVERT_DEVIATION = 430332;

    /** 产品上没有这条单位换算。 */
    public static final int UNIT_CONVERT_FACTOR_MISSING = 430333;

    /** 良品 Bin 合计与 Lot 数量不一致。 */
    public static final int DIE_BIN_QTY_MISMATCH = 430341;

    /** 已有逐片 Bin 明细，不能再写汇总行。 */
    public static final int DIE_BIN_DETAIL_EXISTS = 430342;

    /** Bin 编码重复、为空，或数量为负。 */
    public static final int DIE_BIN_LINE_INVALID = 430343;

    /** Wafer 不属于这个 Lot。 */
    public static final int WAFER_MAP_LOT = 430351;

    /** Wafer Map 文本没有可用的 Bin 行。 */
    public static final int WAFER_MAP_CSV = 430352;

    /** 投入数量必须大于 0，且不能超过源 Lot 当前数量。 */
    public static final int PACKAGE_INPUT_QTY = 430361;

    /** 源 Lot 不在制，或与目标是同一个 Lot。 */
    public static final int PACKAGE_INPUT_SOURCE = 430362;

    /** 封装 Lot 必须已绑定工单，并且仍在制。 */
    public static final int PACKAGE_INPUT_TARGET = 430363;

    /** Strip 编号已存在。 */
    public static final int STRIP_CODE_EXISTS = 430371;

    /** 只有在制 Lot 可以建立 Strip。 */
    public static final int STRIP_LOT_CLOSED = 430372;

    /** Strip 的数量不合法。 */
    public static final int STRIP_QTY = 430373;

    /** 没有可切筋成型的 Strip。 */
    public static final int STRIP_CONVERT_EMPTY = 430374;

    /** 容器号已存在。 */
    public static final int PACK_NO_EXISTS = 430381;

    /** 容器类型或容量不合法。 */
    public static final int PACK_TYPE = 430382;

    /** 装入数量会超过容量，或容器已不是 OPEN。 */
    public static final int PACK_QTY = 430383;

    /** 产品没有打开 Unit 序列号跟踪。 */
    public static final int UNIT_SERIAL_DISABLED = 430391;

    /** 序列号已存在。 */
    public static final int UNIT_SERIAL_EXISTS = 430392;

    /** 当前用户对这台设备的资质无效。 */
    public static final int PERSON_NOT_QUALIFIED = 430401;

    /** 当前用户对这个站点的资质无效。 */
    public static final int STEP_NOT_QUALIFIED = 430402;

    /** 当前用户对这个设备组的资质无效。 */
    public static final int GROUP_NOT_QUALIFIED = 430403;

    /** 当前用户没有这个站点的数据权限。 */
    public static final int STEP_OUT_OF_SCOPE = 430411;

    /** 当前用户没有这个设备组的数据权限。 */
    public static final int EQUIPMENT_GROUP_OUT_OF_SCOPE = 430412;

    /** 排队时间已超过步骤上限。 */
    public static final int QUEUE_TIME_EXCEEDED = 430421;

    /** 工具已达到寿命上限。 */
    public static final int TOOL_LIFE_EXCEEDED = 430431;

    /** 工具类型不符合站点要求。 */
    public static final int TOOL_TYPE_MISMATCH = 430432;

    /** 这个动作还没有完成要求的确认。 */
    public static final int CONFIRM_REQUIRED = 430441;

    /** 这不是未清偿的欠账，或线边仍不够补扣。 */
    public static final int SHORTAGE_UNSETTLED = 430451;

    /** 欠账不能修正，或修正后的数量不合法，或线边不够再扣。 */
    public static final int CONSUMPTION_ADJUST = 430452;

    /** 抽检间隔小于 1。 */
    public static final int SAMPLING_INTERVAL = 430461;

    /** Lot 不在加工中，不能录入采集值。 */
    public static final int COLLECT_NOT_RUNNING = 430471;

    /** 采集值没有填写。 */
    public static final int COLLECT_VALUE = 430472;

    /** 没有可作废的采集值。 */
    public static final int COLLECT_VOID = 430473;

    /** 必采项还没有有效值。 */
    public static final int COLLECT_REQUIRED = 430474;

    /** 没有可更正的采集值。 */
    public static final int COLLECT_CORRECT = 430475;

    /** 交接单号已存在。 */
    public static final int HANDOVER_INVALID = 430481;

    /** 交接班不存在，或当前状态不允许这次操作。 */
    public static final int HANDOVER_STATUS = 430482;

    /** 自定义属性没定义、已停用，或值不合法。 */
    public static final int UDA_VALUE = 430491;

    /** 打印份数小于 1。 */
    public static final int PRINT_COPIES = 430492;

    /** 期初导入没有指定合法的站点。 */
    public static final int WIP_STEP = 430493;

    private MesExecErrorCodes() {
    }
}
