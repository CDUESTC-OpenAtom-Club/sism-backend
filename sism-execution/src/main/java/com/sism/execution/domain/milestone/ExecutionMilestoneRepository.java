package com.sism.execution.domain.milestone;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * ExecutionMilestoneRepository - 里程碑仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface ExecutionMilestoneRepository {

    Optional<Milestone> findById(Long id);

    List<Milestone> findAll();

    Page<Milestone> findAll(Pageable pageable);

    List<Milestone> findByIndicatorId(Long indicatorId);

    List<Milestone> findByStatus(MilestoneStatus status);

    Page<Milestone> findByStatus(MilestoneStatus status, Pageable pageable);

    Milestone save(Milestone milestone);

    void delete(Milestone milestone);

    boolean existsById(Long id);
}
