package com.sism.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.util.CacheUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * Bridges the application ObjectMapper into CacheUtils' static helpers.
 *
 * This keeps the cache utility API static while ensuring JSON hashing uses
 * the same mapper configuration as the rest of the application.
 */
@Component
public class CacheUtilsObjectMapperConfig {

    private final ObjectMapper objectMapper;

    public CacheUtilsObjectMapperConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void wireObjectMapper() {
        CacheUtils.setObjectMapper(objectMapper);
    }
}
