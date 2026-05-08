package com.sism.iam.application.service;

import java.util.Locale;

/**
 * Centralized contact info validation and normalization rules.
 */
public final class ContactInfoPolicy {

    public static final String EMAIL_REGEX =
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    private ContactInfoPolicy() {
    }

    public static boolean looksLikeEmail(String value) {
        return value != null && value.contains("@");
    }

    public static boolean looksLikePhone(String value) {
        String normalized = trimToNull(value);
        return normalized != null && normalized.matches(PHONE_REGEX);
    }

    public static String normalizeEmail(String email) {
        String normalized = trimToNull(email);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!normalized.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        return normalized;
    }

    public static String normalizePhone(String phone) {
        String normalized = trimToNull(phone);
        if (normalized == null) {
            return null;
        }
        if (!normalized.matches(PHONE_REGEX)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        return normalized;
    }

    public static String normalizeAccount(String account) {
        String normalized = trimToNull(account);
        if (normalized == null) {
            return null;
        }
        if (looksLikeEmail(normalized)) {
            return normalizeEmail(normalized);
        }
        if (looksLikePhone(normalized)) {
            return normalizePhone(normalized);
        }
        return normalized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
