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
            throw new IllegalArgumentException(policyMessage());
        }
        int length = password.length();
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(policyMessage());
        }
        if (!password.chars().anyMatch(Character::isLetter)) {
            throw new IllegalArgumentException(policyMessage());
        }
        if (!password.chars().anyMatch(Character::isDigit)) {
            throw new IllegalArgumentException(policyMessage());
        }
    }

    public static String policyMessage() {
        return "Password must be at least 8 characters and contain at least one letter and one number";
    }
}
