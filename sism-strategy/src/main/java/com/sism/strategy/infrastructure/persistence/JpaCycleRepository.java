package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.Cycle;
import com.sism.strategy.domain.repository.CycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaCycleRepository implements CycleRepository {

    private final JpaCycleRepositoryInternal jpaRepository;

    @Override
    public Optional<Cycle> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Cycle> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Page<Cycle> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public List<Cycle> findByStatus(String status) {
        return jpaRepository.findByStatus(status);
    }

    @Override
    public List<Cycle> findByYear(Integer year) {
        return jpaRepository.findByYear(year);
    }

    @Override
    public List<Cycle> findByStatusAndYear(String status, Integer year) {
        return jpaRepository.findByStatusAndYear(status, year);
    }

    @Override
    public Cycle save(Cycle cycle) {
        return jpaRepository.save(cycle);
    }

    @Override
    public void delete(Cycle cycle) {
        jpaRepository.delete(cycle);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
