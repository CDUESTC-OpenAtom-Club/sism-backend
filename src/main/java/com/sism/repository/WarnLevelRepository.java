package com.sism.repository;

import com.sism.entity.WarnLevel;
import com.sism.enums.AlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for WarnLevel entity
 */
@Repository
public interface WarnLevelRepository extends JpaRepository<WarnLevel, Long> {

    /**
     * Find warning level by level code
     */
    Optional<WarnLevel> findByLevelCode(String levelCode);

    /**
     * Find warning levels by severity
     */
    List<WarnLevel> findBySeverity(AlertSeverity severity);

    /**
     * Find active warning levels
     */
    List<WarnLevel> findByIsActiveTrue();
    
    /**
     * Find warning levels by active status
     */
    List<WarnLevel> findByIsActive(Boolean isActive);

    /**
     * Find warning levels by severity and active status
     */
    List<WarnLevel> findBySeverityAndIsActiveTrue(AlertSeverity severity);

    /**
     * Check if level code exists
     */
    boolean existsByLevelCode(String levelCode);
}
