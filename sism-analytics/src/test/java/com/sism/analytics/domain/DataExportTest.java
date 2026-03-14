package com.sism.analytics.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DataExport Aggregate Root Tests")
class DataExportTest {

    @Test
    @DisplayName("Should create DataExport with valid parameters")
    void shouldCreateDataExportWithValidParameters() {
        DataExport dataExport = DataExport.create(
                "指标数据导出",
                "INDICATOR_DATA",
                DataExport.FORMAT_EXCEL,
                1L,
                "{\"indicatorId\":1001,\"startDate\":\"2024-01-01\",\"endDate\":\"2024-12-31\"}"
        );

        assertNotNull(dataExport);
        assertEquals("指标数据导出", dataExport.getName());
        assertEquals("INDICATOR_DATA", dataExport.getType());
        assertEquals(DataExport.FORMAT_EXCEL, dataExport.getFormat());
        assertEquals(1L, dataExport.getRequestedBy());
        assertEquals(DataExport.STATUS_PENDING, dataExport.getStatus());
        assertNotNull(dataExport.getRequestedAt());
        assertNotNull(dataExport.getCreatedAt());
    }

    @Test
    @DisplayName("Should throw exception when creating DataExport with null parameters")
    void shouldThrowExceptionWhenCreatingDataExportWithNullParameters() {
        assertThrows(IllegalArgumentException.class, () ->
                DataExport.create(null, "INDICATOR_DATA", DataExport.FORMAT_EXCEL, 1L, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                DataExport.create("导出名称", null, DataExport.FORMAT_EXCEL, 1L, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                DataExport.create("导出名称", "INDICATOR_DATA", null, 1L, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                DataExport.create("导出名称", "INDICATOR_DATA", DataExport.FORMAT_EXCEL, null, null)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating DataExport with invalid format")
    void shouldThrowExceptionWhenCreatingDataExportWithInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () ->
                DataExport.create("数据导出", "INDICATOR_DATA", "INVALID_FORMAT", 1L, null)
        );
    }

    @Test
    @DisplayName("Should start processing DataExport")
    void shouldStartProcessingDataExport() {
        DataExport dataExport = DataExport.create(
                "测试数据导出",
                "TEST_DATA",
                DataExport.FORMAT_CSV,
                1L,
                null
        );

        dataExport.startProcessing();

        assertEquals(DataExport.STATUS_PROCESSING, dataExport.getStatus());
        assertNotNull(dataExport.getStartedAt());
    }

    @Test
    @DisplayName("Should complete DataExport successfully")
    void shouldCompleteDataExportSuccessfully() {
        DataExport dataExport = DataExport.create(
                "完成的导出",
                "COMPLETED_DATA",
                DataExport.FORMAT_EXCEL,
                1L,
                null
        );

        dataExport.startProcessing();
        dataExport.complete("/exports/completed_data.xlsx", 1024000L);

        assertEquals(DataExport.STATUS_COMPLETED, dataExport.getStatus());
        assertEquals("/exports/completed_data.xlsx", dataExport.getFilePath());
        assertEquals(1024000L, dataExport.getFileSize());
        assertNotNull(dataExport.getCompletedAt());
        assertTrue(dataExport.isDownloadable());
    }

    @Test
    @DisplayName("Should fail DataExport processing")
    void shouldFailDataExportProcessing() {
        DataExport dataExport = DataExport.create(
                "失败的导出",
                "FAILED_DATA",
                DataExport.FORMAT_PDF,
                1L,
                null
        );

        dataExport.startProcessing();
        dataExport.fail("数据库连接超时");

        assertEquals(DataExport.STATUS_FAILED, dataExport.getStatus());
        assertEquals("数据库连接超时", dataExport.getErrorMessage());
        assertNotNull(dataExport.getCompletedAt());
        assertFalse(dataExport.isDownloadable());
        assertTrue(dataExport.isRetryable());
    }

    @Test
    @DisplayName("Should retry failed DataExport")
    void shouldRetryFailedDataExport() {
        DataExport dataExport = DataExport.create(
                "可重试的导出",
                "RETRY_DATA",
                DataExport.FORMAT_CSV,
                1L,
                null
        );

        dataExport.startProcessing();
        dataExport.fail("网络错误");

        dataExport.retry();

        assertEquals(DataExport.STATUS_PENDING, dataExport.getStatus());
        assertNull(dataExport.getErrorMessage());
        assertNull(dataExport.getStartedAt());
        assertNull(dataExport.getCompletedAt());
        assertNull(dataExport.getFilePath());
        assertNull(dataExport.getFileSize());
        assertNotNull(dataExport.getRequestedAt());
    }

    @Test
    @DisplayName("Should delete DataExport")
    void shouldDeleteDataExport() {
        DataExport dataExport = DataExport.create(
                "待删除的导出",
                "DELETE_DATA",
                DataExport.FORMAT_EXCEL,
                1L,
                null
        );

        dataExport.delete();

        assertTrue(dataExport.isDeleted());
        assertNotNull(dataExport.getUpdatedAt());
    }

    @Test
    @DisplayName("Should calculate processing time correctly")
    void shouldCalculateProcessingTimeCorrectly() throws InterruptedException {
        DataExport dataExport = DataExport.create(
                "计时导出",
                "TIMING_DATA",
                DataExport.FORMAT_PDF,
                1L,
                null
        );

        dataExport.startProcessing();
        Thread.sleep(100);
        dataExport.complete("/exports/timing_report.pdf", 512000L);

        Long processingTime = dataExport.getProcessingTimeInSeconds();
        assertNotNull(processingTime);
        assertTrue(processingTime >= 0 && processingTime <= 1);
    }

    @Test
    @DisplayName("Should validate DataExport with valid parameters")
    void shouldValidateDataExportWithValidParameters() {
        DataExport dataExport = DataExport.create(
                "验证测试",
                "VALIDATION_DATA",
                DataExport.FORMAT_CSV,
                1L,
                null
        );

        assertDoesNotThrow(dataExport::validate);
    }
}
