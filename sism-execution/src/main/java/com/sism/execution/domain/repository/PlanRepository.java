package com.sism.execution.domain.repository;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * PlanRepository - 计划仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface PlanRepository {

    /**
     * 根据ID查询计划
     */
    Optional<Plan> findById(Long id);

    /**
     * 查询所有计划
     */
    List<Plan> findAll();

    /**
     * 分页查询计划
     */
    Page<Plan> findPage(List<Long> cycleIds, List<String> statuses, Pageable pageable);

    /**
     * 根据目标组织ID查询计划
     */
    List<Plan> findByTargetOrgId(Long targetOrgId);

    /**
     * 根据创建组织ID查询计划
     */
    List<Plan> findByCreatedByOrgId(Long createdByOrgId);

    /**
     * 根据周期ID查询计划
     */
    List<Plan> findByCycleId(Long cycleId);

    /**
     * 根据层级查询计划
     */
    List<Plan> findByPlanLevel(PlanLevel planLevel);

    /**
     * 根据状态查询计划
     */
    List<Plan> findByStatuses(List<String> statuses);

    /**
     * 保存计划
     */
    Plan save(Plan plan);

    /**
     * 删除计划
     */
    void delete(Plan plan);

    /**
     * 检查计划是否存在
     */
    boolean existsById(Long id);
}
