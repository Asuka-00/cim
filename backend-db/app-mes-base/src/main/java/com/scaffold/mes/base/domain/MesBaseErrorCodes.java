package com.scaffold.mes.base.domain;

/**
 * MesBaseErrorCodes 主数据错误码
 * <p>
 * 段 410000–419999。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public final class MesBaseErrorCodes {

    /** 计数单位不存在。 */
    public static final int COUNT_UNIT_NOT_FOUND = 410001;

    /** 计数单位编码已存在。 */
    public static final int COUNT_UNIT_CODE_EXISTS = 410002;

    /** 计数单位仍被产品或换算引用。 */
    public static final int COUNT_UNIT_IN_USE = 410003;

    /** 物料计量单位不存在。 */
    public static final int UOM_NOT_FOUND = 410011;

    /** 物料计量单位编码已存在。 */
    public static final int UOM_CODE_EXISTS = 410012;

    /** 物料计量单位仍被换算或物料引用。 */
    public static final int UOM_IN_USE = 410013;

    /** 物料单位换算不存在。 */
    public static final int UOM_CONVERSION_NOT_FOUND = 410021;

    /** 同一源单位、目标单位、物料的换算已存在。 */
    public static final int UOM_CONVERSION_EXISTS = 410022;

    /** 换算系数必须大于 0。 */
    public static final int UOM_CONVERSION_FACTOR_INVALID = 410023;

    /** 产品族不存在。 */
    public static final int PRODUCT_FAMILY_NOT_FOUND = 410031;

    /** 产品族编码已存在。 */
    public static final int PRODUCT_FAMILY_CODE_EXISTS = 410032;

    /** 产品族仍被产品引用。 */
    public static final int PRODUCT_FAMILY_IN_USE = 410033;

    /** 客户不存在。 */
    public static final int CUSTOMER_NOT_FOUND = 410041;

    /** 客户编码已存在。 */
    public static final int CUSTOMER_CODE_EXISTS = 410042;

    /** 客户仍被产品引用。 */
    public static final int CUSTOMER_IN_USE = 410043;

    /** 产品不存在。 */
    public static final int PRODUCT_NOT_FOUND = 410051;

    /** 产品料号已存在。 */
    public static final int PRODUCT_CODE_EXISTS = 410052;

    /** 产品仍被换算引用。 */
    public static final int PRODUCT_IN_USE = 410053;

    /** 产品类型不在 WAFER、DIE、PACKAGE、OTHER 中。 */
    public static final int PRODUCT_TYPE_INVALID = 410054;

    /** 晶圆尺寸不在 150、200、300 中。 */
    public static final int PRODUCT_WAFER_SIZE_INVALID = 410055;

    /** 产品计数换算不存在。 */
    public static final int PRODUCT_UNIT_CONV_NOT_FOUND = 410061;

    /** 同一产品、源单位、目标单位的换算已存在。 */
    public static final int PRODUCT_UNIT_CONV_EXISTS = 410062;

    /** 换算系数必须大于 0。 */
    public static final int PRODUCT_UNIT_CONV_FACTOR_INVALID = 410063;

    /** 允许偏差百分比不能为负。 */
    public static final int PRODUCT_UNIT_CONV_TOLERANCE_INVALID = 410064;

    /** 区域不存在。 */
    public static final int AREA_NOT_FOUND = 410071;

    /** 区域编码已存在。 */
    public static final int AREA_CODE_EXISTS = 410072;

    /** 区域仍被下级、站点或设备引用。 */
    public static final int AREA_IN_USE = 410073;

    /** 上级区域形成了自身或循环。 */
    public static final int AREA_PARENT_CYCLE = 410074;

    /** 站点不存在。 */
    public static final int STEP_NOT_FOUND = 410081;

    /** 站点编码已存在。 */
    public static final int STEP_CODE_EXISTS = 410082;

    /** 站点仍被后续工艺数据引用。 */
    public static final int STEP_IN_USE = 410083;

    /** 站点类型不在允许取值中。 */
    public static final int STEP_TYPE_INVALID = 410084;

    /** 设备组不存在。 */
    public static final int EQUIPMENT_GROUP_NOT_FOUND = 410091;

    /** 设备组编码已存在。 */
    public static final int EQUIPMENT_GROUP_CODE_EXISTS = 410092;

    /** 设备组仍被设备引用。 */
    public static final int EQUIPMENT_GROUP_IN_USE = 410093;

    /** 设备不存在。 */
    public static final int EQUIPMENT_NOT_FOUND = 410101;

    /** 设备编码已存在。 */
    public static final int EQUIPMENT_CODE_EXISTS = 410102;

    /** 设备仍被状态历史引用。 */
    public static final int EQUIPMENT_IN_USE = 410103;

    /** 设备类型不在允许取值中。 */
    public static final int EQUIPMENT_TYPE_INVALID = 410104;

    /** E10 状态不在六个标准大类中。 */
    public static final int EQUIPMENT_E10_INVALID = 410111;

    /** 停机类状态缺少原因码。 */
    public static final int EQUIPMENT_REASON_REQUIRED = 410112;

    /** 状态时间段结束早于开始。 */
    public static final int EQUIPMENT_STATUS_TIME_INVALID = 410113;

    /** 物料不存在。 */
    public static final int MATERIAL_NOT_FOUND = 410121;

    /** 物料编码已存在。 */
    public static final int MATERIAL_CODE_EXISTS = 410122;

    /** 物料仍被换算引用。 */
    public static final int MATERIAL_IN_USE = 410123;

    /** 物料类型不在允许取值中。 */
    public static final int MATERIAL_TYPE_INVALID = 410124;

    /** Recipe 不存在。 */
    public static final int RECIPE_NOT_FOUND = 410131;

    /** Recipe 编码已存在。 */
    public static final int RECIPE_CODE_EXISTS = 410132;

    /** Recipe 仍被版本引用。 */
    public static final int RECIPE_IN_USE = 410133;

    /** Recipe 版本不存在。 */
    public static final int RECIPE_VERSION_NOT_FOUND = 410141;

    /** 已生效或已失效的版本不能改内容，也不能删除。 */
    public static final int RECIPE_VERSION_LOCKED = 410142;

    /** 只有草稿可以生效。 */
    public static final int RECIPE_VERSION_NOT_DRAFT = 410143;

    /** 只有待审批的 Recipe 版本可以审批或驳回。 */
    public static final int RECIPE_VERSION_NOT_PENDING = 410144;

    /** 审批业务类型不在允许取值中。 */
    public static final int APPROVAL_BIZ_TYPE_INVALID = 410201;

    /** 审批动作不在 SUBMIT、APPROVE、REJECT、REVOKE 中。 */
    public static final int APPROVAL_ACTION_INVALID = 410202;

    /** 审批操作人不能为空。 */
    public static final int APPROVAL_OPERATOR_REQUIRED = 410203;

    /** 站点组不存在。 */
    public static final int STEP_GROUP_NOT_FOUND = 410211;

    /** 站点组编码已存在。 */
    public static final int STEP_GROUP_CODE_EXISTS = 410212;

    /** 站点组仍被站点引用。 */
    public static final int STEP_GROUP_IN_USE = 410213;

    /** Route 不存在。 */
    public static final int ROUTE_NOT_FOUND = 410221;

    /** Route 编码已存在。 */
    public static final int ROUTE_CODE_EXISTS = 410222;

    /** Route 仍被版本引用。 */
    public static final int ROUTE_IN_USE = 410223;

    /** Route 版本不存在。 */
    public static final int ROUTE_VERSION_NOT_FOUND = 410231;

    /** 只有草稿可以修改。 */
    public static final int ROUTE_VERSION_NOT_DRAFT = 410232;

    /** 只有待审批版本可以审批或驳回。 */
    public static final int ROUTE_VERSION_NOT_PENDING = 410233;

    /** 提交审批必须指定审批人。 */
    public static final int ROUTE_VERSION_ASSIGNEE_REQUIRED = 410234;

    /** 只有指定审批人可以通过或驳回。 */
    public static final int ROUTE_VERSION_NOT_ASSIGNEE = 410235;

    /** 需要设备的步骤没有配置设备组。 */
    public static final int ROUTE_VERSION_EQUIPMENT_REQUIRED = 410236;

    /** Route 步骤不存在。 */
    public static final int ROUTE_STEP_NOT_FOUND = 410241;

    /** 同一版本内步骤序号已存在。 */
    public static final int ROUTE_STEP_SEQ_EXISTS = 410242;

    /** 单位转换站点必须指定转换后的计数单位。 */
    public static final int ROUTE_STEP_CONVERT_UNIT_REQUIRED = 410243;

    /** Queue Time 动作只能是 HOLD 或 WARN。 */
    public static final int ROUTE_STEP_QUEUE_ACTION_INVALID = 410244;

    /** Time Link 不存在。 */
    public static final int ROUTE_TIME_LINK_NOT_FOUND = 410251;

    /** 同一版本、起点、终点的 Time Link 已存在。 */
    public static final int ROUTE_TIME_LINK_EXISTS = 410252;

    /** Time Link 的起点必须早于终点，且两步都在该版本中。 */
    public static final int ROUTE_TIME_LINK_SEQ_INVALID = 410253;

    /** HOLD 动作必须带原因码。 */
    public static final int ROUTE_TIME_LINK_REASON_REQUIRED = 410254;

    /** 站点设备组绑定不存在。 */
    public static final int STEP_EQUIPMENT_GROUP_NOT_FOUND = 410261;

    /** 同一站点已经绑定该设备组。 */
    public static final int STEP_EQUIPMENT_GROUP_EXISTS = 410262;

    /** 站点 Recipe 绑定不存在。 */
    public static final int STEP_RECIPE_NOT_FOUND = 410271;

    /** 同一站点、Recipe、产品的绑定已存在。 */
    public static final int STEP_RECIPE_EXISTS = 410272;

    /** 步骤 Recipe 覆盖不存在。 */
    public static final int ROUTE_STEP_RECIPE_NOT_FOUND = 410281;

    /** 同一 Route 步骤、Recipe、产品的覆盖已存在。 */
    public static final int ROUTE_STEP_RECIPE_EXISTS = 410282;

    /** Route 步骤仍被步骤级覆盖引用。 */
    public static final int ROUTE_STEP_IN_USE = 410245;

    /** 设备端口不存在。 */
    public static final int EQUIPMENT_PORT_NOT_FOUND = 410291;

    /** 同一设备的端口编码已存在。 */
    public static final int EQUIPMENT_PORT_CODE_EXISTS = 410292;

    /** 端口类型只能是 PORT 或 CHAMBER。 */
    public static final int EQUIPMENT_PORT_KIND_INVALID = 410293;

    /** 同时加工上限必须大于 0。 */
    public static final int EQUIPMENT_PORT_CAPACITY_INVALID = 410294;

    /** 设备能力不存在。 */
    public static final int EQUIPMENT_CAPABILITY_NOT_FOUND = 410301;

    /** 同一设备、站点、Recipe、产品的能力已存在。 */
    public static final int EQUIPMENT_CAPABILITY_EXISTS = 410302;

    /** 能力状态不在 VALID、SUSPENDED、EXPIRED、REVOKED 中。 */
    public static final int EQUIPMENT_CAPABILITY_STATUS_INVALID = 410303;

    /** 设备没有可用能力，且没有站点设备组绑定。 */
    public static final int EQUIPMENT_CAPABILITY_DENIED = 410304;

    /** BOM 不存在。 */
    public static final int BOM_NOT_FOUND = 410311;

    /** BOM 编码已存在。 */
    public static final int BOM_CODE_EXISTS = 410312;

    /** BOM 仍被版本引用。 */
    public static final int BOM_IN_USE = 410313;

    /** BOM 版本不存在。 */
    public static final int BOM_VERSION_NOT_FOUND = 410321;

    /** 只有 BOM 草稿可以修改或提交。 */
    public static final int BOM_VERSION_NOT_DRAFT = 410322;

    /** 只有待审批的 BOM 版本可以审批或驳回。 */
    public static final int BOM_VERSION_NOT_PENDING = 410323;

    /** BOM 行不存在。 */
    public static final int BOM_ITEM_NOT_FOUND = 410331;

    /** 同一 BOM 版本的行号已存在。 */
    public static final int BOM_ITEM_LINE_EXISTS = 410332;

    /** 消耗方式不在允许取值中。 */
    public static final int BOM_ITEM_CONSUME_MODE_INVALID = 410333;

    /** 单位用量必须大于 0。 */
    public static final int BOM_ITEM_QTY_INVALID = 410334;

    /** 产品版本不存在。 */
    public static final int PRODUCT_VERSION_NOT_FOUND = 410341;

    /** 只有产品草稿可以修改或提交。 */
    public static final int PRODUCT_VERSION_NOT_DRAFT = 410342;

    /** 只有待审批的产品版本可以审批或驳回。 */
    public static final int PRODUCT_VERSION_NOT_PENDING = 410343;

    /** 生效前 Route 和 BOM 都必须已有 ACTIVE 版本。 */
    public static final int PRODUCT_VERSION_MASTER_INACTIVE = 410344;

    /** BOM 行的站点或序号与当前生效 Route 不一致。 */
    public static final int PRODUCT_VERSION_BOM_MISMATCH = 410345;

    /** 工具编码已存在。 */
    public static final int TOOL_CODE_EXISTS = 410351;

    /** 只有可用的工具可以领用。 */
    public static final int TOOL_NOT_AVAILABLE = 410352;

    /** 只有使用中的工具可以归还。 */
    public static final int TOOL_NOT_IN_USE = 410353;

    /** 工具不存在。 */
    public static final int TOOL_NOT_FOUND = 410354;

    /** 人员资质目标类型不合法，或同一目标已有记录。 */
    public static final int USER_QUAL_INVALID = 410361;

    /** 异常单号已存在，或描述为空。 */
    public static final int EQUIPMENT_FAULT_INVALID = 410371;

    /** 异常单已关闭。 */
    public static final int EQUIPMENT_FAULT_CLOSED = 410372;

    /** 二次确认模式不合法，或动作已有规则。 */
    public static final int CONFIRM_RULE_INVALID = 410381;

    /** 这个动作不需要签名。 */
    public static final int ESIGN_NOT_REQUIRED = 410382;

    /** 签名人不符合确认方式。 */
    public static final int ESIGN_SIGNER = 410383;

    /** 版本生效还没有完成确认。 */
    public static final int CONFIRM_REQUIRED = 410384;

    /** 数据权限类型不合法，或同一范围已有记录。 */
    public static final int USER_SCOPE_INVALID = 410391;

    /** 抽检间隔不合法，或规则编码已存在。 */
    public static final int SAMPLING_RULE_INVALID = 410401;

    /** 规则不存在、已停用，或该站点与产品已绑定。 */
    public static final int STEP_SAMPLING_INVALID = 410402;

    /** 规格上下限都空，或该站点、采集项与产品已有规格。 */
    public static final int STEP_COLLECT_INVALID = 410403;

    /** Route 步骤不存在，规则不可用，或该步骤与产品已有抽检覆盖。 */
    public static final int ROUTE_SAMPLING_INVALID = 410404;

    /** Route 步骤不存在，规格不合法，或该步骤、采集项与产品已有规格。 */
    public static final int ROUTE_COLLECT_INVALID = 410405;

    /** 作业指导正文为空，或该站点与产品已有指导。 */
    public static final int STEP_INSTRUCTION_INVALID = 410406;

    /** 变更类型不合法，或 ECN 编号已存在。 */
    public static final int ECN_INVALID = 410407;

    /** 变更申请不存在，或当前状态不能提交。 */
    public static final int ECN_STATUS = 410408;

    private MesBaseErrorCodes() {
    }
}
