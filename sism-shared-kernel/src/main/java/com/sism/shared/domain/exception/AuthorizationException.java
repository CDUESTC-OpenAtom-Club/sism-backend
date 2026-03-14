package com.sism.shared.domain.exception;

/**
 * 授权异常
 */
public class AuthorizationException extends RuntimeException {

    private final String code;

    public AuthorizationException(String message) {
        super(message);
        this.code = "AUTHORIZATION_FAILED";
    }

    public AuthorizationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
