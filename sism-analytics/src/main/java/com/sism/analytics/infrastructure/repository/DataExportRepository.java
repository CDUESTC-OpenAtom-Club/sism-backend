package com.sism.analytics.infrastructure.repository;

import com.sism.analytics.domain.DataExport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    int DEFAULT_FETCH_SIZE = 1000;

    /**
     * 根据ID查找未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.id = :id AND e.deleted = false")
    Optional<DataExport> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 根据请求者ID查找所有未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.deleted = false ORDER BY e.requestedAt DESC")
    Page<DataExport> findByRequestedByAndNotDeleted(@Param("requestedBy") Long requestedBy, Pageable pageable);

    default List<DataExport> findByRequestedByAndNotDeleted(Long requestedBy) {
        return findByRequestedByAndNotDeleted(requestedBy, PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 根据状态查找所有未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = :status AND e.deleted = false ORDER BY e.requestedAt DESC")
    Page<DataExport> findByStatusAndNotDeleted(@Param("status") String status, Pageable pageable);

    default List<DataExport> findByStatusAndNotDeleted(String status) {
        return findByStatusAndNotDeleted(status, PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 根据请求者ID和状态查找所有未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.status = :status AND e.deleted = false ORDER BY e.requestedAt DESC")
    Page<DataExport> findByRequestedByAndStatusAndNotDeleted(
            @Param("requestedBy") Long requestedBy,
            @Param("status") String status,
            Pageable pageable
    );

    default List<DataExport> findByRequestedByAndStatusAndNotDeleted(Long requestedBy, String status) {
        return findByRequestedByAndStatusAndNotDeleted(requestedBy, status, PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 根据请求者ID和日期范围查找所有未删除的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.requestedAt >= :startDate AND e.requestedAt <= :endDate AND e.deleted = false ORDER BY e.requestedAt DESC")
    Page<DataExport> findByRequestedByAndDateRangeAndNotDeleted(
            @Param("requestedBy") Long requestedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    default List<DataExport> findByRequestedByAndDateRangeAndNotDeleted(Long requestedBy, LocalDateTime startDate, LocalDateTime endDate) {
        return findByRequestedByAndDateRangeAndNotDeleted(requestedBy, startDate, endDate, PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 根据请求者ID和名称模糊查找导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.name LIKE CONCAT('%', :name, '%') ESCAPE '\\' AND e.deleted = false ORDER BY e.requestedAt DESC")
    Page<DataExport> findByRequestedByAndNameContainingAndNotDeleted(
            @Param("requestedBy") Long requestedBy,
            @Param("name") String name,
            Pageable pageable
    );

    default List<DataExport> findByRequestedByAndNameContainingAndNotDeleted(Long requestedBy, String name) {
        return findByRequestedByAndNameContainingAndNotDeleted(requestedBy, name, PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 查找所有可下载的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = 'COMPLETED' AND e.deleted = false ORDER BY e.completedAt DESC")
    Page<DataExport> findAllDownloadableAndNotDeleted(Pageable pageable);

    default List<DataExport> findAllDownloadableAndNotDeleted() {
        return findAllDownloadableAndNotDeleted(PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 查找用户的所有可下载导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.status = 'COMPLETED' AND e.deleted = false ORDER BY e.completedAt DESC")
    Page<DataExport> findDownloadableByRequestedByAndNotDeleted(@Param("requestedBy") Long requestedBy, Pageable pageable);

    default List<DataExport> findDownloadableByRequestedByAndNotDeleted(Long requestedBy) {
        return findDownloadableByRequestedByAndNotDeleted(requestedBy, PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 查找所有失败的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = 'FAILED' AND e.deleted = false ORDER BY e.completedAt DESC")
    Page<DataExport> findAllFailedAndNotDeleted(Pageable pageable);

    default List<DataExport> findAllFailedAndNotDeleted() {
        return findAllFailedAndNotDeleted(PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 查找日期范围内的导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.requestedAt >= :startDate AND e.requestedAt <= :endDate AND e.deleted = false ORDER BY e.requestedAt DESC")
    Page<DataExport> findByDateRangeAndNotDeleted(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    default List<DataExport> findByDateRangeAndNotDeleted(LocalDateTime startDate, LocalDateTime endDate) {
        return findByDateRangeAndNotDeleted(startDate, endDate, PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 根据名称模糊查找导出任务
     */
    @Query("SELECT e FROM DataExport e WHERE e.name LIKE CONCAT('%', :name, '%') ESCAPE '\\' AND e.deleted = false ORDER BY e.requestedAt DESC")
    Page<DataExport> findByNameContainingAndNotDeleted(@Param("name") String name, Pageable pageable);

    default List<DataExport> findByNameContainingAndNotDeleted(String name) {
        return findByNameContainingAndNotDeleted(name, PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 查找所有待处理的导出任务（用于调度）
     */
    @Query("SELECT e FROM DataExport e WHERE e.status = 'PENDING' AND e.deleted = false ORDER BY e.requestedAt ASC")
    Page<DataExport> findAllPendingAndNotDeleted(Pageable pageable);

    default List<DataExport> findAllPendingAndNotDeleted() {
        return findAllPendingAndNotDeleted(PageRequest.of(0, DEFAULT_FETCH_SIZE)).getContent();
    }

    /**
     * 统计请求者的导出任务数量
     */
    @Query("SELECT COUNT(e) FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.deleted = false")
    long countByRequestedByAndNotDeleted(@Param("requestedBy") Long requestedBy);

    /**
     * 统计请求者和状态的导出任务数量
     */
    @Query("SELECT COUNT(e) FROM DataExport e WHERE e.requestedBy = :requestedBy AND e.status = :status AND e.deleted = false")
    long countByRequestedByAndStatusAndNotDeleted(
            @Param("requestedBy") Long requestedBy,
            @Param("status") String status
    );

    /**
     * 统计状态的导出任务数量
     */
    @Query("SELECT COUNT(e) FROM DataExport e WHERE e.status = :status AND e.deleted = false")
    long countByStatusAndNotDeleted(@Param("status") String status);
}
