package com.sism.shared.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SharedDomainExceptionTest {

    @Test
    void authenticationExceptionShouldBelongToSharedBusinessHierarchy() {
        AuthenticationException exception = new AuthenticationException("auth failed");

        assertInstanceOf(BusinessException.class, exception);
        assertEquals("AUTHENTICATION_FAILED", exception.getCode());
    }

    @Test
    void technicalExceptionShouldBelongToSharedBusinessHierarchy() {
        TechnicalException exception = new TechnicalException("broken");

        assertInstanceOf(BusinessException.class, exception);
        assertEquals("TECHNICAL_ERROR", exception.getCode());
    }

    @Test
    void resourceNotFoundShouldExposeSharedBusinessCode() {
        ResourceNotFoundException exception = new ResourceNotFoundException("Plan", 1L);

        assertInstanceOf(BusinessException.class, exception);
        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
    }
}
