package com.sism.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingUtilsTest {

    @Test
    void formatDetailsShouldRedactSensitiveValues() throws Exception {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("user", "alice");
        details.put("password", "super-secret");
        details.put("apiToken", "abc123");
        details.put("note", "hello");

        String formatted = invokeFormatDetails(details);

        assertEquals("{user=alice, password=[REDACTED], apiToken=[REDACTED], note=hello}", formatted);
    }

    @Test
    void formatDetailsShouldTruncateLongNonSensitiveValues() throws Exception {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("note", "x".repeat(210));

        String formatted = invokeFormatDetails(details);

        assertTrue(formatted.contains("note=" + "x".repeat(200) + "..."));
    }

    private String invokeFormatDetails(Map<String, Object> details) throws Exception {
        Method method = LoggingUtils.class.getDeclaredMethod("formatDetails", Map.class);
        method.setAccessible(true);
        return (String) method.invoke(null, details);
    }
}
