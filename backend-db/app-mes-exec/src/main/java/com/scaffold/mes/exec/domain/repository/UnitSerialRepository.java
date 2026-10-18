package com.scaffold.mes.exec.domain.repository;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.exec.domain.entity.UnitSerial;

/**
 * UnitSerialRepository Unit 序列号仓储
 * <p>
 * 读写 mes_unit_serial。序列号在租户内唯一。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface UnitSerialRepository {

    /**
     * 按序列号查询。
     *
     * @param serialNo 序列号
     * @return 记录，不存在时为空
     */
    Optional<UnitSerial> findByNo(String serialNo);

    /**
     * 列出某个 Lot 的序列号。
     *
     * @param lotId Lot
     * @return 按序列号排列
     */
    List<UnitSerial> listByLot(Long lotId);

    /**
     * 插入。
     *
     * @param serial 序列号
     */
    void save(UnitSerial serial);
}
