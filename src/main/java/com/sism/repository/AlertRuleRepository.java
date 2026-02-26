package com.sism.repository;

import com.sism.entity.AlertRule;
import com.sism.enums.AlertSeverity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AlertRule entity
 * Provides data access methods for alert rule management
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * Find all alert rules by assessment cycle ID
     */
    List<AlertRule> findByCycle_CycleId(Long cycleId);

    /**
     * Find all enabled alert rules by cycle ID
     */
    List<AlertRule> findByCycle_CycleIdAndIsEnabledTrue(Long cycleId);

    /**
     * Find all alert rules by severity
     */
    List<AlertRule> findBySeverity(AlertSeverity severity);

    /**
     * Find all enabled alert rules by severity
     */
    List<AlertRule> findBySeverityAndIsEnabledTrue(AlertSeverity severity);

    /**
     * Find alert rule by name
     */
    Optional<AlertRule> findByName(String name);

    /**
     * Find all enabled alert rules
     */
    List<AlertRule> findByIsEnabledTrue();

    /**
     * Check if alert rule exists by name
     */
    boolean existsByName(String name);

    /**
     * Find alert rules by cycle ID and severity
     */
    List<AlertRule> findByCycle_CycleIdAndSeverity(Long cycleId, AlertSeverity severity);

    /**
     * Find enabled alert rules by cycle ID ordered by gap threshold
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.cycle.cycleId = :cycleId " +
           "AND ar.isEnabled = true ORDER BY ar.gapThreshold ASC")
    List<AlertRule> findEnabledByCycleOrderedByThreshold(@Param("cycleId") Long cycleId);

    /**
     * Find alert rules with gap threshold greater than or equal to specified value
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.gapThreshold >= :threshold " +
           "AND ar.isEnabled = true")
    List<AlertRule> findByGapThresholdGreaterThanEqual(@Param("threshold") BigDecimal threshold);

    /**
     * Find applicable alert rules for a given gap percentage
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.cycle.cycleId = :cycleId " +
           "AND ar.isEnabled = true AND ar.gapThreshold <= :gapPercent " +
           "ORDER BY ar.gapThreshold DESC")
    List<AlertRule> findApplicableRules(@Param("cycleId") Long cycleId, 
                                        @Param("gapPercent") BigDecimal gapPercent);

    /**
     * Count alert rules by cycle ID
     */
    long countByCycle_CycleId(Long cycleId);

    /**
     * Count enabled alert rules by cycle ID
     */
    long countByCycle_CycleIdAndIsEnabledTrue(Long cycleId);
}
