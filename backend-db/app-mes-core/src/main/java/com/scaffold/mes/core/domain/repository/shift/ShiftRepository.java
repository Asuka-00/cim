package com.scaffold.mes.core.domain.repository.shift;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.core.domain.entity.shift.Shift;

/**
 * ShiftRepository 班次仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface ShiftRepository {

    /**
     * 按编码查询。
     *
     * @param shiftCode 班次编码
     * @return 班次，不存在时为空
     */
    Optional<Shift> findByCode(String shiftCode);

    /**
     * 列出全部班次，按编码。
     *
     * @return 班次
     */
    List<Shift> list();

    /**
     * 插入。
     *
     * @param shift 班次
     */
    void save(Shift shift);
}
