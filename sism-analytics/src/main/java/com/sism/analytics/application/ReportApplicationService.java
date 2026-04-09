package com.sism.analytics.application;

import com.sism.analytics.domain.Report;
import com.sism.analytics.infrastructure.repository.ReportRepository;
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
 * ReportApplicationService - 分析报告应用服务
 * 负责协调分析报告相关的业务操作
 */
@Service("analyticsReportApplicationService")
@RequiredArgsConstructor
public class ReportApplicationService extends BaseApplicationService {

    private final ReportRepository reportRepository;
    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;

    /**
     * 创建报告
     */
    @Transactional
    public Report createReport(String name, String type, String format, Long generatedBy, String parameters, String description) {
        Report report = Report.create(name, type, format, generatedBy, parameters, description);
        publishAndSaveEvents(report);
        return reportRepository.save(report);
    }

    /**
     * 生成报告
     */
    @Transactional
    public Report generateReport(Long reportId, String filePath, Long fileSize) {
        Report report = findById(reportId);
        report.generate(filePath, fileSize);
        publishAndSaveEvents(report);
        return reportRepository.save(report);
    }

    @Transactional
    public Report generateReport(Long reportId, Long currentUserId, String filePath, Long fileSize) {
        Report report = findOwnedByCurrentUser(reportId, currentUserId);
        report.generate(filePath, fileSize);
        publishAndSaveEvents(report);
        return reportRepository.save(report);
    }

    /**
     * 报告生成失败
     */
    @Transactional
    public Report failReport(Long reportId, String errorMessage) {
        Report report = findById(reportId);
        report.fail(errorMessage);
        publishAndSaveEvents(report);
        return reportRepository.save(report);
    }

    @Transactional
    public Report failReport(Long reportId, Long currentUserId, String errorMessage) {
        Report report = findOwnedByCurrentUser(reportId, currentUserId);
        report.fail(errorMessage);
        publishAndSaveEvents(report);
        return reportRepository.save(report);
    }

    /**
     * 更新报告信息
     */
    @Transactional
    public Report updateReport(Long reportId, String name, String type, String format, String description) {
        Report report = findById(reportId);
        report.update(name, type, format, description);
        publishAndSaveEvents(report);
        return reportRepository.save(report);
    }

    @Transactional
    public Report updateReport(Long reportId, Long currentUserId, String name, String type, String format, String description) {
        Report report = findOwnedByCurrentUser(reportId, currentUserId);
        report.update(name, type, format, description);
        publishAndSaveEvents(report);
        return reportRepository.save(report);
    }

    /**
     * 删除报告
     */
    @Transactional
    public void deleteReport(Long reportId) {
        Report report = findById(reportId);
        report.delete();
        publishAndSaveEvents(report);
        reportRepository.save(report);
    }

    @Transactional
    public void deleteReport(Long reportId, Long currentUserId) {
        Report report = findOwnedByCurrentUser(reportId, currentUserId);
        report.delete();
        publishAndSaveEvents(report);
        reportRepository.save(report);
    }

    /**
     * 查找报告
     */
    public Optional<Report> findReportById(Long reportId) {
        return reportRepository.findByIdAndNotDeleted(reportId);
    }

    public Optional<Report> findReportById(Long reportId, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByIdAndNotDeleted(reportId)
                .filter(report -> currentUserId.equals(report.getGeneratedBy()));
    }

    /**
     * 查找用户的所有报告
     */
    public List<Report> findReportsByGeneratedBy(Long generatedBy) {
        return reportRepository.findByGeneratedByAndNotDeleted(generatedBy);
    }

    public List<Report> findReportsByGeneratedBy(Long generatedBy, Long currentUserId) {
        requireGeneratedByMatchesCurrentUser(generatedBy, currentUserId);
        return reportRepository.findByGeneratedByAndNotDeleted(currentUserId);
    }

