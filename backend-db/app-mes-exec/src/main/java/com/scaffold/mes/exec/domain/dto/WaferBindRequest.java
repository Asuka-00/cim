package com.scaffold.mes.exec.domain.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * WaferBindRequest 给 Lot 绑定 Wafer
 * <p>
 * 传入编号则按列表建片。列表为空时按 count 自动生成，编号为 Lot 号加点号和 Slot。片数必须等于 Lot 当前数量。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class WaferBindRequest {

    /** 指定的片。为空则按 count 生成。 */
    private List<WaferPiece> wafers = new ArrayList<>();

    /** 自动生成的片数。指定了 wafers 时忽略。 */
    private Integer count;

    /** 用途。空则 PRODUCT。 */
    private String purpose;

    /**
     * WaferPiece 一片的编号
     */
    @Getter
    @Setter
    public static class WaferPiece {

        /** 本厂内部编号。 */
        private String waferCode;

        /** 客户编号，允许与其他片相同。 */
        private String vendorWaferId;
    }
}
