package com.scaffold.app;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AppApplication 应用启动类
 * <p>
 * 负责启动后端应用，配置组件扫描、MyBatis Mapper 扫描。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */

@SpringBootApplication(scanBasePackages = "com.scaffold")
@MapperScan(basePackages = { "com.scaffold.**.mapper" }, annotationClass = Mapper.class)
public class AppApplication {

    /**
     * 启动应用
     */
    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }

}
