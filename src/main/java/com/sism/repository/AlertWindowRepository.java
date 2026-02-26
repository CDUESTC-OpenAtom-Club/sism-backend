package com.sism.repository;

import com.sism.entity.AlertWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AlertWindow entity
 * Provides data access methods for alert window management
 */
@Repository
public interface AlertWindowRepository extends JpaRepository<AlertWindow, Long> {

    /**
     * Find all alert windows by assessment cycle ID
     */
    List<AlertWindow> findByCycle_CycleId(Long cycleId);

    /**
     * Find all alert windows by assessment cycle ID ordered by cutoff date
     */
    List<AlertWindow> findByCycle_CycleIdOrderByCutoffDateAsc(Long cycleId);

    /**
     * Find the default alert window for a cycle
     */
    Optional<AlertWindow> findByCycle_CycleIdAndIsDefaultTrue(Long cycleId);

    /**
     * Find alert windows by cutoff date range
     */
    List<AlertWindow> findByCutoffDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find alert windows by name
     */
    Optional<AlertWindow> findByName(String name);

    /**
     * Check if alert window exists by name
     */
    boolean existsByName(String name);

    /**
     * Find alert windows with cutoff date on or after a specific date
     */
    @Query("SELECT aw FROM AlertWindow aw WHERE aw.cutoffDate >= :date ORDER BY aw.cutoffDate ASC")
    List<AlertWindow> findUpcomingWindows(@Param("date") LocalDate date);

    /**
     * Find alert windows with cutoff date on a specific date
     */
    List<AlertWindow> findByCutoffDate(LocalDate cutoffDate);

    /**
     * Find alert windows by cycle and date range
     */
    @Query("SELECT aw FROM AlertWindow aw WHERE aw.cycle.cycleId = :cycleId " +
           "AND aw.cutoffDate BETWEEN :startDate AND :endDate")
    List<AlertWindow> findByCycleAndDateRange(@Param("cycleId") Long cycleId,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * Count alert windows by cycle ID
     */
    long countByCycle_CycleId(Long cycleId);
}
