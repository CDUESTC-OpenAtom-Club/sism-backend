package com.sism.analytics.infrastructure.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

final class AnalyticsRepositoryPagingSupport {

    static final int DEFAULT_FETCH_SIZE = 1000;

    private AnalyticsRepositoryPagingSupport() {
    }

    static Pageable firstPage() {
        return PageRequest.of(0, DEFAULT_FETCH_SIZE);
    }

    static <T> List<T> contentOf(Page<T> page) {
        return page.getContent();
    }
}
