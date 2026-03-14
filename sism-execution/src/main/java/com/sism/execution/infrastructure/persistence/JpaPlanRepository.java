package com.sism.execution.infrastructure.persistence;

import com.sism.execution.domain.model.plan.Plan;
import com.sism.execution.domain.model.plan.PlanLevel;
import com.sism.execution.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaPlanRepository implements PlanRepository {

    private final JpaPlanRepositoryInternal jpaRepository;

    @Override
    public Optional<Plan> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Plan> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public List<Plan> findByTargetOrgId(Long targetOrgId) {
        return jpaRepository.findByTargetOrgId(targetOrgId);
    }

    @Override
    public List<Plan> findByCreatedByOrgId(Long createdByOrgId) {
        return jpaRepository.findByCreatedByOrgId(createdByOrgId);
    }

    @Override
    public List<Plan> findByCycleId(Long cycleId) {
        return jpaRepository.findByCycleId(cycleId);
    }

    @Override
    public List<Plan> findByPlanLevel(PlanLevel planLevel) {
        return jpaRepository.findByPlanLevel(planLevel);
    }

    @Override
    public List<Plan> findByStatus(String status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public Plan save(Plan plan) {
        return jpaRepository.save(plan);
    }

    @Override
    public void delete(Plan plan) {
        jpaRepository.delete(plan);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
