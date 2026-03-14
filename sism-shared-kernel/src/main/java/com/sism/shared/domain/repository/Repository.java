package com.sism.shared.domain.repository;

/**
 * Combined Repository interface for backward compatibility
 * Extends both ReadRepository and WriteRepository
 */
public interface Repository<T, ID> extends ReadRepository<T, ID>, WriteRepository<T, ID> {
    // Combined interface for backward compatibility
}
