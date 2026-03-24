package com.sism.strategy.interfaces.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Accept both ISO local date-time and plain date strings for milestone due dates.
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getValueAsString();
        if (raw == null) {
            return null;
        }

        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            if (!normalized.contains("T")) {
                return LocalDate.parse(normalized).atStartOfDay();
            }
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException exception) {
            return (LocalDateTime) context.handleWeirdStringValue(
                    LocalDateTime.class,
                    normalized,
                    "Expected 'yyyy-MM-dd' or ISO local date-time string"
            );
        }
    }
}
