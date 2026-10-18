package com.scaffold.mes.warehouse.domain;

/**
 * MesWarehouseErrorCodes 库房错误码
 * <p>
 * 段 420000–429999。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public final class MesWarehouseErrorCodes {

    /** 收货数量必须大于 0。 */
    public static final int RECEIPT_QTY = 420001;

    /** 物料批号已存在。 */
    public static final int RECEIPT_LOT_EXISTS = 420002;

    /** 只有待检物料批可以做来料检验。 */
    public static final int INSPECTION_STATUS = 420011;

    /** 检验结论必须是 PASS、FAIL 或 CONCESSION。 */
    public static final int INSPECTION_RESULT = 420012;

    /** 特采必须填写说明。 */
    public static final int INSPECTION_REMARK = 420013;

    /** 只有可用物料批可以领料。 */
    public static final int ISSUE_STATUS = 420021;

    /** 领料数量超过可用库存。 */
    public static final int ISSUE_QTY = 420022;

    /** 只有可用物料批可以整批冻结。 */
    public static final int FREEZE_STATUS = 420031;

    /** 只有已冻结的物料批可以解冻。 */
    public static final int UNFREEZE_STATUS = 420032;

    /** 同一物料批里的来料 Wafer 编号重复。 */
    public static final int INCOMING_WAFER_EXISTS = 420041;

    /** 来料 Wafer 编号不能为空。 */
    public static final int INCOMING_WAFER_CODE = 420042;

    /** 库房编码已存在，或类型不合法。 */
    public static final int WAREHOUSE_INVALID = 420051;

    /** 库位编码已存在，或库房未启用。 */
    public static final int LOCATION_INVALID = 420052;

    /** 收货库位不存在或未启用。 */
    public static final int LOCATION_DISABLED = 420053;

    /** 供应商编码已存在。 */
    public static final int SUPPLIER_EXISTS = 420061;

    /** 库位冻结数量超过在库可用量。 */
    public static final int INVENTORY_FREEZE_QTY = 420071;

    /** 解冻数量超过该库位已冻结数量。 */
    public static final int INVENTORY_UNFREEZE_QTY = 420072;

    /** 线边没有足够的可用库存。 */
    public static final int MATERIAL_SHORT = 420081;

    /** 绑定已下料，或处置方式不合法。 */
    public static final int BINDING_UNMOUNT = 420091;

    private MesWarehouseErrorCodes() {
    }
}
