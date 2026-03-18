package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaCycleRepositoryInternal extends JpaRepository<Cycle, Long> {
    List<Cycle> findByYear(Integer year);

    @Query("SELECT DISTINCT c.year FROM Cycle c WHERE c.year IS NOT NULL ORDER BY c.year DESC")
    List<Integer> findDistinctYears();
}
