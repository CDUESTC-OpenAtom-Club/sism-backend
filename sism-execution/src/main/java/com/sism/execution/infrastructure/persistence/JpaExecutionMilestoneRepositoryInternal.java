package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.milestone.Milestone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaExecutionMilestoneRepositoryInternal extends JpaRepository<Milestone, Long> {

    Page<Milestone> findAll(Pageable pageable);

    List<Milestone> findByIndicatorId(Long indicatorId);

    List<Milestone> findByStatus(String status);

    Page<Milestone> findByStatus(String status, Pageable pageable);
}
