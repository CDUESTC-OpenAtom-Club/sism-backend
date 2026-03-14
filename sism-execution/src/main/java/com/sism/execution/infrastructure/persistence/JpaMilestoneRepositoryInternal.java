package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.model.milestone.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaMilestoneRepositoryInternal extends JpaRepository<Milestone, Long> {
    List<Milestone> findByIndicatorId(Long indicatorId);
    List<Milestone> findByStatus(String status);
}
