package com.sism.exception;

/**
 * Legacy resource-not-found exception kept for compatibility with older modules.
 *
 * <p>Delegates to the shared-domain resource-not-found hierarchy so the shared
 * kernel can resolve both legacy and new callers through one handler path.</p>
 */
@Deprecated
public class ResourceNotFoundException extends com.sism.shared.domain.exception.ResourceNotFoundException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource, id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
