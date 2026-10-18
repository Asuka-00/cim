package com.scaffold.mes.exec.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.exec.domain.MesExecErrorCodes;
import com.scaffold.mes.exec.domain.entity.Wafer;
import com.scaffold.mes.exec.domain.entity.WaferHistory;
import com.scaffold.mes.exec.domain.repository.WaferHistoryRepository;
import com.scaffold.mes.exec.domain.repository.WaferRepository;
import com.scaffold.share.exception.BusinessException;

import lombok.RequiredArgsConstructor;

/**
 * WaferQueryApplicationService Wafer 查询
 * <p>
 * 分页、单片和历史都只读。不存在的片不能查历史。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Service
@RequiredArgsConstructor
public class WaferQueryApplicationService {

    private final WaferRepository waferRepository;
    private final WaferHistoryRepository historyRepository;

    /**
     * 分页查询。
     *
     * @param page 页码，从 1 开始
     * @param pageSize 每页条数
     * @param waferCode 内部编号模糊条件，可空
     * @param lotId Lot，可空
     * @return 分页结果
     */
    public PageDTO<Wafer> page(int page, int pageSize, String waferCode, Long lotId) {
        return waferRepository.page(page, pageSize, waferCode, lotId);
    }

    /**
     * 按主键查询。
     *
     * @param id Wafer
     * @return Wafer
     * @throws BusinessException 片不存在
     */
    public Wafer get(Long id) {
        return require(id);
    }

    /**
     * 列出历史，新的在前。
     *
     * @param id Wafer
     * @return 历史
     * @throws BusinessException 片不存在
     */
    public List<WaferHistory> history(Long id) {
        require(id);
        return historyRepository.listByWafer(id);
    }

    private Wafer require(Long id) {
        return waferRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MesExecErrorCodes.WAFER_NOT_FOUND, "Wafer 不存在"));
    }
}
