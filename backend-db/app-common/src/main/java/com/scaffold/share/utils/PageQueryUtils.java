package com.scaffold.share.utils;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;

import lombok.experimental.UtilityClass;

/**
 * PageQueryUtils 工具类
 * <p>
 * 统一处理内存过滤后的分页切片，避免各仓储重复组装 PageDTO
 */
@UtilityClass
public class PageQueryUtils {

    /**
     * 对内存中的结果集做分页切片
     * <p>
     * 适用于“先从缓存/内存查出候选集，再在 JVM 内做过滤”的场景，
     * 统一处理页码、页大小越界问题，避免每个仓储各写一套下标计算
     */
    public static <T> PageDTO<T> paginate(List<T> records, int page, int pageSize) {
        List<T> safeRecords = records == null ? List.of() : records;
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(pageSize, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, safeRecords.size());
        int toIndex = Math.min(fromIndex + safeSize, safeRecords.size());

        PageDTO<T> pageDTO = new PageDTO<>();
        // 页码超出范围时返回空记录，但 total/current/size 仍保留真实分页元数据
        pageDTO.setRecords(fromIndex < toIndex ? safeRecords.subList(fromIndex, toIndex) : List.of());
        pageDTO.setTotal(safeRecords.size());
        pageDTO.setSize(safeSize);
        pageDTO.setCurrent(safePage);
        return pageDTO;
    }

    /**
     * 转换分页记录类型，同时保留分页元数据
     */
    public static <S, T> PageDTO<T> map(PageDTO<S> page, Function<? super S, ? extends T> mapper) {
        Objects.requireNonNull(page, "page must not be null");
        Objects.requireNonNull(mapper, "mapper must not be null");

        List<S> records = page.getRecords() == null ? List.of() : page.getRecords();
        List<T> mappedRecords = records.stream()
                .map(mapper)
                .collect(Collectors.toList());

        PageDTO<T> pageDTO = new PageDTO<>();
        pageDTO.setRecords(mappedRecords);
        pageDTO.setTotal(page.getTotal());
        pageDTO.setSize(page.getSize());
        pageDTO.setCurrent(page.getCurrent());
        return pageDTO;
    }
}
