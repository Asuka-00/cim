package com.scaffold.share.integration.realtime;

import java.io.Serializable;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RealtimeEvent 实时事件载荷。
 * <p>
 * type 使用字符串常量（见 {@link RealtimeEventTypes}），便于业务扩展而无需改动通道层。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型（如 PERMISSION_CHANGED） */
    private String type;

    /** 时间戳（毫秒） */
    private long timestamp;

    /** 业务数据，须可 JSON 序列化 */
    private Object data;

    /**
     * 以当前时间构造事件。
     *
     * @param type 事件类型
     * @param data 业务数据
     * @return 实时事件
     */
    public static RealtimeEvent of(String type, Object data) {
        return RealtimeEvent.builder()
                .type(type)
                .timestamp(Instant.now().toEpochMilli())
                .data(data)
                .build();
    }
}
