package com.scaffold.mes.exec.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Lot;
import com.scaffold.mes.exec.domain.entity.LotHistory;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.repository.LotHistoryRepository;
import com.scaffold.mes.exec.domain.repository.LotRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.integration.mes.base.provider.UserScopePort;

import lombok.RequiredArgsConstructor;

/**
 * LotQueryApplicationService Lot 查询
 * <p>
 * 分页、单批、历史和 Wafer 都只读。不存在的 Lot 拒绝。分页结果按当前用户的站点和区域范围过滤。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class LotQueryApplicationService {

    private final LotRepository lotRepository;
    private final LotHistoryRepository historyRepository;
    private final WaferRepository waferRepository;
    private final UserScopePort scopes;
    private final CurrentSessionProvider session;

    /**
     * 分页查询 Lot。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param lotNo Lot 号模糊条件，可空
     * @param status 状态，可空
     * @return 分页结果
     */
    public PageDTO<Lot> page(int page, int pageSize, String lotNo, String status) {
        PageDTO<Lot> result = lotRepository.page(page, pageSize, lotNo, status);
        String user = session.getCurrentUserNameOrDefault();
        List<Lot> records = result.getRecords() == null ? List.of() : result.getRecords();
        result.setRecords(records.stream()
                .filter(lot -> scopes.allowed(user, "STEP", lot.getStepId()))
                .filter(lot -> !"AREA".equals(lot.getLocationType())
                        || scopes.allowed(user, "AREA", lot.getLocationId()))
                .toList());
        return result;
    }

    /**
     * 按主键查询。
     *
     * @param id Lot
     * @return Lot
     * @throws BusinessException Lot 不存在
     */
    public Lot get(Long id) {
        return require(id);
    }

    /**
     * 列出历史，新的在前。
     *
     * @param id Lot
     * @return 历史
     * @throws BusinessException Lot 不存在
     */
    public List<LotHistory> history(Long id) {
        require(id);
        return historyRepository.listByLot(id);
    }

    /**
     * 列出片。
     *
     * @param id Lot
     * @return Wafer
     * @throws BusinessException Lot 不存在
     */
    public List<Wafer> wafers(Long id) {
        require(id);
        return waferRepository.listByLot(id);
    }

    private Lot require(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.LOT_NOT_FOUND, "Lot 不存在"));
    }
}
