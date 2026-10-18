package com.scaffold.mes.core.api.v1.txn;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.MesErrorCodes;
import com.scaffold.mes.core.domain.entity.txn.MesTxnRecord;
import com.scaffold.mes.core.domain.repository.txn.MesTxnRecordRepository;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.vo.Result;

import lombok.RequiredArgsConstructor;

/**
 * MesTxnControllerV1 事务流水接口
 * <p>
 * 查询统一事务层写下的成功与失败流水。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@RestController
@RequestMapping("/api/v1/mes/transactions")
@RequiredArgsConstructor
public class MesTxnControllerV1 {

    private final MesTxnRecordRepository txnRepository;

    /**
     * 分页查询事务流水。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param txnType 事务类型,可空
     * @param objectType 对象类型,可空
     * @param objectId 对象主键,可空
     * @return 流水分页
     */
    @GetMapping
    public Result<PageDTO<MesTxnRecord>> page(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String txnType,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) Long objectId) {
        return Result.success(txnRepository.page(page, pageSize, txnType, objectType, objectId));
    }

    /**
     * 查询事务详情。
     *
     * @param id 流水主键
     * @return 流水
     * @throws BusinessException 事务不存在
     */
    @GetMapping("/{id}")
    public Result<MesTxnRecord> detail(@PathVariable Long id) {
        return Result.success(txnRepository.findById(id)
                .orElseThrow(() -> new BusinessException(MesErrorCodes.TXN_NOT_FOUND, "事务不存在")));
    }
}
