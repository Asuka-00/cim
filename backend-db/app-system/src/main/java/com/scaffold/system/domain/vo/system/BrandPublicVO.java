package com.scaffold.system.domain.vo.system;

import lombok.Builder;
import lombok.Data;

/**
 * 登录前可公开的品牌配置（仅 BRAND 分组白名单字段）。
 */
@Data
@Builder
public class BrandPublicVO {

    private String title;
    private String poweredBy;
    /** 原始 SVG 文本，空表示未配置 */
    private String logoSvg;
    private String faviconSvg;
    private String loginTitleSvg;
    private String loginBgSvg;
    private Boolean showLoginTitleImage;
}
