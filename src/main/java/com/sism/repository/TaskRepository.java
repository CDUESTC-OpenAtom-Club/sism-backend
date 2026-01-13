package com.sism.repository;

import com.sism.entity.StrategicTask;
import com.sism.enums.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for StrategicTask entity
 * Provides data access methods for strategic task management
 */
@Repository
public interface TaskRepository extends JpaRepository<StrategicTask, Long> {

    /**
     * Find all tasks by assessment cycle ID
     */
    List<StrategicTask> findByCycle_CycleId(Long cycleId);

    /**
     * Find all tasks by organization ID
     */
    List<StrategicTask> findByOrg_OrgId(Long orgId);

    /**
     * Find all tasks by task type
     */
    List<StrategicTask> findByTaskType(TaskType taskType);

    /**
     * Find all tasks by cycle ID and organization ID
     */
    List<StrategicTask> findByCycle_CycleIdAndOrg_OrgId(Long cycleId, Long orgId);

    /**
     * Find all tasks by cycle ID ordered by sort order
     */
    List<StrategicTask> findByCycle_CycleIdOrderBySortOrderAsc(Long cycleId);

    /**
     * Find all tasks by cycle ID and task type
     */
    List<StrategicTask> findByCycle_CycleIdAndTaskType(Long cycleId, TaskType taskType);

    /**
     * Find tasks created by a specific organization
     */
    List<StrategicTask> findByCreatedByOrg_OrgId(Long createdByOrgId);

    /**
     * Count tasks by cycle ID
     */
    long countByCycle_CycleId(Long cycleId);

    /**
     * Find tasks by cycle ID and organization hierarchy
     * Returns tasks where the organization or its descendants are involved
     */
    @Query("SELECT DISTINCT t FROM StrategicTask t " +
           "WHERE t.cycle.cycleId = :cycleId " +
           "AND (t.org.orgId = :orgId OR t.org.parentOrg.orgId = :orgId)")
    List<StrategicTask> findByCycleAndOrgHierarchy(@Param("cycleId") Long cycleId, 
                                                     @Param("orgId") Long orgId);

    /**
     * Search tasks by name or description
     */
    @Query("SELECT t FROM StrategicTask t WHERE " +
           "t.taskName LIKE %:keyword% OR t.taskDesc LIKE %:keyword%")
    List<StrategicTask> searchByKeyword(@Param("keyword") String keyword);
}
