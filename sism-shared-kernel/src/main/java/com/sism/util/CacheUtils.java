package com.sism.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility class for HTTP caching support
 * 
 * Provides methods for:
 * - Generating ETags from response data
 * - Handling If-None-Match validation
 * - Generating Last-Modified headers
 * - Handling If-Modified-Since validation
 * 
 * **Validates: Requirements 4.2.1, 4.2.4**
 */
@Slf4j
public class CacheUtils {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    private static volatile ObjectMapper objectMapper = DEFAULT_OBJECT_MAPPER;
    private static final AtomicBoolean OBJECT_MAPPER_INITIALIZED = new AtomicBoolean(false);
    private static final DateTimeFormatter HTTP_DATE_FORMATTER =
        DateTimeFormatter.RFC_1123_DATE_TIME;

    /**
     * Override the ObjectMapper used by the static cache helpers.
     * Spring configuration can wire the shared application ObjectMapper here
     * without changing the public static API.
     */
    public static synchronized void setObjectMapper(ObjectMapper mapper) {
        ObjectMapper next = mapper != null ? mapper : DEFAULT_OBJECT_MAPPER;
        if (OBJECT_MAPPER_INITIALIZED.get()) {
            if (objectMapper != next) {
                log.warn("CacheUtils ObjectMapper already initialized, ignoring subsequent update");
            }
            return;
        }
        objectMapper = next;
        OBJECT_MAPPER_INITIALIZED.set(true);
    }

    static synchronized void resetObjectMapperForTests() {
        objectMapper = DEFAULT_OBJECT_MAPPER;
        OBJECT_MAPPER_INITIALIZED.set(false);
    }

