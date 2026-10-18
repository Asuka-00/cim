package com.scaffold.share.infra.repository.db;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.share.context.CurrentSessionProvider;
import com.scaffold.share.entity.BaseEntity;
import com.scaffold.share.exception.BusinessException;
import com.scaffold.share.id.AppIdGenerator;
import com.scaffold.share.infra.mapper.BatchBaseMapper;

/**
 * DbRepositorySupport DB 热库仓储公共基座。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-05-28
 * @param <T> 实体类型
 */
public class DbRepositorySupport<T extends BaseEntity> {

    private final BatchBaseMapper<T> mapper;
    private final Class<T> entityClass;

    public DbRepositorySupport(BatchBaseMapper<T> mapper, Class<T> entityClass) {
        this.mapper = mapper;
        this.entityClass = entityClass;
    }

    public Optional<T> findById(Long id) {
        return Optional.ofNullable(id).map(mapper::selectById);
    }

    @SuppressWarnings("deprecation")
    public List<T> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return mapper.selectBatchIds(ids);
    }

    public List<T> findAll() {
        return queryAll(wrapper());
    }

    public void save(T entity) {
        mapper.insert(entity);
    }

    public void update(T entity) {
        mapper.updateById(entity);
    }

    /**
     * 带版本的更新。影响行数为 0 时抛出 400101(与 MesErrorCodes.CONCURRENT_MODIFICATION 相同)。
     * 实体带 {@code @Version} 时由乐观锁拦截器完成版本比对。
     */
    public void updateWithVersion(T entity) {
        if (mapper.updateById(entity) == 0) {
            throw new BusinessException(400101, "数据已被他人修改,请刷新后重试");
        }
    }

    public void deleteById(Long id) {
        if (id != null) {
            mapper.deleteById(id);
        }
    }

    public void batchSave(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        mapper.insertBatchSomeColumn(entities);
    }

    /**
     * 填充式批量插入：先在 Java 侧补齐主键、租户与审计字段（缺失才填），再走单条 SQL 批量插入。
     * <p>
     * {@code insertBatchSomeColumn} 不触发 MyBatis-Plus 的 ASSIGN_ID 与 MetaObjectHandler 自动填充，
     * 故由此方法显式预填，保证批量写入字段完整。
     *
     * @param entities 待写入实体
     * @param session  当前会话提供方，用于取租户与操作人
     */
    public void batchSaveWithFill(List<T> entities, CurrentSessionProvider session) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        fillForBatchInsert(entities, session);
        mapper.insertBatchSomeColumn(entities);
    }

    /**
     * 仅填充：为批量插入实体补齐主键、租户与审计字段（缺失才填），不落库。
     * <p>
     * 供"生产端先填好字段、再异步发消息由消费端原样批插"的链路复用（与 {@link #batchSaveWithFill} 同口径），
     * 因 {@code insertBatchSomeColumn} 不触发 ASSIGN_ID 与 MetaObjectHandler 自动填充。
     *
     * @param entities 待填充实体
     * @param session  当前会话提供方，用于取租户与操作人
     * @param <E>      实体类型
     */
    public static <E extends BaseEntity> void fillForBatchInsert(List<E> entities, CurrentSessionProvider session) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        String tenantId = session.getCurrentTenantIdOrDefault();
        String userName = session.getCurrentUserNameOrDefault();
        for (E entity : entities) {
            if (entity.getId() == null) {
                entity.setId(AppIdGenerator.nextId());
            }
            if (isBlankText(entity.getTenantId())) {
                entity.setTenantId(tenantId);
            }
            if (isBlankText(entity.getCreateBy())) {
                entity.setCreateBy(userName);
            }
            if (entity.getCreateAt() == null) {
                entity.setCreateAt(now);
            }
            if (isBlankText(entity.getUpdateBy())) {
                entity.setUpdateBy(userName);
            }
            if (entity.getUpdateAt() == null) {
                entity.setUpdateAt(now);
            }
        }
    }

    private static boolean isBlankText(String value) {
        return value == null || value.isBlank();
    }

    @SuppressWarnings("deprecation")
    public void batchDelete(Collection<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            mapper.deleteBatchIds(ids);
        }
    }

    public Optional<T> findOne(LambdaQueryWrapper<T> wrapper) {
        return Optional.ofNullable(mapper.selectOne(wrapper.last("LIMIT 1")));
    }

    public List<T> queryAll(LambdaQueryWrapper<T> wrapper) {
        return mapper.selectList(wrapper);
    }

    public PageDTO<T> queryPage(int page, int pageSize, LambdaQueryWrapper<T> wrapper) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        Page<T> result = mapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        PageDTO<T> pageDTO = new PageDTO<>();
        pageDTO.setRecords(result.getRecords());
        pageDTO.setTotal(result.getTotal());
        pageDTO.setSize(result.getSize());
        pageDTO.setCurrent(result.getCurrent());
        return pageDTO;
    }

    public LambdaQueryWrapper<T> wrapper() {
        return new LambdaQueryWrapper<T>(entityClass).orderByDesc(T::getCreateAt);
    }

    public QueryWrapper<T> queryWrapper() {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_at");
        return wrapper;
    }

    public <V> LambdaQueryWrapper<T> eq(LambdaQueryWrapper<T> wrapper, SFunction<T, V> column, V value) {
        if (value instanceof String text) {
            return wrapper.eq(!text.isBlank(), column, value);
        }
        return wrapper.eq(value != null, column, value);
    }

    public LambdaQueryWrapper<T> like(LambdaQueryWrapper<T> wrapper, SFunction<T, String> column, String value) {
        return wrapper.like(value != null && !value.isBlank(), column, value);
    }

    /**
     * 追加生效窗口过滤：from 为空或 from<=now，且 to 为空或 to>now。
     *
     * @param wrapper 待追加条件的查询包装器
     * @param from    生效起始时间列
     * @param to      生效结束时间列
     * @param now     当前时间基准
     * @return 追加条件后的同一查询包装器
     */
    public LambdaQueryWrapper<T> withinWindow(LambdaQueryWrapper<T> wrapper,
            SFunction<T, LocalDateTime> from, SFunction<T, LocalDateTime> to, LocalDateTime now) {
        return wrapper
                .and(w -> w.isNull(from).or().le(from, now))
                .and(w -> w.isNull(to).or().gt(to, now));
    }

    public boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
