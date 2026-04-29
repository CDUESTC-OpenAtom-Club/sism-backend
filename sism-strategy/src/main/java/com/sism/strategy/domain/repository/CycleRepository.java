package com.sism.strategy.domain.repository;

import com.sism.strategy.domain.cycle.Cycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * CycleRepository - 考核周期仓储接口
 */
public interface CycleRepository {

    Optional<Cycle> findById(Long id);

    List<Cycle> findAll();

    Page<Cycle> findAll(Pageable pageable);

    List<Cycle> findByYear(Integer year);

    Cycle save(Cycle cycle);

    void delete(Cycle cycle);

    boolean existsById(Long id);

    /**
     * 获取所有不重复的年份（用于年份选择器）
     */
    List<Integer> findDistinctYears();
}
