package com.sism.shared.domain.exception;

/**
 * 认证异常
 */
public class AuthenticationException extends RuntimeException {

    private final String code;

    public AuthenticationException(String message) {
        super(message);
        this.code = "AUTHENTICATION_FAILED";
    }

    public AuthenticationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
