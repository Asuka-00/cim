package com.scaffold.share.config;

import java.util.List;

import org.apache.ibatis.session.Configuration;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;

/**
 * BatchSqlInjector 自定义 SQL 注入器
 * <p>
 * 注册 InsertBatchSomeColumn 方法，使所有继承 BatchBaseMapper 的 Mapper
 * 可以使用 insertBatchSomeColumn 进行真正的批量插入（单条 INSERT ... VALUES (...), (...), ...）
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-25
 */
public class BatchSqlInjector extends DefaultSqlInjector {

    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(configuration, mapperClass, tableInfo);
        methodList.add(new InsertBatchSomeColumn());
        return methodList;
    }
}
