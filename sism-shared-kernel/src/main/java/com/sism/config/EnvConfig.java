package com.sism.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class EnvConfig {
    private static final Logger log = LoggerFactory.getLogger(EnvConfig.class);
    private static final Object LOCK = new Object();
    private static volatile Dotenv dotenv;

    private EnvConfig() {
    }

    public static void setDotenv(Dotenv override) {
        synchronized (LOCK) {
            dotenv = override;
        }
    }

    public static String get(String key) {
        String value = lookup(key);
        return value != null && !value.isBlank() ? value : null;
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "true", "1", "yes", "y", "on" -> true;
            case "false", "0", "no", "n", "off" -> false;
            default -> defaultValue;
        };
    }

    private static String lookup(String key) {
        Dotenv loader = getDotenv();
        return loader != null ? loader.get(key) : System.getenv(key);
    }

    private static Dotenv getDotenv() {
        Dotenv current = dotenv;
        if (current == null) {
            synchronized (LOCK) {
                if (dotenv == null) {
                    dotenv = loadDotenv();
                }
                current = dotenv;
            }
        }
        return current;
    }

    private static Dotenv loadDotenv() {
        try {
            File projectDir = new File(System.getProperty("user.dir"));
            return Dotenv.configure()
                    .directory(projectDir.getAbsolutePath())
                    .ignoreIfMissing()
                    .load();
        } catch (Exception e) {
            log.warn("Failed to load .env file, falling back to system environment: {}", e.getMessage());
            return null;
        }
    }
}
