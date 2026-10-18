package com.scaffold.mes.exec.domain.dto;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * PackRequest 装入容器
 * <p>
 * 数量加到当前装载。达到容量时容器变为 FULL。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Getter
@Setter
public class PackRequest {

    /** 本次装入数量。 */
    @NotNull
    private Integer qty;
}
