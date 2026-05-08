package com.sism.analytics.application;

import com.sism.analytics.domain.DataExport;
import com.sism.analytics.domain.ExportFormat;
import com.sism.analytics.domain.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class AnalyticsFileStorageService {

    @Value("${export.temp-dir:./exports}")
    private String exportBasePath = "./exports";

    public Path prepareManagedExportFile(DataExport export) {
        String extension = ExportFormat.fromCode(export.getFormat()).getFileExtension();
        String safeName = sanitizeFileName(export.getName());
        Path path = resolveExportRoot()
                .resolve("data-exports")
                .resolve(export.getId() + "_" + safeName + extension)
                .normalize();
        ensureInsideRoot(path);
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }
            return path;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare managed export file", e);
        }
    }

    public Path prepareManagedReportFile(Report report) {
        String extension = ExportFormat.fromCode(report.getFormat()).getFileExtension();
        String safeName = sanitizeFileName(report.getName());
        Path path = resolveExportRoot()
                .resolve("reports")
                .resolve(report.getId() + "_" + safeName + extension)
                .normalize();
        ensureInsideRoot(path);
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }
            return path;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to prepare managed report file", e);
        }
    }

    public Path resolveManagedPath(String persistedPath) {
        Path resolved = Paths.get(persistedPath).toAbsolutePath().normalize();
        ensureInsideRoot(resolved);
        return resolved;
    }

    public Path resolveExportRoot() {
        if (exportBasePath == null || exportBasePath.isBlank()) {
            throw new IllegalStateException("export.temp-dir is not configured");
        }
        Path resolvedRoot = Paths.get(exportBasePath.trim()).toAbsolutePath().normalize();
        Path filesystemRoot = resolvedRoot.getRoot();
        if (filesystemRoot == null || resolvedRoot.equals(filesystemRoot)) {
            throw new IllegalStateException("export.temp-dir cannot point to filesystem root");
        }
        return resolvedRoot;
    }

    private void ensureInsideRoot(Path candidate) {
        Path root = resolveExportRoot();
        if (!candidate.startsWith(root)) {
            throw new IllegalArgumentException("Resolved file path escapes configured export directory");
        }
    }

    private String sanitizeFileName(String value) {
        return value == null
                ? "analytics-file"
                : value.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5._-]", "_");
    }
}
