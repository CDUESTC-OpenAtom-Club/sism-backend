package com.sism.analytics.application;

import com.sism.analytics.domain.Report;
import com.sism.analytics.infrastructure.repository.ReportRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
public class ReportApplicationService {

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

    /**
     * 报告生成失败
     */
    @Transactional
    public Report failReport(Long reportId, String errorMessage) {
        Report report = findById(reportId);
        report.fail();
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

    /**
     * 查找报告
     */
    public Optional<Report> findReportById(Long reportId) {
        return reportRepository.findByIdAndNotDeleted(reportId);
    }

    /**
     * 查找用户的所有报告
     */
    public List<Report> findReportsByGeneratedBy(Long generatedBy) {
        return reportRepository.findByGeneratedByAndNotDeleted(generatedBy);
    }

    /**
     * 查找所有已生成的报告
     */
    public List<Report> findAllGeneratedReports() {
        return reportRepository.findAllGeneratedAndNotDeleted();
    }

    /**
     * 按类型查找报告
     */
    public List<Report> findReportsByType(String type) {
        return reportRepository.findByTypeAndNotDeleted(type);
    }

    /**
     * 按状态查找报告
     */
    public List<Report> findReportsByStatus(String status) {
        return reportRepository.findByStatusAndNotDeleted(status);
    }

    /**
     * 按日期范围查找报告
     */
    public List<Report> findReportsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return reportRepository.findByDateRangeAndNotDeleted(startDate, endDate);
    }

    /**
     * 按名称搜索报告
     */
    public List<Report> searchReportsByName(String name) {
        return reportRepository.findByNameContainingAndNotDeleted(name);
    }

    /**
     * 统计用户的报告数量
     */
    public long countReportsByGeneratedBy(Long generatedBy) {
        return reportRepository.countByGeneratedByAndNotDeleted(generatedBy);
    }

    /**
     * 统计状态的报告数量
     */
    public long countReportsByStatus(String status) {
        return reportRepository.countByStatusAndNotDeleted(status);
    }

    /**
     * 获取报告详情
     */
    private Report findById(Long reportId) {
        return reportRepository.findByIdAndNotDeleted(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: " + reportId));
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
