package com.sism.task.infrastructure.persistence;

import com.sism.task.domain.StrategicTask;
import com.sism.task.domain.TaskType;
import com.sism.task.domain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JpaTaskRepository - JPA任务仓储实现
 * <p>
 * 实现领域层定义的TaskRepository接口，使用JPA进行数据持久化。
 * 该类作为适配器，将领域层的仓储接口委托给Spring Data JPA实现。
 * </p>
 * <p>
 * 设计模式：
 * <ul>
 *   <li>Repository模式：封装数据访问逻辑</li>
 *   <li>Adapter模式：将JPA仓储适配为领域仓储接口</li>
 * </ul>
 * </p>
 */
@Repository
@RequiredArgsConstructor
public class JpaTaskRepository implements TaskRepository {

    private final JpaTaskRepositoryInternal jpaRepository;

    @Override
    public Optional<StrategicTask> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<StrategicTask> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Page<StrategicTask> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public List<StrategicTask> findByOrgId(Long orgId) {
        return jpaRepository.findByOrgId(orgId);
    }

    @Override
    public List<StrategicTask> findByStatus(String status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<StrategicTask> findByTaskType(TaskType taskType) {
        return jpaRepository.findByTaskType(taskType);
    }

    @Override
    public List<StrategicTask> findByPlanId(Long planId) {
        return jpaRepository.findByPlanId(planId);
    }

    @Override
    public List<StrategicTask> findByCycleId(Long cycleId) {
        return jpaRepository.findByCycleId(cycleId);
    }

    @Override
    public List<StrategicTask> findByPlanIdAndCycleId(Long planId, Long cycleId) {
        return jpaRepository.findByPlanIdAndCycleId(planId, cycleId);
    }

    @Override
    public Page<StrategicTask> findByCriteria(
            Long planId, Long cycleId, Long orgId, Long createdByOrgId,
            TaskType taskType, String status, String taskName, Pageable pageable) {
        return jpaRepository.findByCriteria(planId, cycleId, orgId, createdByOrgId,
                taskType, status, taskName, pageable);
    }

    @Override
    public StrategicTask save(StrategicTask task) {
        return jpaRepository.save(task);
    }

    @Override
    public void delete(StrategicTask task) {
        jpaRepository.delete(task);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
