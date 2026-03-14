package com.sism.analytics.domain;

import com.sism.shared.domain.model.base.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Report Aggregate Root Tests")
class ReportTest {

    @Test
    @DisplayName("Should create Report with valid parameters")
    void shouldCreateReportWithValidParameters() {
        Report report = Report.create(
                "战略分析报告",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                "{\"param1\":\"value1\"}",
                "2024年战略执行情况分析"
        );

        assertNotNull(report);
        assertEquals("战略分析报告", report.getName());
        assertEquals(Report.TYPE_STRATEGIC, report.getType());
        assertEquals(Report.FORMAT_PDF, report.getFormat());
        assertEquals(1L, report.getGeneratedBy());
        assertEquals("{\"param1\":\"value1\"}", report.getParameters());
        assertEquals("2024年战略执行情况分析", report.getDescription());
        assertEquals(Report.STATUS_DRAFT, report.getStatus());
        assertNotNull(report.getCreatedAt());
    }

    @Test
    @DisplayName("Should throw exception when creating Report with null parameters")
    void shouldThrowExceptionWhenCreatingReportWithNullParameters() {
        assertThrows(IllegalArgumentException.class, () ->
                Report.create(null, Report.TYPE_STRATEGIC, Report.FORMAT_PDF, 1L, null, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                Report.create("报告名称", null, Report.FORMAT_PDF, 1L, null, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                Report.create("报告名称", Report.TYPE_STRATEGIC, null, 1L, null, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                Report.create("报告名称", Report.TYPE_STRATEGIC, Report.FORMAT_PDF, null, null, null)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating Report with invalid type")
    void shouldThrowExceptionWhenCreatingReportWithInvalidType() {
        assertThrows(IllegalArgumentException.class, () ->
                Report.create("报告名称", "INVALID_TYPE", Report.FORMAT_PDF, 1L, null, null)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating Report with invalid format")
    void shouldThrowExceptionWhenCreatingReportWithInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () ->
                Report.create("报告名称", Report.TYPE_STRATEGIC, "INVALID_FORMAT", 1L, null, null)
        );
    }

    @Test
    @DisplayName("Should validate Report with valid parameters")
    void shouldValidateReportWithValidParameters() {
        Report report = Report.create(
                "测试报告",
                Report.TYPE_EXECUTION,
                Report.FORMAT_EXCEL,
                1L,
                null,
                null
        );

        assertDoesNotThrow(report::validate);
    }

    @Test
    @DisplayName("Should generate Report successfully")
    void shouldGenerateReportSuccessfully() {
        Report report = Report.create(
                "财务报表",
                Report.TYPE_FINANCIAL,
                Report.FORMAT_PDF,
                2L,
                "{\"year\":2024,\"quarter\":1}",
                "2024年第一季度财务分析"
        );

        report.generate("/reports/financial_2024_q1.pdf", 204800L);

        assertEquals(Report.STATUS_GENERATED, report.getStatus());
        assertEquals("/reports/financial_2024_q1.pdf", report.getFilePath());
        assertEquals(204800L, report.getFileSize());
        assertNotNull(report.getGeneratedAt());
    }

    @Test
    @DisplayName("Should publish ReportGeneratedEvent when report is generated")
    void shouldPublishReportGeneratedEvent() {
        Report report = Report.create(
                "测试报告",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                null,
                null
        );
        report.setId(1L);

        report.generate("/path/to/report.pdf", 1024L);

        List<DomainEvent> domainEvents = report.getDomainEvents();
        assertFalse(domainEvents.isEmpty());
        assertTrue(domainEvents.stream()
                .anyMatch(event -> "ReportGenerated".equals(event.getEventType())));
    }

    @Test
    @DisplayName("Should fail Report generation")
    void shouldFailReportGeneration() {
        Report report = Report.create(
                "失败报告",
                Report.TYPE_OPERATIONAL,
                Report.FORMAT_EXCEL,
                3L,
                "{\"param\":\"value\"}",
                "测试失败场景"
        );

        report.fail();

        assertEquals(Report.STATUS_FAILED, report.getStatus());
    }

    @Test
    @DisplayName("Should publish ReportGenerationFailedEvent when report generation fails")
    void shouldPublishReportGenerationFailedEvent() {
        Report report = Report.create(
                "失败报告",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                null,
                null
        );
        report.setId(1L);

        report.fail();

        List<DomainEvent> domainEvents = report.getDomainEvents();
        assertFalse(domainEvents.isEmpty());
        assertTrue(domainEvents.stream()
                .anyMatch(event -> "ReportGenerationFailed".equals(event.getEventType())));
    }

    @Test
    @DisplayName("Should reject report generation when already failed")
    void shouldRejectReportGenerationWhenFailed() {
        Report report = Report.create(
                "失败的报告",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                null,
                null
        );
        report.setId(1L);
        report.fail();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                report.generate("/path/to/report.pdf", 1024L));

        assertTrue(exception.getMessage().contains("Cannot regenerate a failed report"));
    }

    @Test
    @DisplayName("Should update Report information")
    void shouldUpdateReportInformation() {
        Report report = Report.create(
                "原始报告",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                null,
                null
        );

        report.update(
                "更新后的报告",
                Report.TYPE_EXECUTION,
                Report.FORMAT_EXCEL,
                "更新后的描述"
        );

        assertEquals("更新后的报告", report.getName());
        assertEquals(Report.TYPE_EXECUTION, report.getType());
        assertEquals(Report.FORMAT_EXCEL, report.getFormat());
        assertEquals("更新后的描述", report.getDescription());
        assertNotNull(report.getUpdatedAt());
    }

    @Test
    @DisplayName("Should reject report updates when already generated")
    void shouldRejectReportUpdatesWhenGenerated() {
        Report report = Report.create(
                "已生成的报告",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                null,
                null
        );
        report.setId(1L);
        report.generate("/path/to/report.pdf", 1024L);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                report.update("更新的报告", Report.TYPE_EXECUTION, Report.FORMAT_EXCEL, "更新的描述"));

        assertTrue(exception.getMessage().contains("Cannot update a generated report"));
    }

    @Test
    @DisplayName("Should delete Report")
    void shouldDeleteReport() {
        Report report = Report.create(
                "待删除报告",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                null,
                null
        );

        report.delete();

        assertTrue(report.isDeleted());
        assertNotNull(report.getUpdatedAt());
    }

    @Test
    @DisplayName("Should compare Reports for equality")
    void shouldCompareReportsForEquality() {
        Report report1 = Report.create(
                "报告1",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                null,
                null
        );
        report1.setId(1L);

        Report report2 = Report.create(
                "报告1",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                null,
                null
        );
        report2.setId(2L);

        Report report3 = Report.create(
                "报告2",
                Report.TYPE_EXECUTION,
                Report.FORMAT_EXCEL,
                2L,
                null,
                null
        );
        report3.setId(3L);

        assertNotEquals(report1, report2);
        assertNotEquals(report1, report3);
    }

    @Test
    @DisplayName("Should generate Report with large parameters")
    void shouldGenerateReportWithLargeParameters() {
        String largeParameters = "{\"data\":" + "1".repeat(1000) + "}";

        Report report = Report.create(
                "大参数报告",
                Report.TYPE_COMPREHENSIVE,
                Report.FORMAT_PDF,
                1L,
                largeParameters,
                "包含大量数据的报告"
        );

        assertNotNull(report.getParameters());
        assertTrue(report.getParameters().length() > 1000);
    }
}
