package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;

/**
 * LotCancelTrackInRequest 取消进站
 * <p>
 * 加工中的 Lot 回到等待，并离开设备。原因必填。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class LotCancelTrackInRequest {

    /** 原因码。 */
    @NotBlank
    private String reasonCode;

    /** 备注。 */
    private String remark;
}
