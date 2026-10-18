package com.scaffold.mes.core.domain.repository.alert;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.alert.MesAlert;

/**
 * MesAlertRepository 预警仓储
 * <p>
 * 查询并更新预警状态。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesAlertRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 记录,不存在时为空
     */
    Optional<MesAlert> findById(Long id);

    /**
     * 按来源事件和规则编码查询。
     *
     * @param eventId 领域事件
     * @param ruleCode 规则编码
     * @return 预警，还没生成时为空
     */
    Optional<MesAlert> findByEventAndRule(Long eventId, String ruleCode);

    /**
     * 分页查询预警。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param status 状态,空表示不限
     * @return 分页结果
     */
    PageDTO<MesAlert> page(int page, int pageSize, String status);

    /**
     * 统计未关闭的预警。状态为 NEW 或 READ。
     *
     * @return 条数
     */
    long countOpen();

    /**
     * 插入记录。
     *
     * @param alert 待插入对象
     */
    void save(MesAlert alert);

    /**
     * 更新记录。
     *
     * @param alert 已加载的对象
     */
    void update(MesAlert alert);
}
