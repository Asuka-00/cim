package com.scaffold.mes.base.infra.repository.db;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.entity.Material;
import com.scaffold.mes.base.domain.repository.MaterialRepository;
import com.scaffold.mes.base.infra.mapper.db.MaterialMapper;
import com.scaffold.share.infra.repository.db.DbRepositorySupport;

import lombok.RequiredArgsConstructor;

/**
 * MaterialDbRepositoryImpl 物料仓储实现
 * <p>
 * 基于 MyBatis-Plus 访问 mes_material。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
@Repository
@RequiredArgsConstructor
public class MaterialDbRepositoryImpl implements MaterialRepository {

    private final MaterialMapper mapper;

    private DbRepositorySupport<Material> support() {
        return new DbRepositorySupport<>(mapper, Material.class);
    }

    @Override
    public Optional<Material> findById(Long id) {
        return support().findById(id);
    }

    @Override
    public Optional<Material> findByCode(String materialCode) {
        return support().findOne(new LambdaQueryWrapper<Material>().eq(Material::getMaterialCode, materialCode));
    }

    @Override
    public PageDTO<Material> page(int page, int pageSize, String materialCode) {
        LambdaQueryWrapper<Material> wrapper = support().wrapper();
        if (materialCode != null && !materialCode.isBlank()) {
            wrapper.like(Material::getMaterialCode, materialCode);
        }
        return support().queryPage(page, pageSize, wrapper);
    }

    @Override
    public void save(Material material) {
        support().save(material);
    }

    @Override
    public void update(Material material) {
        support().update(material);
    }

    @Override
    public void deleteById(Long id) {
        support().deleteById(id);
    }

    @Override
    public long countReferences(Long id) {
        return mapper.countReferences(id);
    }
}
