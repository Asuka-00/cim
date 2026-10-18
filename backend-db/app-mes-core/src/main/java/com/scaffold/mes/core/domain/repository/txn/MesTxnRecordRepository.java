package com.scaffold.mes.core.domain.repository.txn;

import java.util.Optional;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.txn.MesTxnRecord;

/**
 * MesTxnRecordRepository 事务流水仓储
 * <p>
 * 只插入与查询,不更新历史行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesTxnRecordRepository {

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 记录,不存在时为空
     */
    Optional<MesTxnRecord> findById(Long id);

    /**
     * 查询已成功的幂等流水。
     *
     * @param idemKey 幂等键,格式为事务类型加请求号
     * @return 成功流水,不存在时为空
     */
    Optional<MesTxnRecord> findSuccessByIdemKey(String idemKey);

    /**
     * 分页查询事务流水。
     *
     * @param page 页码,从 1 开始
     * @param pageSize 每页条数
     * @param txnType 事务类型,空表示不限
     * @param objectType 对象类型,空表示不限
     * @param objectId 对象主键,空表示不限
     * @return 分页结果
     */
    PageDTO<MesTxnRecord> page(int page, int pageSize, String txnType, String objectType, Long objectId);

    /**
     * 插入记录。
     *
     * @param record 待插入对象
     */
    void save(MesTxnRecord record);
}
