package com.sism.task.domain.repository;

import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * TaskRepository - 任务仓储接口
 * <p>
 * 定义在领域层，由基础设施层实现。提供任务的持久化操作和查询功能。
 * </p>
 * <p>
 * 注意：根据数据库结构分析，StrategicTask实体已不再包含indicatorId和assigneeId字段。
 * 相关查询方法已移除，如需按指标或执行人查询任务，请使用findByCriteria方法。
 * </p>
 */
public interface TaskRepository {

    /**
     * 根据ID查找任务
     *
     * @param id 任务ID
     * @return 任务Optional对象，不存在时返回Optional.empty()
     */
    Optional<StrategicTask> findById(Long id);

    /**
     * 查找所有任务
     *
     * @return 所有任务列表
     */
    List<StrategicTask> findAll();

    /**
     * 分页查找所有任务
     *
     * @param pageable 分页参数
     * @return 分页任务结果
     */
    Page<StrategicTask> findAll(Pageable pageable);

    /**
     * 根据组织ID查找任务
     *
     * @param orgId 组织ID
     * @return 该组织的任务列表
     */
    List<StrategicTask> findByOrgId(Long orgId);

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
     *
     * 注意：Task 的状态从关联的 Plan 获取，因此不支持按状态查询。
     * 如需按状态筛选，请先查询 Plan，再根据 planId 查询 Task。
     *
     * @param planId 计划ID（可选）
     * @param cycleId 周期ID（可选）
     * @param orgId 执行组织ID（可选）
     * @param createdByOrgId 创建组织ID（可选）
     * @param taskType 任务类型（可选）
     * @param name 任务名称（支持模糊查询，可选）
     * @param pageable 分页参数
     * @return 分页任务结果
     */
    Page<StrategicTask> findByCriteria(
            Long planId, Long cycleId, Long orgId, Long createdByOrgId,
            TaskType taskType, String name, Pageable pageable);

    /**
     * 保存任务（新增或更新）
     *
     * @param task 要保存的任务
     * @return 保存后的任务
     */
    StrategicTask save(StrategicTask task);

    /**
     * 删除任务
     *
     * @param task 要删除的任务
     */
    void delete(StrategicTask task);

    /**
     * 检查任务是否存在
     *
     * @param id 任务ID
     * @return true如果存在，false如果不存在
     */
    boolean existsById(Long id);
}
