package com.scaffold.system.infra.config.rbac;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.scaffold.system.domain.converter.rbac.LoginConverterImpl;

/**
 * RbacConverterConfiguration RBAC 对象转换器配置类
 * <p>
 * 显式注册 MapStruct 生成的登录转换器，确保其在多模块 Reactor 启动过程中可被应用服务注入。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-07-10
 */
@Configuration
@Import(LoginConverterImpl.class)
public class RbacConverterConfiguration {
}
