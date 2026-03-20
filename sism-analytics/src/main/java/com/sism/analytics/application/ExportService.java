package com.sism.analytics.application;

import com.sism.analytics.domain.DataExport;
import com.sism.analytics.domain.ExportFormat;
import com.sism.analytics.infrastructure.repository.DataExportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * ExportService - 数据导出服务
 * 提供Excel、CSV、PDF等格式的数据导出功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final DataExportApplicationService dataExportApplicationService;
    private final DataExportRepository dataExportRepository;

    private static final String EXPORT_BASE_PATH = "exports/";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * 导出数据为Excel格式
     */
    @Transactional
    public DataExport exportToExcel(String exportName, String exportType, Long requestedBy,
                                     List<Map<String, Object>> data, List<String> headers) {
        DataExport export = dataExportApplicationService.createDataExport(
                exportName, exportType, ExportFormat.EXCEL.getCode(), requestedBy, null
        );

        try {
            dataExportApplicationService.startProcessing(export.getId());

            String fileName = generateFileName(exportName, ExportFormat.EXCEL);
            String filePath = EXPORT_BASE_PATH + fileName;

            // 确保目录存在
            Files.createDirectories(Paths.get(EXPORT_BASE_PATH));

            // 创建Excel文件
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Data");

                // 创建表头
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers.get(i));
                }

                // 填充数据
                for (int i = 0; i < data.size(); i++) {
                    Row dataRow = sheet.createRow(i + 1);
                    Map<String, Object> rowData = data.get(i);

                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = dataRow.createCell(j);
                        Object value = rowData.get(headers.get(j));
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }

                // 自动调整列宽
                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                }

                // 保存文件
                try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                    workbook.write(fileOut);
                }
            }

            long fileSize = Files.size(Paths.get(filePath));
            return dataExportApplicationService.completeDataExport(export.getId(), filePath, fileSize);

        } catch (Exception e) {
            log.error("Failed to export to Excel: {}", e.getMessage(), e);
            return dataExportApplicationService.failDataExport(export.getId(), e.getMessage());
        }
    }

    /**
     * 导出数据为CSV格式
     */
    @Transactional
    public DataExport exportToCSV(String exportName, String exportType, Long requestedBy,
                                   List<Map<String, Object>> data, List<String> headers) {
        DataExport export = dataExportApplicationService.createDataExport(
                exportName, exportType, ExportFormat.CSV.getCode(), requestedBy, null
        );

        try {
            dataExportApplicationService.startProcessing(export.getId());

            String fileName = generateFileName(exportName, ExportFormat.CSV);
            String filePath = EXPORT_BASE_PATH + fileName;

            // 确保目录存在
            Files.createDirectories(Paths.get(EXPORT_BASE_PATH));

            // 创建CSV文件
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(headers.toArray(new String[0]))
                    .build();

            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath));
                 CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

                for (Map<String, Object> rowData : data) {
                    List<String> values = headers.stream()
                            .map(header -> {
                                Object value = rowData.get(header);
                                return value != null ? value.toString() : "";
                            })
                            .toList();
                    csvPrinter.printRecord(values);
                }
            }

            long fileSize = Files.size(Paths.get(filePath));
            return dataExportApplicationService.completeDataExport(export.getId(), filePath, fileSize);

        } catch (Exception e) {
            log.error("Failed to export to CSV: {}", e.getMessage(), e);
            return dataExportApplicationService.failDataExport(export.getId(), e.getMessage());
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String exportName, ExportFormat format) {
        String safeName = exportName.replaceAll("[^a-zA-Z0-9\u4e00-\u9fa5]", "_");
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        return safeName + "_" + timestamp + format.getFileExtension();
    }

    /**
     * 获取导出文件路径
     */
    public Path getExportFilePath(Long exportId) {
        DataExport export = dataExportApplicationService.findDataExportById(exportId)
                .orElseThrow(() -> new IllegalArgumentException("Export not found: " + exportId));

        if (!export.isDownloadable()) {
            throw new IllegalStateException("Export is not downloadable");
        }

        return Paths.get(export.getFilePath());
    }

    /**
     * 删除导出文件
     */
    public void deleteExportFile(Long exportId) {
        DataExport export = dataExportApplicationService.findDataExportById(exportId)
                .orElseThrow(() -> new IllegalArgumentException("Export not found: " + exportId));

        if (export.getFilePath() != null) {
            try {
                Path path = Paths.get(export.getFilePath());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete export file: {}", e.getMessage());
            }
        }

        dataExportApplicationService.deleteDataExport(exportId);
    }

    /**
     * 清理过期的导出文件
     */
    @Transactional
    public int cleanupExpiredExports(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        List<DataExport> expiredExports = dataExportRepository.findByDateRangeAndNotDeleted(
                LocalDateTime.of(2000, 1, 1, 0, 0),
                cutoffDate
        );

        int cleaned = 0;
        for (DataExport export : expiredExports) {
            if (export.getFilePath() != null) {
                try {
                    Path path = Paths.get(export.getFilePath());
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    log.warn("Failed to delete expired export file: {}", e.getMessage());
                }
            }
            dataExportApplicationService.deleteDataExport(export.getId());
            cleaned++;
        }

        log.info("Cleaned up {} expired exports", cleaned);
        return cleaned;
    }
}
