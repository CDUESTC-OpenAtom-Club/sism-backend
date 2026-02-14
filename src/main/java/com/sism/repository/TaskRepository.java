package com.sism.repository;

import com.sism.entity.StrategicTask;
import com.sism.enums.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<StrategicTask, Long> {

    /**
     * Find all tasks by assessment cycle ID
     */
    List<StrategicTask> findByCycleId(Long cycleId);

    /**
     * Find all tasks by organization ID
     */
    List<StrategicTask> findByOrg_Id(Long orgId);

    /**
     * Find all tasks by task type
     */
    List<StrategicTask> findByTaskType(TaskType taskType);

    /**
     * Find all tasks by cycle ID and organization ID
     */
    List<StrategicTask> findByCycleIdAndOrg_Id(Long cycleId, Long orgId);

    /**
     * Find all tasks by cycle ID ordered by sort order
     */
    List<StrategicTask> findByCycleIdOrderBySortOrderAsc(Long cycleId);

    /**
     * Find all tasks by cycle ID and task type
     */
    List<StrategicTask> findByCycleIdAndTaskType(Long cycleId, TaskType taskType);

    /**
     * Find tasks created by a specific organization
     */
    List<StrategicTask> findByCreatedByOrg_Id(Long createdByOrgId);

    /**
     * Count tasks by cycle ID
     */
    long countByCycleId(Long cycleId);

    /**
     * Find tasks by cycle ID and organization hierarchy
     * Returns tasks where the organization is involved (flat structure, no hierarchy)
     */
    @Query("SELECT DISTINCT t FROM StrategicTask t " +
           "WHERE t.cycleId = :cycleId " +
           "AND t.org.id = :orgId")
    List<StrategicTask> findByCycleAndOrgHierarchy(@Param("cycleId") Long cycleId, 
                                                     @Param("orgId") Long orgId);

    /**
     * Search tasks by name or description
     */
    @Query("SELECT t FROM StrategicTask t WHERE " +
           "t.taskName LIKE %:keyword% OR t.taskDesc LIKE %:keyword%")
    List<StrategicTask> searchByKeyword(@Param("keyword") String keyword);
}
