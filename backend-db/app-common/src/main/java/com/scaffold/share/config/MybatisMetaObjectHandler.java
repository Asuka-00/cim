package com.scaffold.share.config;

import java.time.LocalDateTime;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.scaffold.share.context.CurrentSessionProvider;

import lombok.RequiredArgsConstructor;

/**
 * MybatisMetaObjectHandler类
 * <p>
 * 负责处理MybatisMetaObjectHandler相关核心业务能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@Component
@RequiredArgsConstructor
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    private final CurrentSessionProvider currentSessionProvider;

    /**
     * 填充新增字段
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String userName = currentSessionProvider.getCurrentUserNameOrDefault();
        String tenantId = currentSessionProvider.getCurrentTenantIdOrDefault();

        fillIfAbsent(metaObject, "tenantId", tenantId);
        fillIfAbsent(metaObject, "createBy", userName);
        fillIfAbsent(metaObject, "createAt", now);
        fillIfAbsent(metaObject, "updateBy", userName);
        fillIfAbsent(metaObject, "updateAt", now);
    }

    /**
     * 填充更新字段
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        String userName = currentSessionProvider.getCurrentUserNameOrDefault();

        setFieldValByName("updateBy", userName, metaObject);
        setFieldValByName("updateAt", now, metaObject);
    }

    /**
     * 在字段缺失时填充默认值
     */
    private void fillIfAbsent(MetaObject metaObject, String fieldName, Object value) {
        if (getFieldValByName(fieldName, metaObject) == null) {
            setFieldValByName(fieldName, value, metaObject);
        }
    }
}
