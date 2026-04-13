package com.sism.strategy.domain.repository;

import com.sism.strategy.domain.model.milestone.Milestone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    List<Milestone> findByIndicatorIdIn(List<Long> indicatorIds);

    List<Milestone> findByStatus(String status);

    Page<Milestone> findAll(Pageable pageable);

    Page<Milestone> findByIndicatorId(Long indicatorId, Pageable pageable);

    Page<Milestone> findByStatus(String status, Pageable pageable);

    Page<Milestone> findByIndicatorIdAndStatus(Long indicatorId, String status, Pageable pageable);

    Milestone save(Milestone milestone);

    void delete(Milestone milestone);

    boolean existsById(Long id);
}
