package com.sism.workflow.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int pageNum;
    private int pageSize;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> items, long total, int pageNum, int pageSize) {
        int safePageSize = Math.max(pageSize, 1);
        int safePageNum = Math.max(pageNum, 1);
        int totalPages = total <= 0 ? 0 : (int) Math.ceil((double) total / safePageSize);
        return PageResult.<T>builder()
                .items(items != null ? items : List.of())
                .total(total)
                .pageNum(safePageNum)
                .pageSize(safePageSize)
                .totalPages(totalPages)
                .build();
    }
}
