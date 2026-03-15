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
    List<StrategicTask> findByOrgId(Long orgId);

    /**
     * 根据状态查找任务
     *
     * @param status 任务状态
     * @return 指定状态的任务列表
     */
    List<StrategicTask> findByStatus(String status);

    /**
     * 根据任务类型查找任务
     *
     * @param taskType 任务类型
     * @return 指定类型的任务列表
     */
    List<StrategicTask> findByTaskType(TaskType taskType);

    /**
     * 根据计划ID查找任务
     *
     * @param planId 计划ID
     * @return 该计划的任务列表
     */
    List<StrategicTask> findByPlanId(Long planId);

    /**
     * 根据周期ID查找任务
     *
     * @param cycleId 周期ID
     * @return 该周期的任务列表
     */
    List<StrategicTask> findByCycleId(Long cycleId);

    /**
     * 根据计划ID和周期ID查找任务
     *
     * @param planId 计划ID
     * @param cycleId 周期ID
     * @return 指定计划和周期的任务列表
     */
    List<StrategicTask> findByPlanIdAndCycleId(Long planId, Long cycleId);

    /**
     * 根据多个条件查询任务（支持组合查询）
     * <p>
     * 该查询会自动过滤已删除的任务（isDeleted = false）。
     * 所有参数都是可选的，传入null表示不限制该条件。
     * </p>
     *
     * @param planId 计划ID（可选）
     * @param cycleId 周期ID（可选）
     * @param orgId 执行组织ID（可选）
     * @param createdByOrgId 创建组织ID（可选）
     * @param taskType 任务类型（可选）
     * @param status 任务状态（可选）
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
            "(:status IS NULL OR t.status = :status) AND " +
            "(:taskName IS NULL OR t.taskName LIKE %:taskName%) AND " +
            "t.isDeleted = false")
    Page<StrategicTask> findByCriteria(
            @Param("planId") Long planId,
            @Param("cycleId") Long cycleId,
            @Param("orgId") Long orgId,
            @Param("createdByOrgId") Long createdByOrgId,
            @Param("taskType") TaskType taskType,
            @Param("status") String status,
            @Param("taskName") String taskName,
            Pageable pageable);
}
