package com.sism.analytics.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 分页参数归一化工具，避免控制器和服务层重复校验逻辑。
 */
final class AnalyticsPaginationSupport {

    static final int DEFAULT_PAGE_NUM = 1;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private AnalyticsPaginationSupport() {
    }

    static Pageable toPageable(int pageNum, int pageSize) {
        return PageRequest.of(normalizePageNum(pageNum) - 1, normalizePageSize(pageSize));
    }

    static int normalizePageNum(int pageNum) {
        return Math.max(pageNum, DEFAULT_PAGE_NUM);
    }

    static int normalizePageSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
    }
}
