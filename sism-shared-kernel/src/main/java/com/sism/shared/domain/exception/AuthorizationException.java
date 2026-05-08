package com.sism.shared.domain.exception;

/**
 * 授权异常
 */
public class AuthorizationException extends BusinessException {

    public AuthorizationException(String message) {
        super("AUTHORIZATION_FAILED", message);
    }

    public AuthorizationException(String code, String message) {
        super(code, message);
    }
}
