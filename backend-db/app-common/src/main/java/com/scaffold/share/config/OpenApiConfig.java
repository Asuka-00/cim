package com.scaffold.share.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenApiConfig配置类
 * <p>
 * 负责提供OpenApiConfig相关配置与组件装配
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Configuration
public class OpenApiConfig {

        private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

        /**
         * 创建 OpenAPI 配置
         */
        @Bean
        public OpenAPI customOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("APP Backend API")
                                                .description("APP 系统后端 API 文档")
                                                .version("v1.0.0")
                                                .contact(new Contact()
                                                                .name("Scaffold")
                                                                .email("contact.com"))
                                                .license(new License()
                                                                .name("Apache 2.0")
                                                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                                .components(new Components()
                                                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                                                new SecurityScheme()
                                                                                .type(SecurityScheme.Type.HTTP)
                                                                                .scheme("bearer")
                                                                                .bearerFormat("JWT")
                                                                                .description("请输入 JWT Token")))
                                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
        }
}
