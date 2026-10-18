package com.scaffold.mes.base.domain.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.EcnApproveRequest;
import com.scaffold.mes.base.domain.dto.EcnCreateRequest;
import com.scaffold.mes.base.domain.entity.Ecn;
import com.scaffold.mes.base.domain.repository.EcnRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * EcnApplicationService 变更申请
 * <p>
 * 新建时状态固定为 DRAFT。只有草稿可以提交或取消。只有评审中可以通过或驳回。只有已批准可以生效。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class EcnApplicationService {

    private static final Set<String> TYPES = Set.of("ROUTE", "RECIPE", "BOM", "PRODUCT", "OTHER");

    private final EcnRepository repository;
    private final Clock clock;

    /**
     * 列出全部申请。
     *
     * @return 按编号排列
     */
    public List<Ecn> list() {
        return repository.list();
    }

    /**
     * 新建草稿。
     *
     * @param request 编号、标题、类型和原因
     * @throws BusinessException 类型不合法，或编号已存在
     */
    @Transactional
    public void create(EcnCreateRequest request) {
        if (!TYPES.contains(request.getChangeType())) {
            throw new BusinessException(MesBaseErrorCodes.ECN_INVALID, "变更类型不合法");
        }
        if (repository.findByNo(request.getEcnNo()).isPresent()) {
            throw new BusinessException(MesBaseErrorCodes.ECN_INVALID, "ECN 编号已存在");
        }
        Ecn ecn = new Ecn();
        ecn.setEcnNo(request.getEcnNo());
        ecn.setTitle(request.getTitle());
        ecn.setChangeType(request.getChangeType());
        ecn.setReason(request.getReason());
        ecn.setApplicant(request.getApplicant());
        ecn.setStatus("DRAFT");
        repository.save(ecn);
    }

    /**
     * 把草稿提交成评审中。
     *
     * @param id 申请
     * @throws BusinessException 申请不存在，或不是草稿
     */
    @Transactional
    public void submit(Long id) {
        Ecn ecn = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ECN_STATUS, "变更申请不存在"));
        if (!"DRAFT".equals(ecn.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ECN_STATUS, "只有草稿可以提交评审");
        }
        ecn.setStatus("REVIEWING");
        repository.update(ecn);
    }

    /**
     * 批准评审中的申请。
     *
     * @param id 申请
     * @param request 审批人
     * @throws BusinessException 申请不存在，不是评审中，或没写审批人
     */
    @Transactional
    public void approve(Long id, EcnApproveRequest request) {
        if (request.getApprover() == null || request.getApprover().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.ECN_STATUS, "批准必须填写审批人");
        }
        Ecn ecn = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ECN_STATUS, "变更申请不存在"));
        if (!"REVIEWING".equals(ecn.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ECN_STATUS, "只有评审中的申请可以通过");
        }
        ecn.setStatus("APPROVED");
        ecn.setApprover(request.getApprover());
        repository.update(ecn);
    }

    /**
     * 驳回评审中的申请。
     *
     * @param id 申请
     * @param request 审批人
     * @throws BusinessException 申请不存在，不是评审中，或没写审批人
     */
    @Transactional
    public void reject(Long id, EcnApproveRequest request) {
        if (request.getApprover() == null || request.getApprover().isBlank()) {
            throw new BusinessException(MesBaseErrorCodes.ECN_STATUS, "驳回必须填写审批人");
        }
        Ecn ecn = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ECN_STATUS, "变更申请不存在"));
        if (!"REVIEWING".equals(ecn.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ECN_STATUS, "只有评审中的申请可以驳回");
        }
        ecn.setStatus("REJECTED");
        ecn.setApprover(request.getApprover());
        repository.update(ecn);
    }

    /**
     * 让已批准的申请生效。
     *
     * @param id 申请
     * @throws BusinessException 申请不存在，或不是已批准
     */
    @Transactional
    public void effect(Long id) {
        Ecn ecn = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ECN_STATUS, "变更申请不存在"));
        if (!"APPROVED".equals(ecn.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ECN_STATUS, "只有已批准的申请可以生效");
        }
        ecn.setStatus("EFFECTIVE");
        ecn.setEffectiveAt(LocalDateTime.now(clock));
        repository.update(ecn);
    }

    /**
     * 取消草稿。
     *
     * @param id 申请
     * @throws BusinessException 申请不存在，或不是草稿
     */
    @Transactional
    public void cancel(Long id) {
        Ecn ecn = repository.findById(id)
                .orElseThrow(() -> new BusinessException(MesBaseErrorCodes.ECN_STATUS, "变更申请不存在"));
        if (!"DRAFT".equals(ecn.getStatus())) {
            throw new BusinessException(MesBaseErrorCodes.ECN_STATUS, "只有草稿可以取消");
        }
        ecn.setStatus("CANCELLED");
        repository.update(ecn);
    }
}
