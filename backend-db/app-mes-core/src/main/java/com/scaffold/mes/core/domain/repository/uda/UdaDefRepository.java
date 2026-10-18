package com.scaffold.mes.core.domain.repository.uda;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.core.domain.entity.uda.UdaDef;

/**
 * UdaDefRepository 自定义属性定义仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UdaDefRepository {

    /**
     * 按对象类型和属性编码查询。
     *
     * @param objectType 对象类型
     * @param attrCode 属性编码
     * @return 定义，不存在时为空
     */
    Optional<UdaDef> findByObjectAndCode(String objectType, String attrCode);

    /**
     * 列出某个对象类型的全部定义，按属性编码排序。
     *
     * @param objectType 对象类型
     * @return 定义列表。类型为空时为空列表
     */
    List<UdaDef> listByObject(String objectType);

    /**
     * 列出某个对象类型上已启用的必填属性。
     *
     * @param objectType 对象类型
     * @return 必填定义。没有时为空列表
     */
    List<UdaDef> listRequired(String objectType);

    /**
     * 插入。
     *
     * @param def 定义
     */
    void save(UdaDef def);
}
