package com.sism.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CacheUtilsTest {

    @AfterEach
    void tearDown() {
        CacheUtils.resetObjectMapperForTests();
    }

    @Test
    void shouldInitializeObjectMapperOnlyOnce() {
        CacheUtils.resetObjectMapperForTests();

        ObjectMapper firstMapper = new ObjectMapper();
        ObjectMapper secondMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        CacheUtils.setObjectMapper(firstMapper);
        String firstTag = CacheUtils.generateETag(new SamplePayload("alpha", null));

        CacheUtils.setObjectMapper(secondMapper);
        String secondTag = CacheUtils.generateETag(new SamplePayload("alpha", null));

        assertEquals(firstTag, secondTag);
    }

    @Test
    void shouldTreatSameSecondLastModifiedAsNotModified() {
        Instant lastModified = Instant.parse("2026-04-07T10:15:30.900Z");
        String headerValue = CacheUtils.formatHttpDate(Instant.parse("2026-04-07T10:15:30.000Z"));

        assertFalse(CacheUtils.isModifiedSince(headerValue, lastModified));
    }

    @Test
    void shouldReturnNotModifiedResponseWhenHeaderMatchesSameSecondTimestamp() {
        Instant lastModified = Instant.parse("2026-04-07T10:15:30.900Z");
        String headerValue = CacheUtils.formatHttpDate(Instant.parse("2026-04-07T10:15:30.000Z"));

        ResponseEntity<String> response = CacheUtils.buildLastModifiedResponse("payload", lastModified, headerValue);

        assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLastModified());
    }

    @Test
    void shouldTruncateHttpDateFormattingToSecondPrecision() {
        String httpDate = CacheUtils.formatHttpDate(Instant.parse("2026-04-07T10:15:30.987Z"));

        assertEquals("Tue, 7 Apr 2026 10:15:30 GMT", httpDate);
    }

    private record SamplePayload(String name, String note) {
    }
}
