package com.sism.analytics.infrastructure.repository;

import com.sism.analytics.domain.DataExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DataExportRepository - 数据导出仓储接口
 */
@Repository
public interface DataExportRepository extends JpaRepository<DataExport, Long> {

    /**
     * 根据ID查找未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.id = :id AND e.deleted = false")
    Optional<DataExport> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 根据请求者ID查找所有未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.deleted = false ORDER BY e.requestedAt DESC")
    List<DataExport> findByRequestedByAndNotDeleted(@Param("requestedBy") Long requestedBy);

    /**
     * 根据状态查找所有未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = :status AND e.deleted = false ORDER BY e.requestedAt DESC")
    List<DataExport> findByStatusAndNotDeleted(@Param("status") String status);

    /**
     * 根据请求者ID和状态查找所有未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.status = :status AND e.deleted = false ORDER BY e.requestedAt DESC")
    List<DataExport> findByRequestedByAndStatusAndNotDeleted(
            @Param("requestedBy") Long requestedBy,
            @Param("status") String status
    );

    /**
     * 查找所有可下载的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = 'COMPLETED' AND e.deleted = false ORDER BY e.completedAt DESC")
    List<DataExport> findAllDownloadableAndNotDeleted();

    /**
     * 查找用户的所有可下载导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.status = 'COMPLETED' AND e.deleted = false ORDER BY e.completedAt DESC")
    List<DataExport> findDownloadableByRequestedByAndNotDeleted(@Param("requestedBy") Long requestedBy);

    /**
     * 查找所有失败的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = 'FAILED' AND e.deleted = false ORDER BY e.completedAt DESC")
    List<DataExport> findAllFailedAndNotDeleted();

    /**
     * 查找所有可重试的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = 'FAILED' AND e.deleted = false ORDER BY e.completedAt DESC")
    List<DataExport> findAllRetryableAndNotDeleted();

    /**
     * 查找日期范围内的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedAt >= :startDate AND e.requestedAt <= :endDate AND e.deleted = false ORDER BY e.requestedAt DESC")
    List<DataExport> findByDateRangeAndNotDeleted(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 根据名称模糊查找导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.name LIKE %:name% AND e.deleted = false ORDER BY e.requestedAt DESC")
    List<DataExport> findByNameContainingAndNotDeleted(@Param("name") String name);

    /**
     * 查找所有待处理的导出任务（用于调度）
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = 'PENDING' AND e.deleted = false ORDER BY e.requestedAt ASC")
    List<DataExport> findAllPendingAndNotDeleted();

    /**
     * 统计请求者的导出任务数量
     */
    @Query("SELECT COUNT(e) FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.deleted = false")
    long countByRequestedByAndNotDeleted(@Param("requestedBy") Long requestedBy);

    /**
     * 统计状态的导出任务数量
     */
    @Query("SELECT COUNT(e) FROM DataExport e WHERE e.status = :status AND e.deleted = false")
    long countByStatusAndNotDeleted(@Param("status") String status);
}
