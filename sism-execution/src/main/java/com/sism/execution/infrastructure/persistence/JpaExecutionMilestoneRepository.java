package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.model.milestone.Milestone;
import com.sism.execution.domain.repository.ExecutionMilestoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaExecutionMilestoneRepository implements ExecutionMilestoneRepository {

    private final JpaExecutionMilestoneRepositoryInternal jpaRepository;

    @Override
    public Optional<Milestone> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Milestone> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Milestone> findByIndicatorId(Long indicatorId) {
        return jpaRepository.findByIndicatorId(indicatorId);
    }

    @Override
    public List<Milestone> findByStatus(String status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public Milestone save(Milestone milestone) {
        return jpaRepository.save(milestone);
    }

    @Override
    public void delete(Milestone milestone) {
        jpaRepository.delete(milestone);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
