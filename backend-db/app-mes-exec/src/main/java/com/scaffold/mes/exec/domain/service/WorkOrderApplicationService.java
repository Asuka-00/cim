package com.scaffold.mes.exec.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.dto.WorkOrderSaveRequest;
import com.scaffold.mes.exec.domain.entity.WorkOrder;
import com.scaffold.mes.exec.domain.repository.WorkOrderRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.ActiveProcessProvider;
import com.scaffold.share.integration.mes.base.response.FrozenProcess;

import lombok.RequiredArgsConstructor;

/**
 * WorkOrderApplicationService 工单应用服务
 * <p>
 * 创建时固化当时的 Route 和 BOM 版本。进度计数不在这里改。只有已创建的工单可以下达或取消。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WorkOrderApplicationService {

    private static final String CREATED = "CREATED";
    private static final String RELEASED = "RELEASED";
    private static final String CANCELLED = "CANCELLED";

    private final WorkOrderRepository repository;
    private final ActiveProcessProvider processProvider;
    private final WoMaterialRequirementApplicationService requirements;
    private final Clock clock;

    /**
     * 分页查询工单。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param woNo 工单号模糊条件，空表示不限
     * @return 分页结果
     */
    public PageDTO<WorkOrder> page(int page, int pageSize, String woNo) {
        return repository.page(page, pageSize, woNo);
    }

    /**
     * 创建工单，并固化产品版本当前的 Route、BOM。
     *
     * @param request 工单号、产品版本和计划量
     * @throws BusinessException 工单号重复、数量或优先级不合法，或工艺不能固化
     */
    @Transactional
    public void create(WorkOrderSaveRequest request) {
        if (repository.findByNo(request.getWoNo()).isPresent()) {
            throw new BusinessException(MesExecErrorCodes.WORK_ORDER_NO_EXISTS, "工单号已存在: " + request.getWoNo());
        }
        if (request.getPlanQty() == null || request.getPlanQty() <= 0) {
            throw new BusinessException(MesExecErrorCodes.WORK_ORDER_QTY_INVALID, "计划数量必须大于 0");
        }
        int priority = request.getPriority() == null ? 5 : request.getPriority();
        if (priority < 1 || priority > 9) {
            throw new BusinessException(MesExecErrorCodes.WORK_ORDER_PRIORITY_INVALID, "优先级必须在 1 到 9");
        }
        FrozenProcess frozen = processProvider.freeze(request.getProductVersionId());
        WorkOrder workOrder = new WorkOrder();
        workOrder.setWoNo(request.getWoNo());
        workOrder.setProductId(frozen.productId());
        workOrder.setProductVersionId(frozen.productVersionId());
        workOrder.setRouteVersionId(frozen.routeVersionId());
        workOrder.setBomVersionId(frozen.bomVersionId());
        workOrder.setCustomerId(frozen.customerId());
        workOrder.setPlanQty(request.getPlanQty());
        workOrder.setQtyUnitId(request.getQtyUnitId());
        workOrder.setPriority(priority);
        workOrder.setDueDate(request.getDueDate());
        workOrder.setStatus(CREATED);
        workOrder.setLotCount(0);
        workOrder.setStartedQty(0L);
        workOrder.setCompletedQty(0L);
        workOrder.setScrapQty(0L);
        workOrder.setTerminatedQty(0L);
        workOrder.setSource("MANUAL");
        workOrder.setRemark(request.getRemark());
        workOrder.setVersion(0);
        repository.save(workOrder);
    }

    /**
     * 下达工单。
     *
     * @param id 主键
     * @throws BusinessException 工单不存在或不是已创建
     */
    @Transactional
    public List<String> release(Long id) {
        WorkOrder workOrder = require(id);
        if (!CREATED.equals(workOrder.getStatus())) {
            throw new BusinessException(MesExecErrorCodes.WORK_ORDER_STATUS_INVALID, "只有已创建的工单可以下达");
        }
        workOrder.setStatus(RELEASED);
        workOrder.setReleasedAt(LocalDateTime.now(clock));
        repository.update(workOrder);
        return requirements.expand(workOrder);
    }

    /**
     * 取消工单。已开批的不能取消。
     *
     * @param id 主键
     * @throws BusinessException 工单不存在、不是已创建，或已经开批
     */
    @Transactional
    public void cancel(Long id) {
        WorkOrder workOrder = require(id);
        if (!CREATED.equals(workOrder.getStatus()) || (workOrder.getLotCount() != null && workOrder.getLotCount() > 0)) {
            throw new BusinessException(MesExecErrorCodes.WORK_ORDER_STATUS_INVALID, "只有未开批的已创建工单可以取消");
        }
        workOrder.setStatus(CANCELLED);
        workOrder.setClosedAt(LocalDateTime.now(clock));
        repository.update(workOrder);
    }

    private WorkOrder require(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WORK_ORDER_NOT_FOUND, "工单不存在"));
    }
}
