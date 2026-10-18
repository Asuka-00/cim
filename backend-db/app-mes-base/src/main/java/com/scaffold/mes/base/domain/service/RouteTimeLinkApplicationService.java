package com.scaffold.mes.base.domain.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.RouteTimeLinkSaveRequest;
import com.scaffold.mes.base.domain.entity.RouteTimeLink;
import com.scaffold.mes.base.domain.entity.RouteVersion;
import com.scaffold.mes.base.domain.repository.RouteStepRepository;
import com.scaffold.mes.base.domain.repository.RouteTimeLinkRepository;
import com.scaffold.mes.base.domain.repository.RouteVersionRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * RouteTimeLinkApplicationService Time Link 应用服务
 * <p>
 * 只有草稿版本能改。起点和终点都必须是该版本已有步骤，且起点序号小于终点。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class RouteTimeLinkApplicationService {

    private static final Set<String> ACTIONS = Set.of("HOLD", "WARN");

    private final RouteTimeLinkRepository repository;
    private final RouteVersionRepository versionRepository;
    private final RouteStepRepository stepRepository;

    /**
     * 列出某个版本的 Time Link。
     *
     * @param routeVersionId Route 版本
     * @return 按起点序号排序的列表
     * @throws BusinessException 版本不存在
     */
    public List<RouteTimeLink> list(Long routeVersionId) {
        requireVersion(routeVersionId);
        return repository.listByVersion(routeVersionId);
    }

    /**
     * 在草稿版本上新增 Time Link。
     *
     * @param routeVersionId Route 版本
     * @param request 约束内容
     * @throws BusinessException 版本不是草稿，序号不合法，或组合已存在
     */
    @Transactional
    public void create(Long routeVersionId, RouteTimeLinkSaveRequest request) {
        requireDraft(routeVersionId);
        validate(routeVersionId, request);
        if (repository.findExact(routeVersionId, request.getFromSeqNo(), request.getToSeqNo()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_TIME_LINK_EXISTS, "Time Link 已存在");
        }
        RouteTimeLink link = new RouteTimeLink();
        link.setRouteVersionId(routeVersionId);
        copy(request, link);
        repository.save(link);
    }

    /**
     * 修改草稿版本上的 Time Link。
     *
     * @param routeVersionId Route 版本
     * @param id 主键
     * @param request 约束内容
     * @throws BusinessException 记录不存在，版本不是草稿，或起止序号与其他记录冲突
     */
    @Transactional
    public void update(Long routeVersionId, Long id, RouteTimeLinkSaveRequest request) {
        requireDraft(routeVersionId);
        RouteTimeLink link = require(routeVersionId, id);
        validate(routeVersionId, request);
        repository.findExact(routeVersionId, request.getFromSeqNo(), request.getToSeqNo())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new BusinessException(MesBaseErrorCodes.ROUTE_TIME_LINK_EXISTS, "Time Link 已存在");
                });
        copy(request, link);
        repository.update(link);
    }

    /**
     * 删除草稿版本上的 Time Link。
     *
     * @param routeVersionId Route 版本
     * @param id 主键
     * @throws BusinessException 记录不存在或版本不是草稿
     */
    @Transactional
    public void delete(Long routeVersionId, Long id) {
        requireDraft(routeVersionId);
        require(routeVersionId, id);
        repository.deleteById(id);
    }

    private void validate(Long routeVersionId, RouteTimeLinkSaveRequest request) {
        if (request.getFromSeqNo() >= request.getToSeqNo()
                || stepRepository.findBySeq(routeVersionId, request.getFromSeqNo()).isEmpty()
                || stepRepository.findBySeq(routeVersionId, request.getToSeqNo()).isEmpty()) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_TIME_LINK_SEQ_INVALID, "Time Link 起止步骤不合法");
        }
        if (request.getMaxMinutes() == null || request.getMaxMinutes() <= 0) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_TIME_LINK_SEQ_INVALID, "最长间隔必须大于 0");
        }
        if (request.getWarnMinutes() != null
                && (request.getWarnMinutes() <= 0 || request.getWarnMinutes() >= request.getMaxMinutes())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_TIME_LINK_SEQ_INVALID, "预警阈值必须小于最长间隔");
        }
        String action = request.getAction() == null || request.getAction().isBlank() ? "HOLD" : request.getAction();
        if (!ACTIONS.contains(action)) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_STEP_QUEUE_ACTION_INVALID, "超时动作不合法: " + action);
        }
        if ("HOLD".equals(action) && (request.getReasonCode() == null || request.getReasonCode().isBlank())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_TIME_LINK_REASON_REQUIRED, "HOLD 必须填写原因码");
        }
    }

    private void copy(RouteTimeLinkSaveRequest request, RouteTimeLink link) {
        String action = request.getAction() == null || request.getAction().isBlank() ? "HOLD" : request.getAction();
        link.setFromSeqNo(request.getFromSeqNo());
        link.setToSeqNo(request.getToSeqNo());
        link.setMaxMinutes(request.getMaxMinutes());
        link.setWarnMinutes(request.getWarnMinutes());
        link.setAction(action);
        link.setReasonCode(request.getReasonCode());
    }

    private RouteVersion requireDraft(Long routeVersionId) {
        RouteVersion version = requireVersion(routeVersionId);
        if (!"DRAFT".equals(version.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_DRAFT, "只有草稿版本可以修改 Time Link");
        }
        return version;
    }

    private RouteVersion requireVersion(Long routeVersionId) {
        return versionRepository.findById(routeVersionId)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_VERSION_NOT_FOUND, "Route 版本不存在"));
    }

    private RouteTimeLink require(Long routeVersionId, Long id) {
        RouteTimeLink link = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ROUTE_TIME_LINK_NOT_FOUND, "Time Link 不存在"));
        if (!routeVersionId.equals(link.getRouteVersionId())) {
            throw new BusinessException(MesBaseErrorCodes.ROUTE_TIME_LINK_NOT_FOUND, "Time Link 不存在");
        }
        return link;
    }
}
