package com.sism.task.infrastructure.persistence;

import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JpaTaskRepositoryInternal - JPA内部仓储接口
 * <p>
 * Spring Data JPA仓储接口，提供StrategicTask实体的数据库访问能力。
 * 该接口直接使用JPA特性，通过JpaTaskRepository适配器暴露给领域层。
 * </p>
 * <p>
 * 注意：
 * <ul>
 *   <li>已移除findByIndicatorId和findByAssigneeId方法，因为StrategicTask实体不再包含这些字段</li>
 *   <li>findByCriteria方法自动过滤已删除的任务（isDeleted = false）</li>
 * </ul>
 * </p>
 */
@Repository
public interface JpaTaskRepositoryInternal extends JpaRepository<StrategicTask, Long> {

    /**
     * 根据组织ID查找任务
     *
     * @param orgId 组织ID
     * @return 该组织的任务列表
     */
    @Query("""
            SELECT t FROM StrategicTask t
            LEFT JOIN FETCH t.org
            LEFT JOIN FETCH t.createdByOrg
            WHERE t.org.id = :orgId
              AND t.isDeleted = false
            """)
    List<StrategicTask> findByOrgId(@Param("orgId") Long orgId);

    /**
     * 根据任务类型查找任务
     *
     * @param taskType 任务类型
     * @return 指定类型的任务列表
     */
    @Query("""
            SELECT t FROM StrategicTask t
            LEFT JOIN FETCH t.org
            LEFT JOIN FETCH t.createdByOrg
            WHERE t.taskType = :taskType
              AND t.isDeleted = false
            """)
    List<StrategicTask> findByTaskType(@Param("taskType") TaskType taskType);

    /**
     * 根据计划ID查找任务
     *
     * @param planId 计划ID
     * @return 该计划的任务列表
     */
    @Query("""
            SELECT t FROM StrategicTask t
            LEFT JOIN FETCH t.org
            LEFT JOIN FETCH t.createdByOrg
            WHERE t.planId = :planId
              AND t.isDeleted = false
            """)
    List<StrategicTask> findByPlanId(@Param("planId") Long planId);

    /**
     * 根据周期ID查找任务
     *
     * @param cycleId 周期ID
     * @return 该周期的任务列表
     */
    @Query("""
            SELECT t FROM StrategicTask t
            LEFT JOIN FETCH t.org
            LEFT JOIN FETCH t.createdByOrg
            WHERE t.cycleId = :cycleId
              AND t.isDeleted = false
            """)
    List<StrategicTask> findByCycleId(@Param("cycleId") Long cycleId);

    @Query(value = """
            SELECT
                t.task_id AS id,
                t.task_name AS taskName,
                t.task_desc AS taskDesc,
                t.task_type AS taskType,
                t.plan_id AS planId,
                t.cycle_id AS cycleId,
                t.org_id AS orgId,
                t.created_by_org_id AS createdByOrgId,
                t.sort_order AS sortOrder,
                COALESCE(p.status, 'DRAFT') AS status,
                t.remark AS remark,
                t.created_at AS createdAt,
                t.updated_at AS updatedAt
            FROM sys_task t
            LEFT JOIN plan p ON p.id = t.plan_id
            WHERE t.cycle_id = :cycleId
              AND COALESCE(t.is_deleted, false) = false
            ORDER BY t.sort_order ASC, t.task_id ASC
            """, nativeQuery = true)
    List<TaskFlatView> findFlatViewsByCycleId(@Param("cycleId") Long cycleId);

