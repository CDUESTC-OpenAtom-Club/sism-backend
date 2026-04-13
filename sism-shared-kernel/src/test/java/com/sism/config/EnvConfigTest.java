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
    void getBooleanShouldRecognizeCommonFalsyValues() throws IOException {
        Path tempDir = Files.createTempDirectory("envconfig-bool-false-test");
        try {
            Files.writeString(
                    tempDir.resolve(".env"),
                    "BOOL_FALSE=false\nBOOL_ZERO=0\nBOOL_NO=no\nBOOL_OFF=off\n",
                    StandardCharsets.UTF_8
            );
            Dotenv custom = Dotenv.configure()
                    .directory(tempDir.toString())
                    .ignoreIfMissing()
                    .load();
            EnvConfig.setDotenv(custom);

            assertFalse(EnvConfig.getBoolean("BOOL_FALSE", true));
            assertFalse(EnvConfig.getBoolean("BOOL_ZERO", true));
            assertFalse(EnvConfig.getBoolean("BOOL_NO", true));
            assertFalse(EnvConfig.getBoolean("BOOL_OFF", true));
        } finally {
            EnvConfig.setDotenv(null);
            Files.deleteIfExists(tempDir.resolve(".env"));
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void getBooleanShouldRecognizeCommonTruthyValues() throws IOException {
        Path tempDir = Files.createTempDirectory("envconfig-bool-true-test");
        try {
            Files.writeString(
                    tempDir.resolve(".env"),
                    "BOOL_TRUE=true\nBOOL_ONE=1\nBOOL_YES=yes\nBOOL_ON=on\n",
                    StandardCharsets.UTF_8
            );
            Dotenv custom = Dotenv.configure()
                    .directory(tempDir.toString())
                    .ignoreIfMissing()
                    .load();
            EnvConfig.setDotenv(custom);

            assertTrue(EnvConfig.getBoolean("BOOL_TRUE", false));
            assertTrue(EnvConfig.getBoolean("BOOL_ONE", false));
            assertTrue(EnvConfig.getBoolean("BOOL_YES", false));
            assertTrue(EnvConfig.getBoolean("BOOL_ON", false));
        } finally {
            EnvConfig.setDotenv(null);
            Files.deleteIfExists(tempDir.resolve(".env"));
            Files.deleteIfExists(tempDir);
        }
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
