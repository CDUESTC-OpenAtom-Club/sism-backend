package com.sism.strategy.infrastructure.persistence;

import com.sism.strategy.domain.Indicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface JpaIndicatorRepositoryInternal extends JpaRepository<Indicator, Long> {
    @EntityGraph(attributePaths = {"ownerOrg", "targetOrg"})
    Optional<Indicator> findByIdAndIsDeletedFalse(Long id);
    boolean existsByIdAndIsDeletedFalse(Long id);

    List<Indicator> findAllByIsDeletedFalse();

    @EntityGraph(attributePaths = {"ownerOrg", "targetOrg"})
    Page<Indicator> findAllByIsDeletedFalse(Pageable pageable);

    List<Indicator> findByOwnerOrgIdAndIsDeletedFalse(Long ownerOrgId);
    List<Indicator> findByTargetOrgIdAndIsDeletedFalse(Long targetOrgId);
    List<Indicator> findByStatusAndIsDeletedFalse(String status);

    @EntityGraph(attributePaths = {"ownerOrg", "targetOrg"})
    Page<Indicator> findByStatusAndIsDeletedFalse(String status, Pageable pageable);
    List<Indicator> findByParentIndicatorIdAndIsDeletedFalse(Long parentIndicatorId);

    @EntityGraph(attributePaths = {"ownerOrg", "targetOrg"})
    List<Indicator> findByTaskIdAndIsDeletedFalse(Long taskId);

    /**
     * 根据任务ID列表分页查询指标
     */
    @EntityGraph(attributePaths = {"ownerOrg", "targetOrg"})
    @Query("""
            SELECT i FROM Indicator i
            WHERE i.taskId IN :taskIds
              AND i.isDeleted = false
            """)
    Page<Indicator> findByTaskIds(@Param("taskIds") List<Long> taskIds, Pageable pageable);

    /**
     * 根据任务ID列表统计指标数量
     */
    @Query("""
            SELECT COUNT(i) FROM Indicator i
            WHERE i.taskId IN :taskIds
              AND i.isDeleted = false
            """)
    long countByTaskIds(@Param("taskIds") List<Long> taskIds);
}