    /**
     * Generate ETag from object content using SHA-256 hash
     * 
     * @param data the data to generate ETag from
     * @return ETag string (weak ETag format: W/"hash")
     */
    public static String generateETag(Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            String hashHex = HexFormat.of().formatHex(hash).substring(0, 16);
            return "W/\"" + hashHex + "\"";
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.warn("Failed to generate ETag: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if the client's ETag matches the current ETag
     * 
     * @param clientETag the ETag from If-None-Match header
     * @param currentETag the current ETag of the resource
     * @return true if ETags match (304 should be returned)
     */
    public static boolean etagMatches(String clientETag, String currentETag) {
        if (clientETag == null || currentETag == null) {
            return false;
        }
        // Handle multiple ETags in If-None-Match
        String[] clientETags = clientETag.split(",");
        for (String tag : clientETags) {
            String trimmed = tag.trim();
            if (trimmed.equals("*") || trimmed.equals(currentETag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format timestamp as HTTP date for Last-Modified header
     * 
     * @param timestamp the timestamp in milliseconds
     * @return HTTP date string
     */
    public static String formatHttpDate(long timestamp) {
        ZonedDateTime dateTime = truncateToHttpSecondPrecision(Instant.ofEpochMilli(timestamp))
            .atZone(ZoneId.of("GMT"));
        return HTTP_DATE_FORMATTER.format(dateTime);
    }

    /**
     * Format Instant as HTTP date for Last-Modified header
     * 
     * @param instant the instant to format
     * @return HTTP date string
     */
    public static String formatHttpDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        ZonedDateTime dateTime = truncateToHttpSecondPrecision(instant).atZone(ZoneId.of("GMT"));
        return HTTP_DATE_FORMATTER.format(dateTime);
    }

    /**
     * Parse HTTP date from If-Modified-Since header
     * 
     * @param httpDate the HTTP date string
     * @return Instant or null if parsing fails
     */
    public static Instant parseHttpDate(String httpDate) {
        if (httpDate == null || httpDate.isEmpty()) {
            return null;
        }
        try {
            ZonedDateTime dateTime = ZonedDateTime.parse(httpDate, HTTP_DATE_FORMATTER);
            return dateTime.toInstant();
        } catch (Exception e) {
            log.warn("Failed to parse HTTP date: {}", httpDate);
            return null;
        }
    }

    /**
     * Check if resource has been modified since the given date
     * 
     * @param ifModifiedSince the If-Modified-Since header value
     * @param lastModified the last modification time of the resource
     * @return true if resource has been modified (200 should be returned)
     */
    public static boolean isModifiedSince(String ifModifiedSince, Instant lastModified) {
        if (ifModifiedSince == null || lastModified == null) {
            return true; // No cache validation, return fresh data
        }
        Instant clientDate = parseHttpDate(ifModifiedSince);
        if (clientDate == null) {
            return true;
        }
        // HTTP dates are second precision, so compare at the same granularity.
        return truncateToHttpSecondPrecision(lastModified).isAfter(clientDate);
    }

    /**
     * Build a cacheable response with ETag
     * 
     * @param data the response data
     * @param ifNoneMatch the If-None-Match header from request
     * @param <T> the type of response data
     * @return ResponseEntity with appropriate status and headers
     */
    public static <T> ResponseEntity<T> buildETagResponse(
            T data,
            String ifNoneMatch) {
        String etag = generateETag(data);

        if (etag != null && etagMatches(ifNoneMatch, etag)) {
            log.debug("ETag match, returning 304 Not Modified");
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .eTag(etag)
                .build();
        }

        HttpHeaders headers = new HttpHeaders();
        if (etag != null) {
            headers.setETag(etag);
        }
        // Cache-Control: max-age=300 (5 minutes)
        headers.setCacheControl("max-age=300, must-revalidate");

        return ResponseEntity.ok()
            .headers(headers)
            .body(data);
    }

    /**
     * Build a cacheable response with Last-Modified
     * 
     * @param data the response data
     * @param lastModified the last modification time
     * @param ifModifiedSince the If-Modified-Since header from request
     * @param <T> the type of response data
     * @return ResponseEntity with appropriate status and headers
     */
    public static <T> ResponseEntity<T> buildLastModifiedResponse(
            T data,
            Instant lastModified,
            String ifModifiedSince) {

        if (!isModifiedSince(ifModifiedSince, lastModified)) {
            log.debug("Resource not modified since {}, returning 304", ifModifiedSince);
            HttpHeaders headers = new HttpHeaders();
            if (lastModified != null) {
                headers.setLastModified(lastModified);
            }
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .headers(headers)
                .build();
        }

        HttpHeaders headers = new HttpHeaders();
        if (lastModified != null) {
            headers.setLastModified(lastModified);
        }
        // Cache-Control: max-age=120 (2 minutes)
        headers.setCacheControl("max-age=120, must-revalidate");

        return ResponseEntity.ok()
            .headers(headers)
            .body(data);
    }

    /**
     * Build a cacheable response with both ETag and Last-Modified
     * 
     * @param data the response data
     * @param lastModified the last modification time
     * @param ifNoneMatch the If-None-Match header from request
     * @param ifModifiedSince the If-Modified-Since header from request
     * @param <T> the type of response data
     * @return ResponseEntity with appropriate status and headers
     */
    public static <T> ResponseEntity<T> buildCacheableResponse(
            T data,
            Instant lastModified,
            String ifNoneMatch,
            String ifModifiedSince) {

        String etag = generateETag(data);

        // Check ETag first (stronger validation)
        if (etag != null && etagMatches(ifNoneMatch, etag)) {
            log.debug("ETag match, returning 304 Not Modified");
            HttpHeaders headers = new HttpHeaders();
            headers.setETag(etag);
            if (lastModified != null) {
                headers.setLastModified(lastModified);
            }
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .headers(headers)
                .build();
        }

        // Check Last-Modified as fallback
        if (ifNoneMatch == null && !isModifiedSince(ifModifiedSince, lastModified)) {
            log.debug("Resource not modified, returning 304");
            HttpHeaders headers = new HttpHeaders();
            if (etag != null) {
                headers.setETag(etag);
            }
            if (lastModified != null) {
                headers.setLastModified(lastModified);
            }
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .headers(headers)
                .build();
        }

        // Return fresh data with cache headers
        HttpHeaders headers = new HttpHeaders();
        if (etag != null) {
            headers.setETag(etag);
        }
        if (lastModified != null) {
            headers.setLastModified(lastModified);
        }
        headers.setCacheControl("max-age=300, must-revalidate");

        return ResponseEntity.ok()
            .headers(headers)
            .body(data);
    }

    private static Instant truncateToHttpSecondPrecision(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS);
    }
}
