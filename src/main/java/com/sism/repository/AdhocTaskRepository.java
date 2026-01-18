package com.sism.repository;

import com.sism.entity.AdhocTask;
import com.sism.enums.AdhocScopeType;
import com.sism.enums.AdhocTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for AdhocTask entity
 * Provides data access methods for adhoc task management with pagination support
 */
@Repository
public interface AdhocTaskRepository extends JpaRepository<AdhocTask, Long> {

    /**
     * Find all adhoc tasks by assessment cycle ID
     */
    List<AdhocTask> findByCycle_CycleId(Long cycleId);

    /**
     * Find all adhoc tasks by assessment cycle ID with pagination
     */
    Page<AdhocTask> findByCycle_CycleId(Long cycleId, Pageable pageable);

    /**
     * Find all adhoc tasks by creator organization ID
     */
    List<AdhocTask> findByCreatorOrg_OrgId(Long creatorOrgId);

    /**
     * Find all adhoc tasks by creator organization ID with pagination
     */
    Page<AdhocTask> findByCreatorOrg_OrgId(Long creatorOrgId, Pageable pageable);

    /**
     * Find all adhoc tasks by scope type
     */
    List<AdhocTask> findByScopeType(AdhocScopeType scopeType);

    /**
     * Find all adhoc tasks by status
     */
    List<AdhocTask> findByStatus(AdhocTaskStatus status);

    /**
     * Find all adhoc tasks by status with pagination
     */
    Page<AdhocTask> findByStatus(AdhocTaskStatus status, Pageable pageable);

    /**
     * Find all adhoc tasks by cycle ID and status
     */
    List<AdhocTask> findByCycle_CycleIdAndStatus(Long cycleId, AdhocTaskStatus status);

    /**
     * Find all adhoc tasks by indicator ID
     */
    List<AdhocTask> findByIndicator_IndicatorId(Long indicatorId);

    /**
     * Find adhoc tasks that include in alert
     */
    List<AdhocTask> findByIncludeInAlertTrue();

    /**
     * Find adhoc tasks by cycle ID that include in alert
     */
    List<AdhocTask> findByCycle_CycleIdAndIncludeInAlertTrue(Long cycleId);

    /**
     * Find adhoc tasks by due date range
     */
    List<AdhocTask> findByDueAtBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find adhoc tasks by due date range with pagination
     */
    Page<AdhocTask> findByDueAtBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    /**
     * Count adhoc tasks by cycle ID
     */
    long countByCycle_CycleId(Long cycleId);

    /**
     * Count adhoc tasks by status
     */
    long countByStatus(AdhocTaskStatus status);

    /**
     * Count adhoc tasks by creator organization ID
     */
    long countByCreatorOrg_OrgId(Long creatorOrgId);

    /**
     * Find adhoc tasks by multiple statuses with pagination
     */
    @Query("SELECT at FROM AdhocTask at WHERE at.status IN :statuses ORDER BY at.createdAt DESC")
    Page<AdhocTask> findByStatusIn(@Param("statuses") List<AdhocTaskStatus> statuses, Pageable pageable);

    /**
     * Find overdue adhoc tasks (due date passed and not closed/archived)
     */
    @Query("SELECT at FROM AdhocTask at WHERE at.dueAt < :currentDate " +
           "AND at.status NOT IN (com.sism.enums.AdhocTaskStatus.CLOSED, com.sism.enums.AdhocTaskStatus.ARCHIVED)")
    List<AdhocTask> findOverdueTasks(@Param("currentDate") LocalDate currentDate);

    /**
     * Find upcoming adhoc tasks (due within specified date range)
     */
    @Query("SELECT at FROM AdhocTask at WHERE at.dueAt BETWEEN :startDate AND :endDate " +
           "AND at.status NOT IN (com.sism.enums.AdhocTaskStatus.CLOSED, com.sism.enums.AdhocTaskStatus.ARCHIVED)")
    List<AdhocTask> findUpcomingTasks(@Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);

    /**
     * Search adhoc tasks by title or description
     */
    @Query("SELECT at FROM AdhocTask at WHERE at.taskTitle LIKE %:keyword% " +
           "OR at.taskDesc LIKE %:keyword%")
    List<AdhocTask> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Find adhoc tasks by cycle and scope type
     */
    @Query("SELECT at FROM AdhocTask at WHERE at.cycle.cycleId = :cycleId " +
           "AND at.scopeType = :scopeType")
    List<AdhocTask> findByCycleAndScopeType(@Param("cycleId") Long cycleId, 
                                            @Param("scopeType") AdhocScopeType scopeType);
}
