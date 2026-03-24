package com.sism.analytics.infrastructure.repository;

import com.sism.analytics.domain.Report;
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
    List<Report> findByGeneratedByAndNotDeleted(@Param("generatedBy") Long generatedBy);

    /**
     * 根据报告类型查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.type = :type AND r.deleted = false ORDER BY r.createdAt DESC")
    List<Report> findByTypeAndNotDeleted(@Param("type") String type);

    /**
     * 根据状态查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.status = :status AND r.deleted = false ORDER BY r.createdAt DESC")
    List<Report> findByStatusAndNotDeleted(@Param("status") String status);

    /**
     * 根据生成者ID和状态查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.generatedBy = :generatedBy AND r.status = :status AND r.deleted = false ORDER BY r.createdAt DESC")
    List<Report> findByGeneratedByAndStatusAndNotDeleted(
            @Param("generatedBy") Long generatedBy,
            @Param("status") String status
    );

    /**
     * 根据日期范围查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.createdAt >= :startDate AND r.createdAt <= :endDate AND r.deleted = false ORDER BY r.createdAt DESC")
    List<Report> findByDateRangeAndNotDeleted(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 根据名称模糊查找所有未删除的报告
     */
    @Query("SELECT r FROM Report r WHERE r.name LIKE %:name% AND r.deleted = false ORDER BY r.createdAt DESC")
    List<Report> findByNameContainingAndNotDeleted(@Param("name") String name);

    /**
     * 查找所有已生成的报告
     */
    @Query("SELECT r FROM Report r WHERE r.status = 'GENERATED' AND r.deleted = false ORDER BY r.generatedAt DESC")
    List<Report> findAllGeneratedAndNotDeleted();

    /**
     * 统计生成者的报告数量
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.generatedBy = :generatedBy AND r.deleted = false")
    long countByGeneratedByAndNotDeleted(@Param("generatedBy") Long generatedBy);

    /**
     * 统计状态的报告数量
     */
    @Query("SELECT COUNT(r) FROM Report r WHERE r.status = :status AND r.deleted = false")
    long countByStatusAndNotDeleted(@Param("status") String status);
}
