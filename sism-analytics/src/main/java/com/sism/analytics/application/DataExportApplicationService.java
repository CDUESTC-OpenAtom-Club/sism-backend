package com.sism.analytics.application;

import com.sism.analytics.domain.DataExport;
import com.sism.analytics.infrastructure.repository.DataExportRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DataExportApplicationService - 数据导出应用服务
 * 负责协调数据导出相关的业务操作
 */
@Service
@RequiredArgsConstructor
public class DataExportApplicationService extends BaseApplicationService {

    private final DataExportRepository dataExportRepository;
    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;

    /**
     * 创建导出任务
     */
    @Transactional
    public DataExport createDataExport(String name, String type, String format, Long requestedBy, String parameters) {
        DataExport dataExport = DataExport.create(name, type, format, requestedBy, parameters);
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    /**
     * 开始导出处理
     */
    @Transactional
    public DataExport startProcessing(Long exportId) {
        DataExport dataExport = findById(exportId);
        dataExport.startProcessing();
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    @Transactional
    public DataExport startProcessing(Long exportId, Long currentUserId) {
        DataExport dataExport = findOwnedByCurrentUser(exportId, currentUserId);
        dataExport.startProcessing();
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    /**
     * 完成导出
     */
    @Transactional
    public DataExport completeDataExport(Long exportId, String filePath, Long fileSize) {
        DataExport dataExport = findById(exportId);
        dataExport.complete(filePath, fileSize);
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    @Transactional
    public DataExport completeDataExport(Long exportId, Long currentUserId, String filePath, Long fileSize) {
        DataExport dataExport = findOwnedByCurrentUser(exportId, currentUserId);
        dataExport.complete(filePath, fileSize);
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    /**
     * 导出失败
     */
    @Transactional
    public DataExport failDataExport(Long exportId, String errorMessage) {
        DataExport dataExport = findById(exportId);
        dataExport.fail(errorMessage);
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    @Transactional
    public DataExport failDataExport(Long exportId, Long currentUserId, String errorMessage) {
        DataExport dataExport = findOwnedByCurrentUser(exportId, currentUserId);
        dataExport.fail(errorMessage);
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    /**
     * 重试导出
     */
    @Transactional
    public DataExport retryDataExport(Long exportId) {
        DataExport dataExport = findById(exportId);
        dataExport.retry();
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    @Transactional
    public DataExport retryDataExport(Long exportId, Long currentUserId) {
        DataExport dataExport = findOwnedByCurrentUser(exportId, currentUserId);
        dataExport.retry();
        publishAndSaveEvents(dataExport);
        return dataExportRepository.save(dataExport);
    }

    /**
     * 删除导出任务
     */
    @Transactional
    public void deleteDataExport(Long exportId) {
        DataExport dataExport = findById(exportId);
        dataExport.delete();
        publishAndSaveEvents(dataExport);
        dataExportRepository.save(dataExport);
    }

    @Transactional
    public void deleteDataExport(Long exportId, Long currentUserId) {
        DataExport dataExport = findOwnedByCurrentUser(exportId, currentUserId);
        dataExport.delete();
        publishAndSaveEvents(dataExport);
        dataExportRepository.save(dataExport);
    }

    /**
     * 查找导出任务
     */
    public Optional<DataExport> findDataExportById(Long exportId) {
        return dataExportRepository.findByIdAndNotDeleted(exportId);
    }

    public Optional<DataExport> findDataExportById(Long exportId, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByIdAndNotDeleted(exportId)
                .filter(dataExport -> currentUserId.equals(dataExport.getRequestedBy()));
    }

    /**
     * 查找用户的所有导出任务
     */
    public List<DataExport> findDataExportsByRequestedBy(Long requestedBy) {
        return dataExportRepository.findByRequestedByAndNotDeleted(requestedBy);
    }

    public List<DataExport> findDataExportsByRequestedBy(Long requestedBy, Long currentUserId) {
        requireRequestedUserMatchesCurrentUser(requestedBy, currentUserId);
        return dataExportRepository.findByRequestedByAndNotDeleted(currentUserId);
    }

    /**
     * 查找用户的所有可下载导出任务
     */
    public List<DataExport> findDownloadableByRequestedBy(Long requestedBy) {
        return dataExportRepository.findDownloadableByRequestedByAndNotDeleted(requestedBy);
    }

    public List<DataExport> findDownloadableByRequestedBy(Long requestedBy, Long currentUserId) {
        requireRequestedUserMatchesCurrentUser(requestedBy, currentUserId);
        return dataExportRepository.findDownloadableByRequestedByAndNotDeleted(currentUserId);
    }

    /**
     * 查找所有可下载的导出任务
     */
    public List<DataExport> findAllDownloadable() {
        return dataExportRepository.findAllDownloadableAndNotDeleted();
    }

    public List<DataExport> findAllDownloadable(Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findDownloadableByRequestedByAndNotDeleted(currentUserId);
    }

    public Page<DataExport> findAllDownloadable(Long currentUserId, int pageNum, int pageSize) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findDownloadableByRequestedByAndNotDeleted(
                currentUserId,
                AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 查找所有待处理的导出任务（用于调度）
     */
    public List<DataExport> findAllPending() {
        return dataExportRepository.findAllPendingAndNotDeleted();
    }

    public List<DataExport> findAllPending(Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByRequestedByAndStatusAndNotDeleted(currentUserId, DataExport.STATUS_PENDING);
    }

    /**
     * 查找所有失败的导出任务
     */
    public List<DataExport> findAllFailed() {
        return dataExportRepository.findAllFailedAndNotDeleted();
    }

    public List<DataExport> findAllFailed(Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByRequestedByAndStatusAndNotDeleted(currentUserId, DataExport.STATUS_FAILED);
    }

    public Page<DataExport> findAllFailed(Long currentUserId, int pageNum, int pageSize) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByRequestedByAndStatusAndNotDeleted(
                currentUserId,
                DataExport.STATUS_FAILED,
                AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 查找所有可重试的导出任务
     */
    public List<DataExport> findAllRetryable() {
        return findAllFailed();
    }

    public List<DataExport> findAllRetryable(Long currentUserId) {
        return findAllFailed(currentUserId);
    }

    /**
     * 按状态查找导出任务
     */
    public List<DataExport> findDataExportsByStatus(String status) {
        return dataExportRepository.findByStatusAndNotDeleted(status);
    }

    public List<DataExport> findDataExportsByStatus(String status, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByRequestedByAndStatusAndNotDeleted(currentUserId, status);
    }

    public Page<DataExport> findDataExportsByStatus(String status, Long currentUserId, int pageNum, int pageSize) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByRequestedByAndStatusAndNotDeleted(
                currentUserId,
                status,
                AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 按用户和状态查找导出任务
     */
    public List<DataExport> findDataExportsByRequestedByAndStatus(Long requestedBy, String status) {
        return dataExportRepository.findByRequestedByAndStatusAndNotDeleted(requestedBy, status);
    }

    public List<DataExport> findDataExportsByRequestedByAndStatus(Long requestedBy, String status, Long currentUserId) {
        requireRequestedUserMatchesCurrentUser(requestedBy, currentUserId);
        return dataExportRepository.findByRequestedByAndStatusAndNotDeleted(currentUserId, status);
    }

    /**
     * 按日期范围查找导出任务
     */
    public List<DataExport> findDataExportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        requireValidDateRange(startDate, endDate);
        return dataExportRepository.findByDateRangeAndNotDeleted(startDate, endDate);
    }

    public List<DataExport> findDataExportsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        requireValidDateRange(startDate, endDate);
        return dataExportRepository.findByRequestedByAndDateRangeAndNotDeleted(currentUserId, startDate, endDate);
    }

    /**
     * 按名称搜索导出任务
     */
    public List<DataExport> searchDataExportsByName(String name) {
        return dataExportRepository.findByNameContainingAndNotDeleted(name);
    }

    public List<DataExport> searchDataExportsByName(String name, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByRequestedByAndNameContainingAndNotDeleted(currentUserId, name);
    }

    public Page<DataExport> searchDataExportsByName(String name, Long currentUserId, int pageNum, int pageSize) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByRequestedByAndNameContainingAndNotDeleted(
                currentUserId,
                name,
                AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 统计用户的导出任务数量
     */
    public long countDataExportsByRequestedBy(Long requestedBy) {
        return dataExportRepository.countByRequestedByAndNotDeleted(requestedBy);
    }

    public long countDataExportsByRequestedBy(Long requestedBy, Long currentUserId) {
        requireRequestedUserMatchesCurrentUser(requestedBy, currentUserId);
        return dataExportRepository.countByRequestedByAndNotDeleted(currentUserId);
    }

    /**
     * 统计状态的导出任务数量
     */
    public long countDataExportsByStatus(String status) {
        return dataExportRepository.countByStatusAndNotDeleted(status);
    }

    public long countDataExportsByStatus(String status, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.countByRequestedByAndStatusAndNotDeleted(currentUserId, status);
    }

    /**
     * 获取导出任务详情
     */
    private DataExport findById(Long exportId) {
        return dataExportRepository.findByIdAndNotDeleted(exportId)
                .orElseThrow(() -> new IllegalArgumentException("DataExport not found: " + exportId));
    }

    private DataExport findOwnedByCurrentUser(Long exportId, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dataExportRepository.findByIdAndNotDeleted(exportId)
                .filter(dataExport -> currentUserId.equals(dataExport.getRequestedBy()))
                .orElseThrow(() -> new AccessDeniedException("No permission to access export: " + exportId));
    }

    private void requireRequestedUserMatchesCurrentUser(Long requestedBy, Long currentUserId) {
        requireUserOwnership(requestedBy, currentUserId, "No permission to access another user's exports");
    }

    /**
     * 发布和保存领域事件
     */
    private void publishAndSaveEvents(DataExport dataExport) {
        List<DomainEvent> events = dataExport.getDomainEvents();
        if (events != null && !events.isEmpty()) {
            for (DomainEvent event : events) {
                eventStore.save(event);
            }
            eventPublisher.publishAll(events);
            dataExport.clearEvents();
        }
    }
}
