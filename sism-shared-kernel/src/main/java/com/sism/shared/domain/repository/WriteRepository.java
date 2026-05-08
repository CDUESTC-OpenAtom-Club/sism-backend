package com.sism.shared.domain.repository;

import java.util.List;

/**
 * Write Repository interface for CQRS pattern
 * Defines all write operations
 */
public interface WriteRepository<T, ID> {
    
    /**
     * Save entity (insert or update)
     */
    T save(T entity);
    
    /**
     * Save all entities
     */
    List<T> saveAll(List<T> entities);
    
    /**
     * Delete entity
     */
    void delete(T entity);
    
    /**
     * Delete entity by ID
     */
    void deleteById(ID id);
    
    /**
     * Delete all entities
     */
    void deleteAll();
    
    /**
     * Delete all given entities
     */
    void deleteAll(List<T> entities);
    
    /**
     * Flush all pending changes to database
     */
    void flush();
    
    /**
     * Clear persistence context
     */
    void clear();
    
    /**
     * Refresh entity from database
     */
    void refresh(T entity);
}
