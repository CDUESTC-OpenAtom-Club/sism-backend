package com.sism.strategy.infrastructure.persistence;

import com.sism.enums.IndicatorLevel;
import com.sism.organization.domain.SysOrg;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.repository.IndicatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaIndicatorRepository implements IndicatorRepository {

    private final JpaIndicatorRepositoryInternal jpaRepository;

    @Override
    public Optional<Indicator> findById(Long id) {
        return jpaRepository.findByIdAndIsDeletedFalse(id);
    }

    @Override
    public List<Indicator> findAll() {
        return jpaRepository.findAllByIsDeletedFalse();
    }

    @Override
    public Page<Indicator> findAll(Pageable pageable) {
        return jpaRepository.findAllByIsDeletedFalse(pageable);
    }

    @Override
    public List<Indicator> findByTargetOrg(SysOrg targetOrg) {
        if (targetOrg == null || targetOrg.getId() == null) {
            return List.of();
        }
        return findByTargetOrgId(targetOrg.getId());
    }

    @Override
    public List<Indicator> findByTargetOrgId(Long targetOrgId) {
        return jpaRepository.findByTargetOrgIdAndIsDeletedFalse(targetOrgId);
    }

    @Override
    public List<Indicator> findByOwnerOrg(SysOrg ownerOrg) {
        if (ownerOrg == null || ownerOrg.getId() == null) {
            return List.of();
        }
        return findByOwnerOrgId(ownerOrg.getId());
    }

    @Override
    public List<Indicator> findByOwnerOrgId(Long ownerOrgId) {
        return jpaRepository.findByOwnerOrgIdAndIsDeletedFalse(ownerOrgId);
    }

    @Override
    public List<Indicator> findByStatus(String status) {
        return jpaRepository.findByStatusAndIsDeletedFalse(status);
    }

    @Override
    public Page<Indicator> findByStatus(String status, Pageable pageable) {
        return jpaRepository.findByStatusAndIsDeletedFalse(status, pageable);
    }

    @Override
    public List<Indicator> findByParentIndicatorId(Long parentIndicatorId) {
        return jpaRepository.findByParentIndicatorIdAndIsDeletedFalse(parentIndicatorId);
    }

    @Override
    public List<Indicator> findByTaskId(Long taskId) {
        return jpaRepository.findByTaskIdAndIsDeletedFalse(taskId);
    }

    @Override
    public Indicator save(Indicator indicator) {
        return jpaRepository.save(indicator);
    }

    @Override
    public void delete(Indicator indicator) {
        jpaRepository.delete(indicator);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsByIdAndIsDeletedFalse(id);
    }

    @Override
    public List<Indicator> findFirstLevelIndicators() {
        return findAll().stream()
                .filter(indicator -> indicator.getLevel() == IndicatorLevel.FIRST)
                .collect(Collectors.toList());
    }

    @Override
    public List<Indicator> findSecondLevelIndicators() {
        return findAll().stream()
                .filter(indicator -> indicator.getLevel() == IndicatorLevel.SECOND)
                .collect(Collectors.toList());
    }

    @Override
    public List<Indicator> findByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String lowerKeyword = keyword.toLowerCase();
        return findAll().stream()
                .filter(indicator -> {
                    String desc = indicator.getIndicatorDesc();
                    return desc != null && desc.toLowerCase().contains(lowerKeyword);
                })
                .collect(Collectors.toList());
    }
}
