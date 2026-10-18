package com.scaffold.mes.core.domain.repository.file;

import java.util.List;
import java.util.Optional;

import com.scaffold.mes.core.domain.entity.file.MesFile;

/**
 * MesFileRepository 文件元数据仓储
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-16
 */
public interface MesFileRepository {

    /**
     * 插入。
     *
     * @param file 文件
     */
    void save(MesFile file);

    /**
     * 列出挂在这个 Lot 上的文件。
     *
     * @param lotId Lot
     * @return 文件，没有时为空列表
     */
    List<MesFile> listByLot(Long lotId);

    /**
     * 按主键查询。
     *
     * @param id 文件
     * @return 文件，不存在时为空
     */
    Optional<MesFile> findById(Long id);
}
