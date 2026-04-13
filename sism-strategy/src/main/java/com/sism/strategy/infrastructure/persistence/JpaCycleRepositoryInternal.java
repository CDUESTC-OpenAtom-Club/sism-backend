package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaCycleRepositoryInternal extends JpaRepository<Cycle, Long> {
    @Query("""
            SELECT c
            FROM Cycle c
            WHERE c.id = :id
              AND c.isDeleted = false
            """)
    Optional<Cycle> findByIdAndIsDeletedFalse(@Param("id") Long id);

    @Query("""
            SELECT c
            FROM Cycle c
            WHERE c.isDeleted = false
            """)
    List<Cycle> findAllActive();

    @Query("""
            SELECT c
            FROM Cycle c
            WHERE c.isDeleted = false
            """)
    Page<Cycle> findAllActive(Pageable pageable);

    @Query("""
            SELECT c
            FROM Cycle c
            WHERE c.year = :year
              AND c.isDeleted = false
            """)
    List<Cycle> findByYear(@Param("year") Integer year);

    @Query("""
            SELECT c
            FROM Cycle c
            WHERE c.status = :status
              AND c.isDeleted = false
            """)
    List<Cycle> findByStatus(@Param("status") String status);

    @Query("""
            SELECT c
            FROM Cycle c
            WHERE c.year = :year
              AND c.status = :status
              AND c.isDeleted = false
            """)
    List<Cycle> findByYearAndStatus(@Param("year") Integer year, @Param("status") String status);

    @Query("SELECT DISTINCT c.year FROM Cycle c WHERE c.year IS NOT NULL ORDER BY c.year DESC")
    List<Integer> findDistinctYears();
}
