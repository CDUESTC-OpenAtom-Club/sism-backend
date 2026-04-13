package com.sism.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityHeadersFilterTest {

    private SecurityHeadersFilter newFilter() {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        ReflectionTestUtils.setField(filter, "headersEnabled", true);
        ReflectionTestUtils.setField(filter, "frameOptions", "SAMEORIGIN");
        ReflectionTestUtils.setField(filter, "contentTypeOptions", "nosniff");
        ReflectionTestUtils.setField(filter, "contentSecurityPolicy",
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'");
        ReflectionTestUtils.setField(filter, "strictTransportSecurity", "max-age=31536000; includeSubDomains");
        ReflectionTestUtils.setField(filter, "referrerPolicy", "strict-origin-when-cross-origin");
        ReflectionTestUtils.setField(filter, "permissionsPolicy", "geolocation=(), microphone=(), camera=()");
        return filter;
    }

    @Test
    void shouldApplyNoCacheHeadersForConfiguredApiPrefixes() throws Exception {
        SecurityHeadersFilter filter = newFilter();
        ReflectionTestUtils.setField(filter, "apiPathPrefixes", "/api/,/v1/api/");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/api/users");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("no-store, no-cache, must-revalidate, proxy-revalidate", response.getHeader("Cache-Control"));
        assertEquals("no-cache", response.getHeader("Pragma"));
        assertEquals("0", response.getHeader("Expires"));
    }

    @Test
    void shouldNotApplyNoCacheHeadersOutsideApiPrefixes() throws Exception {
        SecurityHeadersFilter filter = newFilter();
        ReflectionTestUtils.setField(filter, "apiPathPrefixes", "/api/");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(response.getHeader("Cache-Control"));
        assertNull(response.getHeader("Pragma"));
        assertNull(response.getHeader("Expires"));
    }

    @Test
    void shouldApplyCspAndHstsForSecureRequests() throws Exception {
        SecurityHeadersFilter filter = newFilter();
        ReflectionTestUtils.setField(filter, "apiPathPrefixes", "/api/");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/secure");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'",
                response.getHeader("Content-Security-Policy"));
        assertEquals("max-age=31536000; includeSubDomains", response.getHeader("Strict-Transport-Security"));
    }
}
