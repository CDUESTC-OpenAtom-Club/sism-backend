package com.sism.iam.application.service;

import org.springframework.data.domain.PageRequest;

/**
 * Unified pagination policy for IAM endpoints.
 */
public final class PaginationPolicy {

    public static final int MIN_PAGE = 0;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;

    private PaginationPolicy() {
    }

    public static int normalizePage(int page) {
        return Math.max(page, MIN_PAGE);
    }

    public static int normalizeSize(int size) {
        return Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
    }

    public static PageRequest toPageRequest(int page, int size) {
        return PageRequest.of(normalizePage(page), normalizeSize(size));
    }
}
