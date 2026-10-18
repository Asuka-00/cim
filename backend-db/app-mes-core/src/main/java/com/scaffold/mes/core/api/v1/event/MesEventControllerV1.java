package com.scaffold.mes.core.api.v1.event;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.event.MesDomainEvent;
import com.scaffold.mes.core.domain.entity.event.MesDomainEventDelivery;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventDeliveryRepository;
import com.scaffold.mes.core.domain.repository.event.MesDomainEventRepository;
import com.scaffold.mes.core.domain.service.event.MesEventDispatcher;
import com.scaffold.share.logging.AuditLog;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * MesEventControllerV1 领域事件接口
 * <p>
 * 查询事件与投递,并支持重投或跳过。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes")
@RequiredArgsConstructor
public class MesEventControllerV1 {

    private final MesDomainEventRepository eventRepository;
    private final MesDomainEventDeliveryRepository deliveryRepository;
    private final MesEventDispatcher dispatcher;

    /**
     * 分页查询领域事件。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param status 分发状态,可空
     * @return 事件分页
     */
    @GetMapping("/events")
    public Result<PageDTO<MesDomainEvent>> events(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        return Result.success(eventRepository.page(page, pageSize, status));
    }

    /**
     * 分页查询事件投递。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param subscriber 订阅者编码,可空
     * @param status 状态,可空
     * @return 投递分页
     */
    @GetMapping("/event-deliveries")
    public Result<PageDTO<MesDomainEventDelivery>> deliveries(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String subscriber,
            @RequestParam(required = false) String status) {
        return Result.success(deliveryRepository.page(page, pageSize, subscriber, status));
    }

    /**
     * 把投递重新置为待处理。
     *
     * @param id 投递主键
     * @return 固定为 true
     * @throws BusinessException 投递不存在
     */
    @PostMapping("/event-deliveries/{id}/retry")
    @AuditLog(operation = "重投领域事件")
    public Result<Boolean> retry(@PathVariable Long id) {
        dispatcher.retry(id);
        return Result.success(Boolean.TRUE);
    }

    /**
     * 跳过一条投递,不再重试。
     *
     * @param id 投递主键
     * @return 固定为 true
     * @throws BusinessException 投递不存在
     */
    @PostMapping("/event-deliveries/{id}/skip")
    @AuditLog(operation = "跳过领域事件")
    public Result<Boolean> skip(@PathVariable Long id) {
        dispatcher.skip(id);
        return Result.success(Boolean.TRUE);
    }
}