    /**
     * 查找所有已生成的报告
     */
    public List<Report> findAllGeneratedReports() {
        return reportRepository.findAllGeneratedAndNotDeleted();
    }

    public List<Report> findAllGeneratedReports(Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByGeneratedByAndNotDeleted(currentUserId);
    }

    public Page<Report> findAllGeneratedReports(Long currentUserId, int pageNum, int pageSize) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByGeneratedByAndNotDeleted(
                currentUserId,
                AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 按类型查找报告
     */
    public List<Report> findReportsByType(String type) {
        return reportRepository.findByTypeAndNotDeleted(type);
    }

    public List<Report> findReportsByType(String type, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByGeneratedByAndTypeAndNotDeleted(currentUserId, type);
    }

    /**
     * 按状态查找报告
     */
    public List<Report> findReportsByStatus(String status) {
        return reportRepository.findByStatusAndNotDeleted(status);
    }

    public List<Report> findReportsByStatus(String status, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByGeneratedByAndStatusAndNotDeleted(currentUserId, status);
    }

    public Page<Report> findReportsByStatus(String status, Long currentUserId, int pageNum, int pageSize) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByGeneratedByAndStatusAndNotDeleted(
                currentUserId,
                status,
                AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 按日期范围查找报告
     */
    public List<Report> findReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        requireValidDateRange(startDate, endDate);
        return reportRepository.findByDateRangeAndNotDeleted(startDate, endDate);
    }

    public List<Report> findReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        requireValidDateRange(startDate, endDate);
        return reportRepository.findByGeneratedByAndDateRangeAndNotDeleted(currentUserId, startDate, endDate);
    }

    /**
     * 按名称搜索报告
     */
    public List<Report> searchReportsByName(String name) {
        return reportRepository.findByNameContainingAndNotDeleted(name);
    }

    public List<Report> searchReportsByName(String name, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByGeneratedByAndNameContainingAndNotDeleted(currentUserId, name);
    }

    public Page<Report> searchReportsByName(String name, Long currentUserId, int pageNum, int pageSize) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByGeneratedByAndNameContainingAndNotDeleted(
                currentUserId,
                name,
                AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 统计用户的报告数量
     */
    public long countReportsByGeneratedBy(Long generatedBy) {
        return reportRepository.countByGeneratedByAndNotDeleted(generatedBy);
    }

    public long countReportsByGeneratedBy(Long generatedBy, Long currentUserId) {
        requireGeneratedByMatchesCurrentUser(generatedBy, currentUserId);
        return reportRepository.countByGeneratedByAndNotDeleted(currentUserId);
    }

    /**
     * 统计状态的报告数量
     */
    public long countReportsByStatus(String status) {
        return reportRepository.countByStatusAndNotDeleted(status);
    }

    public long countReportsByStatus(String status, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.countByGeneratedByAndStatusAndNotDeleted(currentUserId, status);
    }

    /**
     * 获取报告详情
     */
    private Report findById(Long reportId) {
        return reportRepository.findByIdAndNotDeleted(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
    }

    private Report findOwnedByCurrentUser(Long reportId, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return reportRepository.findByIdAndNotDeleted(reportId)
                .filter(report -> currentUserId.equals(report.getGeneratedBy()))
                .orElseThrow(() -> new AccessDeniedException("No permission to access report: " + reportId));
    }

    private void requireGeneratedByMatchesCurrentUser(Long generatedBy, Long currentUserId) {
        requireUserOwnership(generatedBy, currentUserId, "No permission to access another user's reports");
    }

    /**
     * 发布和保存领域事件
     */
    private void publishAndSaveEvents(Report report) {
        List<DomainEvent> events = report.getDomainEvents();
        if (events != null && !events.isEmpty()) {
            for (DomainEvent event : events) {
                eventStore.save(event);
            }
            eventPublisher.publishAll(events);
            report.clearEvents();
        }
    }
}
