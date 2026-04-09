package com.sism.exception;

import com.sism.common.ApiResponse;
import com.sism.shared.domain.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldConvertMethodArgumentValidationErrorsWithoutFieldCastFailure() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/items");
        request.addHeader("X-Request-ID", "req-123");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new SamplePayload(), "samplePayload");
        bindingResult.addError(new ObjectError("samplePayload", "payload is invalid"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        var response = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Object> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals(1001, body.getCode());
        assertEquals("参数验证失败", body.getMessage());
    }

    @Test
    void shouldMapSharedBusinessExceptionsToBadRequestResponse() {
        var response = handler.handleSharedBusinessException(
                new BusinessException("BUSINESS_ERROR", "shared failure"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals(1000, body.getCode());
        assertEquals("shared failure", body.getMessage());
    }

    private static final class SamplePayload {
    }

    private void sampleEndpoint(String value) {
        // test fixture only
    }
}
