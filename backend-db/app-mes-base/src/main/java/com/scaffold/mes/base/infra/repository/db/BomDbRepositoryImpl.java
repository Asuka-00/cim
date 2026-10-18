package com.scaffold.mes.base.infra.repository.db;

import java.sql.SQLException;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.entity.Bom;
import com.scaffold.mes.base.domain.repository.BomRepository;
import com.scaffold.mes.base.infra.mapper.db.BomMapper;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * BomDbRepositoryImpl BOM 仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_bom。生效前用 FOR UPDATE NOWAIT 锁主表行。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class BomDbRepositoryImpl implements BomRepository {

    private final BomMapper mapper;

    private DbRepositorySupport<Bom> support() {
        return new DbRepositorySupport<>(mapper, Bom.class);
    }

    @Override
    public Optional<Bom> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Bom> findByCode(String bomCode) {
        return support().findOne(new LambdaQueryWrapper<Bom>().eq(Bom::getBomCode, bomCode));
    }

    @Override
    public PageDTO<Bom> page(int page, int pageSize, String bomCode) {
        LambdaQueryWrapper<Bom> wrapper = support().wrapper();
        if (bomCode != null && !bomCode.isBlank()) {
            wrapper.like(Bom::getBomCode, bomCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Bom bom) {
        support().save(bom);
    }

    @Override
    public void update(Bom bom) {
        support().update(bom);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }

    @Override
    public void lock(Long id) {
        try {
            Bom locked = mapper.selectOne(new LambdaQueryWrapper<Bom>().eq(Bom::getId, id).last("FOR UPDATE NOWAIT"));
            if (locked == null) {
                throw new BusinessException(MesBaseErrorCodes.BOM_NOT_FOUND, "BOM 不存在");
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            BusinessException busy = lockConflict(ex);
            if (busy != null) {
                throw busy;
            }
            throw ex;
        }
    }

    private static BusinessException lockConflict(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof SQLException sql) {
                if (sql.getErrorCode() == 3572) {
                    return new BusinessException(400102, "对象被占用");
                }
                if (sql.getErrorCode() == 1205) {
                    return new BusinessException(400103, "锁等待超时");
                }
            }
            current = current.getCause();
        }
        return null;
    }
}
