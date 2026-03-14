package com.sism.shared.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

/**
 * Read Repository interface for CQRS pattern
 * Defines all read operations
 */
public interface ReadRepository<T, ID> {
    
    /**
     * Find entity by ID
     */
    Optional<T> findById(ID id);
    
    /**
     * Find all entities
     */
    List<T> findAll();
    
    /**
     * Find all entities with sort
     */
    List<T> findAll(Sort sort);
    
    /**
     * Find all entities matching specification
     */
    List<T> findAll(Specification<T> spec);
    
    /**
     * Find all entities with pagination
     */
    Page<T> findAll(Pageable pageable);
    
    /**
     * Find all entities matching specification with pagination
     */
    Page<T> findAll(Specification<T> spec, Pageable pageable);
    
    /**
     * Count all entities
     */
    long count();
    
    /**
     * Count entities matching specification
     */
    long count(Specification<T> spec);
    
    /**
     * Check if entity exists
     */
    boolean existsById(ID id);
    
    /**
     * Check if entity exists matching specification
     */
    boolean exists(Specification<T> spec);
    
    /**
     * Find single result matching specification
     */
    Optional<T> findOne(Specification<T> spec);
}