    @Query(value = """
            SELECT
                t.task_id AS id,
                t.task_name AS taskName,
                t.task_desc AS taskDesc,
                t.task_type AS taskType,
                t.plan_id AS planId,
                t.cycle_id AS cycleId,
                t.org_id AS orgId,
                t.created_by_org_id AS createdByOrgId,
                t.sort_order AS sortOrder,
                COALESCE(p.status, 'DRAFT') AS status,
                t.remark AS remark,
                t.created_at AS createdAt,
                t.updated_at AS updatedAt
            FROM sys_task t
            LEFT JOIN plan p ON p.id = t.plan_id
            WHERE COALESCE(t.is_deleted, false) = false
              AND (:planId IS NULL OR t.plan_id = :planId)
              AND (:cycleId IS NULL OR t.cycle_id = :cycleId)
              AND (:orgId IS NULL OR t.org_id = :orgId)
              AND (:createdByOrgId IS NULL OR t.created_by_org_id = :createdByOrgId)
              AND (CAST(:taskType AS TEXT) = '' OR t.task_type = CAST(:taskType AS TEXT))
              AND (CAST(:taskName AS TEXT) = '' OR t.task_name ILIKE CONCAT('%', CAST(:taskName AS TEXT), '%'))
            ORDER BY t.sort_order ASC, t.task_id ASC
            """, nativeQuery = true)
    List<TaskFlatView> findFlatViewsByCriteria(
            @Param("planId") Long planId,
            @Param("cycleId") Long cycleId,
            @Param("orgId") Long orgId,
            @Param("createdByOrgId") Long createdByOrgId,
            @Param("taskType") String taskType,
            @Param("taskName") String taskName);

    @Query(value = """
            SELECT
                t.task_id AS id,
                t.task_name AS taskName
            FROM sys_task t
            WHERE t.task_id IN (:taskIds)
              AND COALESCE(t.is_deleted, false) = false
            """, nativeQuery = true)
    List<TaskNameView> findTaskNamesByIds(@Param("taskIds") List<Long> taskIds);

    /**
     * 根据计划ID和周期ID查找任务
     *
     * @param planId 计划ID
     * @param cycleId 周期ID
     * @return 指定计划和周期的任务列表
     */
    @Query("""
            SELECT t FROM StrategicTask t
            LEFT JOIN FETCH t.org
            LEFT JOIN FETCH t.createdByOrg
            WHERE t.planId = :planId
              AND t.cycleId = :cycleId
              AND t.isDeleted = false
            """)
    List<StrategicTask> findByPlanIdAndCycleId(@Param("planId") Long planId, @Param("cycleId") Long cycleId);

    /**
     * 根据多个条件查询任务（支持组合查询）
     * <p>
     * 该查询会自动过滤已删除的任务（isDeleted = false）。
     * 所有参数都是可选的，传入null表示不限制该条件。
     * </p>
     * <p>
     * 注意：Task 的状态从关联的 Plan 获取，因此不支持按状态查询。
     * </p>
     *
     * @param planId 计划ID（可选）
     * @param cycleId 周期ID（可选）
     * @param orgId 执行组织ID（可选）
     * @param createdByOrgId 创建组织ID（可选）
     * @param taskType 任务类型（可选）
     * @param taskName 任务名称（支持模糊查询，可选）
     * @param pageable 分页参数
     * @return 分页任务结果
     */
    @Query("SELECT t FROM StrategicTask t WHERE " +
            "(:planId IS NULL OR t.planId = :planId) AND " +
            "(:cycleId IS NULL OR t.cycleId = :cycleId) AND " +
            "(:orgId IS NULL OR t.org.id = :orgId) AND " +
            "(:createdByOrgId IS NULL OR t.createdByOrg.id = :createdByOrgId) AND " +
            "(:taskType IS NULL OR t.taskType = :taskType) AND " +
            "(:taskName IS NULL OR t.taskName LIKE %:taskName%) AND " +
            "t.isDeleted = false")
    Page<StrategicTask> findByCriteria(
            @Param("planId") Long planId,
            @Param("cycleId") Long cycleId,
            @Param("orgId") Long orgId,
            @Param("createdByOrgId") Long createdByOrgId,
            @Param("taskType") TaskType taskType,
            @Param("taskName") String taskName,
            Pageable pageable);
}
