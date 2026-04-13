package com.sism.config;

import com.sism.iam.application.JwtTokenService;
import com.sism.iam.application.UserDetailsServiceImpl;
import com.sism.iam.application.dto.CurrentUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Config Tests")
class SecurityConfigTest {

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private FilterChain filterChain;

    @Test
    @DisplayName("Should load current authorities from database when token is valid")
    void shouldLoadCurrentAuthoritiesFromDatabaseWhenTokenIsValid() throws Exception {
        SecurityContextHolder.clearContext();
        SecurityConfig config = new SecurityConfig();
        OncePerRequestFilter filter = config.jwtAuthenticationFilter(jwtTokenService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");

        CurrentUser currentUser = new CurrentUser(
                1L,
                "admin",
                "Admin",
                null,
                35L,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        when(jwtTokenService.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenService.extractUsername("valid-token")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(currentUser);

        filter.doFilter(request, response, filterChain);

        verify(userDetailsService).loadUserByUsername("admin");
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("admin", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should continue filter chain when jwt validation throws")
    void shouldContinueFilterChainWhenJwtValidationThrows() throws Exception {
        SecurityContextHolder.clearContext();
        SecurityConfig config = new SecurityConfig();
        OncePerRequestFilter filter = config.jwtAuthenticationFilter(jwtTokenService, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer bad-token");

        when(jwtTokenService.validateToken("bad-token")).thenThrow(new IllegalArgumentException("invalid token"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        SecurityContextHolder.clearContext();
    }
}
