package com.sism.exception;

/**
 * Legacy business exception kept for compatibility with older modules.
 *
 * <p>It now delegates to the shared-domain exception hierarchy so the shared
 * kernel only has one active handler path. Callers should migrate to
 * {@link com.sism.shared.domain.exception.BusinessException} directly.</p>
 */
@Deprecated
public class BusinessException extends com.sism.shared.domain.exception.BusinessException {

    private final int legacyCode;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(String.valueOf(code), message);
        this.legacyCode = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(String.valueOf(code), message, cause);
        this.legacyCode = code;
    }

    public int getLegacyCode() {
        return legacyCode;
    }
}
