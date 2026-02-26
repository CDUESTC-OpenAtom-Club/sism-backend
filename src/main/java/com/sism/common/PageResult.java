package com.sism.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Pagination result wrapper
 * Provides consistent pagination format across all endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    
    /**
     * List of items in current page
     */
    private List<T> items;
    
    /**
     * Total number of items across all pages
     */
    private long total;
    
    /**
     * Current page number (0-based)
     */
    private int page;
    
    /**
     * Number of items per page
     */
    private int pageSize;
    
    /**
     * Total number of pages
     */
    private int totalPages;
    
    /**
     * Whether this is the first page
     */
    private boolean first;
    
    /**
     * Whether this is the last page
     */
    private boolean last;
    
    /**
     * Create PageResult from Spring Data Page object
     */
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
    
    /**
     * Create PageResult from list and pagination info
     */
    public static <T> PageResult<T> of(List<T> items, long total, int page, int pageSize) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        return new PageResult<>(
                items,
                total,
                page,
                pageSize,
                totalPages,
                page == 0,
                page >= totalPages - 1
        );
    }
    
    /**
     * Constructor with basic pagination info (calculates totalPages, first, last automatically)
     */
    public PageResult(List<T> items, long total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        this.first = page == 0;
        this.last = page >= this.totalPages - 1;
    }
}
