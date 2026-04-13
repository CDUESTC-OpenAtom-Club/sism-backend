package com.sism.shared.domain.exception;

/**
 * 认证异常
 */
public class AuthenticationException extends BusinessException {

    public AuthenticationException(String message) {
        super("AUTHENTICATION_FAILED", message);
    }

    public AuthenticationException(String code, String message) {
        super(code, message);
    }
}
