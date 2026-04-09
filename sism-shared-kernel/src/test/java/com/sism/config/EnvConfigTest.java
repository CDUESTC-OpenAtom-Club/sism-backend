package com.sism.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvConfigTest {

    @Test
    void getShouldReturnDefaultValueWhenMissing() {
        String key = "CODEx_MISSING_" + UUID.randomUUID();

        assertEquals("fallback", EnvConfig.get(key, "fallback"));
    }

    @Test
    void getBooleanShouldRespectDefaultValueWhenMissing() {
        String key = "CODEx_BOOL_MISSING_" + UUID.randomUUID();

        assertTrue(EnvConfig.getBoolean(key, true));
        assertFalse(EnvConfig.getBoolean(key, false));
    }

    @Test
    void setDotenvShouldOverrideLookup() throws IOException {
        Path tempDir = Files.createTempDirectory("envconfig-test");
        try {
            Files.writeString(tempDir.resolve(".env"), "ENV_CONFIG_OVERRIDE=success\n", StandardCharsets.UTF_8);
            Dotenv custom = Dotenv.configure()
                    .directory(tempDir.toString())
                    .ignoreIfMissing()
                    .load();

            EnvConfig.setDotenv(custom);

            assertEquals("success", EnvConfig.get("ENV_CONFIG_OVERRIDE"));
        } finally {
            EnvConfig.setDotenv(null);
            Files.deleteIfExists(tempDir.resolve(".env"));
            Files.deleteIfExists(tempDir);
        }
    }
}
