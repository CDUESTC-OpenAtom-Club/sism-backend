package com.sism.strategy.domain.repository;

import com.sism.strategy.domain.model.milestone.Milestone;

import java.util.List;
import java.util.Optional;

/**
 * MilestoneRepository - 里程碑仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface MilestoneRepository {

    Optional<Milestone> findById(Long id);

    List<Milestone> findAll();

    List<Milestone> findByIndicatorId(Long indicatorId);

    List<Milestone> findByStatus(String status);

    Milestone save(Milestone milestone);

    void delete(Milestone milestone);

    boolean existsById(Long id);
}
