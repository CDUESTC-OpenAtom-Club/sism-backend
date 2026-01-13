package com.sism.repository;

import com.sism.entity.AssessmentCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for AssessmentCycle entity
 * Provides data access methods for assessment cycle management
 */
@Repository
public interface AssessmentCycleRepository extends JpaRepository<AssessmentCycle, Long> {

    /**
     * Find assessment cycle by year
     */
    Optional<AssessmentCycle> findByYear(Integer year);

    /**
     * Find assessment cycle by name
     */
    Optional<AssessmentCycle> findByCycleName(String cycleName);

    /**
     * Find all assessment cycles by year range
     */
    List<AssessmentCycle> findByYearBetween(Integer startYear, Integer endYear);

    /**
     * Check if assessment cycle exists for a given year
     */
    boolean existsByYear(Integer year);

    /**
     * Check if assessment cycle exists by name
     */
    boolean existsByCycleName(String cycleName);

    /**
     * Find assessment cycles ordered by year descending
     */
    List<AssessmentCycle> findAllByOrderByYearDesc();

    /**
     * Find assessment cycle that contains a specific date
     */
    @Query("SELECT ac FROM AssessmentCycle ac WHERE :date BETWEEN ac.startDate AND ac.endDate")
    Optional<AssessmentCycle> findByDateInRange(@Param("date") LocalDate date);

    /**
     * Find active assessment cycles (current or future)
     */
    @Query("SELECT ac FROM AssessmentCycle ac WHERE ac.endDate >= :currentDate ORDER BY ac.year DESC")
    List<AssessmentCycle> findActiveOrFutureCycles(@Param("currentDate") LocalDate currentDate);
}
