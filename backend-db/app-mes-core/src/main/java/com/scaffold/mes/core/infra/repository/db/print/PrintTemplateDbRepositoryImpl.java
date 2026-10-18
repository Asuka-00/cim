package com.scaffold.mes.core.infra.repository.db.print;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scaffold.mes.core.domain.entity.print.PrintTemplate;
import com.scaffold.mes.core.domain.repository.print.PrintTemplateRepository;
import com.scaffold.mes.core.infra.mapper.db.print.PrintTemplateMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * PrintTemplateDbRepositoryImpl 打印模板仓储实现
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class PrintTemplateDbRepositoryImpl implements PrintTemplateRepository {

    private final PrintTemplateMapper mapper;

    @Override
    public Optional<PrintTemplate> findByCode(String tplCode) {
        return new DbRepositorySupport<>(mapper, PrintTemplate.class)
                .findOne(new LambdaQueryWrapper<PrintTemplate>().eq(PrintTemplate::getTplCode, tplCode));
    }

    @Override
    public Optional<PrintTemplate> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id));
    }

    @Override
    public Optional<PrintTemplate> findDefault(String tplType) {
        return new DbRepositorySupport<>(mapper, PrintTemplate.class).findOne(new LambdaQueryWrapper<PrintTemplate>()
                .eq(PrintTemplate::getTplType, tplType)
                .eq(PrintTemplate::getIsDefault, 1)
                .eq(PrintTemplate::getEnabled, 1));
    }

    @Override
    public List<PrintTemplate> listByType(String tplType) {
        return new DbRepositorySupport<>(mapper, PrintTemplate.class)
                .queryAll(new LambdaQueryWrapper<PrintTemplate>().eq(PrintTemplate::getTplType, tplType));
    }

    @Override
    public void save(PrintTemplate template) {
        new DbRepositorySupport<>(mapper, PrintTemplate.class).save(template);
    }

    @Override
    public void update(PrintTemplate template) {
        new DbRepositorySupport<>(mapper, PrintTemplate.class).update(template);
    }
}
