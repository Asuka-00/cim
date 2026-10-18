package com.scaffold.system.domain.vo.system;

import com.scaffold.share.enums.StateEnum;
import com.scaffold.share.vo.BaseVO;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SystemConfigVO视图对象
 * <p>
 * 负责承载SystemConfigVO相关展示与返回数据
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SystemConfigVO extends BaseVO {

    private String configCode;

    private String configValue;

    private String valueType;

    private String groupCode;

    private String remark;

    private StateEnum state;
}
