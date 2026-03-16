package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.model.milestone.Milestone;
import com.sism.strategy.domain.repository.MilestoneRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JpaMilestoneRepository - 里程碑仓储 JPA 实现
 * 实现 MilestoneRepository 接口，提供数据持久化能力
 */
@Repository
public interface JpaMilestoneRepository extends JpaRepository<Milestone, Long>, MilestoneRepository {
    // 所有方法都由 JpaRepository 默认提供
}
