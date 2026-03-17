package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaCycleRepositoryInternal extends JpaRepository<Cycle, Long> {
    List<Cycle> findByYear(Integer year);
}
