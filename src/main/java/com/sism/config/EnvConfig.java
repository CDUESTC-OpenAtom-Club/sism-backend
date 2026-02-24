package com.sism.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;

public class EnvConfig {
    private static Dotenv dotenv;

    static {
        try {
            File projectDir = new File(System.getProperty("user.dir"));
            dotenv = Dotenv.configure()
                    .directory(projectDir.getAbsolutePath())
                    .ignoreIfMissing()
                    .load();
        } catch (Exception e) {
            System.err.println("Failed to load .env file: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return dotenv != null ? dotenv.get(key) : System.getenv(key);
    }

    public static String get(String key, String defaultValue) {
        return dotenv != null ? dotenv.get(key, defaultValue) : System.getenv(key) != null ? System.getenv(key) : defaultValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        return value != null && !value.equalsIgnoreCase("false");
    }
}
