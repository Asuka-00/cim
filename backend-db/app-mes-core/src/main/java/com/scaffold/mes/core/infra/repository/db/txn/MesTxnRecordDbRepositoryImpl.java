package com.scaffold.mes.core.infra.repository.db.txn;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.core.domain.entity.txn.MesTxnRecord;
import com.scaffold.mes.core.domain.repository.txn.MesTxnRecordRepository;
import com.scaffold.mes.core.infra.mapper.db.txn.MesTxnRecordMapper;
import com.scaffold.share.infra.repository.db.AppendOnlyRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MesTxnRecordDbRepositoryImpl 事务流水仓储实现
 * <p>
 * 只增表,不提供更新。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MesTxnRecordDbRepositoryImpl implements MesTxnRecordRepository {

    private final MesTxnRecordMapper mapper;

    private AppendOnlyRepositorySupport<MesTxnRecord> support() {
        return new AppendOnlyRepositorySupport<>(mapper, MesTxnRecord.class);
    }

    @Override
    public Optional<MesTxnRecord> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<MesTxnRecord> findSuccessByIdemKey(String idemKey) {
        return support().findOne(new LambdaQueryWrapper<MesTxnRecord>()
                .eq(MesTxnRecord::getIdemKey, idemKey)
                .eq(MesTxnRecord::getResult, "SUCCESS"));
    }

    @Override
    public PageDTO<MesTxnRecord> page(int page, int pageSize, String txnType, String objectType, Long objectId) {
        LambdaQueryWrapper<MesTxnRecord> wrapper = support().wrapper();
        if (txnType != null && !txnType.isBlank()) {
            wrapper.eq(MesTxnRecord::getTxnType, txnType);
        }
        if (objectType != null && !objectType.isBlank()) {
            wrapper.eq(MesTxnRecord::getObjectType, objectType);
        }
        if (objectId != null) {
            wrapper.eq(MesTxnRecord::getObjectId, objectId);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(MesTxnRecord record) {
        support().save(record);
    }
}
