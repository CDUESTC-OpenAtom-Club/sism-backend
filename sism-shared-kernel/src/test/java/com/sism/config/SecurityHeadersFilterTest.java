package com.sism.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityHeadersFilterTest {

    @Test
    void shouldApplyNoCacheHeadersForConfiguredApiPrefixes() throws Exception {
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        ReflectionTestUtils.setField(filter, "headersEnabled", true);
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
        SecurityHeadersFilter filter = new SecurityHeadersFilter();
        ReflectionTestUtils.setField(filter, "headersEnabled", true);
        ReflectionTestUtils.setField(filter, "apiPathPrefixes", "/api/");

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(response.getHeader("Cache-Control"));
        assertNull(response.getHeader("Pragma"));
        assertNull(response.getHeader("Expires"));
    }
}
