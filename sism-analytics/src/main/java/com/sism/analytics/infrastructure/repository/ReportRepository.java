package com.sism.analytics.infrastructure.repository;

import com.sism.analytics.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ReportRepository - 报告仓储接口
 */
@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 根据ID查找未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.id = :id AND r.deleted = false")
    Optional<Report> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 根据生成者ID查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.generatedBy = :generatedBy AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByGeneratedByAndNotDeleted(@Param("generatedBy") Long generatedBy, Pageable pageable);

    default List<Report> findByGeneratedByAndNotDeleted(Long generatedBy) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByGeneratedByAndNotDeleted(generatedBy, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据报告类型查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.type = :type AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByTypeAndNotDeleted(@Param("type") String type, Pageable pageable);

    default List<Report> findByTypeAndNotDeleted(String type) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByTypeAndNotDeleted(type, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据状态查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByStatusAndNotDeleted(@Param("status") String status, Pageable pageable);

    default List<Report> findByStatusAndNotDeleted(String status) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByStatusAndNotDeleted(status, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据生成者ID和状态查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.generatedBy = :generatedBy AND r.status = :status AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByGeneratedByAndStatusAndNotDeleted(
            @Param("generatedBy") Long generatedBy,
            @Param("status") String status,
            Pageable pageable
    );

    default List<Report> findByGeneratedByAndStatusAndNotDeleted(Long generatedBy, String status) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByGeneratedByAndStatusAndNotDeleted(generatedBy, status, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据生成者ID和报告类型查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.generatedBy = :generatedBy AND r.type = :type AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByGeneratedByAndTypeAndNotDeleted(
            @Param("generatedBy") Long generatedBy,
            @Param("type") String type,
            Pageable pageable
    );

    default List<Report> findByGeneratedByAndTypeAndNotDeleted(Long generatedBy, String type) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByGeneratedByAndTypeAndNotDeleted(generatedBy, type, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据日期范围查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByDateRangeAndNotDeleted(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    default List<Report> findByDateRangeAndNotDeleted(LocalDateTime startDate, LocalDateTime endDate) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByDateRangeAndNotDeleted(startDate, endDate, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据生成者ID和日期范围查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.generatedBy = :generatedBy AND r.createdAt >= :startDate AND r.createdAt <= :endDate AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByGeneratedByAndDateRangeAndNotDeleted(
            @Param("generatedBy") Long generatedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    default List<Report> findByGeneratedByAndDateRangeAndNotDeleted(Long generatedBy, LocalDateTime startDate, LocalDateTime endDate) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByGeneratedByAndDateRangeAndNotDeleted(generatedBy, startDate, endDate, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据名称模糊查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.name LIKE CONCAT('%', :name, '%') ESCAPE '\\' AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByNameContainingAndNotDeleted(@Param("name") String name, Pageable pageable);

    default List<Report> findByNameContainingAndNotDeleted(String name) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByNameContainingAndNotDeleted(name, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据生成者ID和名称模糊查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.generatedBy = :generatedBy AND r.name LIKE CONCAT('%', :name, '%') ESCAPE '\\' AND r.deleted = false ORDER BY r.createdAt DESC")
    Page<Report> findByGeneratedByAndNameContainingAndNotDeleted(
            @Param("generatedBy") Long generatedBy,
            @Param("name") String name,
            Pageable pageable
    );

    default List<Report> findByGeneratedByAndNameContainingAndNotDeleted(Long generatedBy, String name) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByGeneratedByAndNameContainingAndNotDeleted(generatedBy, name, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 查找所有已生成的报告
     */
    @Query("SELECT r FROM Report r WHERE r.status = 'GENERATED' AND r.deleted = false ORDER BY r.generatedAt DESC")
    List<Report> findAllGeneratedAndNotDeleted(Pageable pageable);

    default List<Report> findAllGeneratedAndNotDeleted() {
        return findAllGeneratedAndNotDeleted(AnalyticsRepositoryPagingSupport.firstPage());
    }

    /**
     * 统计生成者的报告数量
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.generatedBy = :generatedBy AND r.deleted = false")
    long countByGeneratedByAndNotDeleted(@Param("generatedBy") Long generatedBy);

    /**
     * 统计生成者和状态的报告数量
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.generatedBy = :generatedBy AND r.status = :status AND r.deleted = false")
    long countByGeneratedByAndStatusAndNotDeleted(
            @Param("generatedBy") Long generatedBy,
            @Param("status") String status
    );

    /**
     * 统计状态的报告数量
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status AND r.deleted = false")
    long countByStatusAndNotDeleted(@Param("status") String status);
}
