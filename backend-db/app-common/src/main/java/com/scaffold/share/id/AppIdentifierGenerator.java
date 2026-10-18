package com.scaffold.share.id;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;

/**
 * AppIdentifierGenerator MyBatis-Plus ID 生成器适配器
 * <p>
 * 负责将 MyBatis-Plus 的 ASSIGN_ID 主键生成委托给 APP 统一短雪花 ID 生成器。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-06-12
 */
public class AppIdentifierGenerator implements IdentifierGenerator {

    /**
     * 创建 MyBatis-Plus ID 生成器适配器
     */
    public AppIdentifierGenerator(AppIdGeneratorProperties properties) {
        AppIdGenerator.configure(properties);
    }

    /**
     * 生成下一个 MyBatis-Plus 主键 ID
     */
    @Override
    public Number nextId(Object entity) {
        return AppIdGenerator.nextId();
    }
}
