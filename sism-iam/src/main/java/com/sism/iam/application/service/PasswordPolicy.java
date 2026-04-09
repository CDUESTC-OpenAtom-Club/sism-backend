package com.sism.iam.application.service;

/**
 * Unified password policy for the IAM module.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 20;

    private PasswordPolicy() {
    }

    public static void validateLength(String password) {
        if (password == null) {
            throw new IllegalArgumentException(lengthMessage());
        }
        int length = password.length();
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(lengthMessage());
        }
    }

    public static String lengthMessage() {
        return "Password length must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters";
    }
}
