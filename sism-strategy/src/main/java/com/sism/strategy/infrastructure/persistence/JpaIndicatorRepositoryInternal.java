package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface JpaIndicatorRepositoryInternal extends JpaRepository<Indicator, Long> {
    List<Indicator> findByOwnerOrgId(Long ownerOrgId);
    List<Indicator> findByTargetOrgId(Long targetOrgId);
    List<Indicator> findByStatus(String status);
    Page<Indicator> findByStatus(String status, Pageable pageable);
    List<Indicator> findByParentIndicatorId(Long parentIndicatorId);
    List<Indicator> findByTaskId(Long taskId);
}
