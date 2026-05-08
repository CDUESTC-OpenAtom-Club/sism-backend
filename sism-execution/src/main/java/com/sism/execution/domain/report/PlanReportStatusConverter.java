package com.sism.execution.domain.report;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * PlanReportStatus 与数据库字符串的双向转换。
 */
@Converter(autoApply = false)
public class PlanReportStatusConverter implements AttributeConverter<PlanReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(PlanReportStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public PlanReportStatus convertToEntityAttribute(String dbData) {
        return PlanReportStatus.from(dbData);
    }
}
