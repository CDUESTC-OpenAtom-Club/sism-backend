package com.sism.service;

import com.sism.dto.LoginRequest;
import com.sism.entity.SysUser;
import com.sism.exception.UnauthorizedException;
import com.sism.repository.UserRepository;
import com.sism.vo.LoginResponse;
import com.sism.vo.UserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AuthService
 * Tests authentication, token management, and user validation
 * 
 * Requirements: 4.2 - Service layer unit test coverage
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private SysUser testUser;
    private String testPassword = "testPassword123";

    @BeforeEach
    void setUp() {
        // Get or create a test user
        testUser = userRepository.findByUsername("testuser").orElseGet(() -> {
            SysUser user = new SysUser();
            user.setUsername("testuser");
            user.setPasswordHash(passwordEncoder.encode(testPassword));
            user.setRealName("Test User");
            user.setIsActive(true);
            // Get first org for test user
            user.setOrg(userRepository.findAll().stream()
                    .filter(u -> u.getOrg() != null)
                    .map(SysUser::getOrg)
                    .findFirst()
                    .orElseThrow());
            return userRepository.save(user);
        });
    }

    @Nested
    @DisplayName("login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLoginSuccessfullyWithValidCredentials() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername(testUser.getUsername());
            request.setPassword(testPassword);

            // When
            LoginResponse response = authService.login(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isNotNull().isNotEmpty();
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getUsername()).isEqualTo(testUser.getUsername());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException for invalid username")
        void shouldThrowExceptionForInvalidUsername() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("nonexistentuser");
            request.setPassword("anypassword");

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException for invalid password")
        void shouldThrowExceptionForInvalidPassword() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername(testUser.getUsername());
            request.setPassword("wrongpassword");

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException for inactive user")
        void shouldThrowExceptionForInactiveUser() {
            // Given - Create inactive user
            SysUser inactiveUser = new SysUser();
            inactiveUser.setUsername("inactiveuser" + System.currentTimeMillis());
            inactiveUser.setPasswordHash(passwordEncoder.encode("password"));
            inactiveUser.setRealName("Inactive User");
            inactiveUser.setIsActive(false);
            inactiveUser.setOrg(testUser.getOrg());
            userRepository.save(inactiveUser);

            LoginRequest request = new LoginRequest();
            request.setUsername(inactiveUser.getUsername());
            request.setPassword("password");

            // When/Then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("disabled");
        }
    }

    @Nested
    @DisplayName("logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout successfully and blacklist token")
        void shouldLogoutSuccessfully() {
            // Given - Login first to get a token
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.getUsername());
            loginRequest.setPassword(testPassword);
            LoginResponse loginResponse = authService.login(loginRequest);
            String token = loginResponse.getToken();

            // When
            authService.logout(token);

            // Then
            assertThat(authService.isTokenBlacklisted(token)).isTrue();
        }

        @Test
        @DisplayName("Should handle logout with Bearer prefix")
        void shouldHandleLogoutWithBearerPrefix() {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.getUsername());
            loginRequest.setPassword(testPassword);
            LoginResponse loginResponse = authService.login(loginRequest);
            String tokenWithBearer = "Bearer " + loginResponse.getToken();

            // When
            authService.logout(tokenWithBearer);

            // Then
            assertThat(authService.isTokenBlacklisted(loginResponse.getToken())).isTrue();
        }

        @Test
        @DisplayName("Should handle logout with null token")
        void shouldHandleLogoutWithNullToken() {
            // When/Then - Should not throw exception
            assertThatCode(() -> authService.logout(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle logout with empty token")
        void shouldHandleLogoutWithEmptyToken() {
            // When/Then - Should not throw exception
            assertThatCode(() -> authService.logout(""))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateTokenAndGetUser Tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should validate token and return user")
        void shouldValidateTokenAndReturnUser() {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.getUsername());
            loginRequest.setPassword(testPassword);
            LoginResponse loginResponse = authService.login(loginRequest);

            // When
            UserVO user = authService.validateTokenAndGetUser(loginResponse.getToken());

            // Then
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo(testUser.getUsername());
        }

        @Test
        @DisplayName("Should validate token with Bearer prefix")
        void shouldValidateTokenWithBearerPrefix() {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.getUsername());
            loginRequest.setPassword(testPassword);
            LoginResponse loginResponse = authService.login(loginRequest);
            String tokenWithBearer = "Bearer " + loginResponse.getToken();

            // When
            UserVO user = authService.validateTokenAndGetUser(tokenWithBearer);

            // Then
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo(testUser.getUsername());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException for null token")
        void shouldThrowExceptionForNullToken() {
            // When/Then
            assertThatThrownBy(() -> authService.validateTokenAndGetUser(null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Token is required");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException for empty token")
        void shouldThrowExceptionForEmptyToken() {
            // When/Then
            assertThatThrownBy(() -> authService.validateTokenAndGetUser(""))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Token is required");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException for blacklisted token")
        void shouldThrowExceptionForBlacklistedToken() {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.getUsername());
            loginRequest.setPassword(testPassword);
            LoginResponse loginResponse = authService.login(loginRequest);
            String token = loginResponse.getToken();
            
            // Blacklist the token
            authService.logout(token);

            // When/Then
            assertThatThrownBy(() -> authService.validateTokenAndGetUser(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("invalidated");
        }

        @Test
        @DisplayName("Should throw UnauthorizedException for invalid token")
        void shouldThrowExceptionForInvalidToken() {
            // Given
            String invalidToken = "invalid.token.here";

            // When/Then
            assertThatThrownBy(() -> authService.validateTokenAndGetUser(invalidToken))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("getCurrentUser Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should get current user from valid token")
        void shouldGetCurrentUserFromValidToken() {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.getUsername());
            loginRequest.setPassword(testPassword);
            LoginResponse loginResponse = authService.login(loginRequest);

            // When
            UserVO currentUser = authService.getCurrentUser(loginResponse.getToken());

            // Then
            assertThat(currentUser).isNotNull();
            assertThat(currentUser.getUsername()).isEqualTo(testUser.getUsername());
            assertThat(currentUser.getRealName()).isEqualTo(testUser.getRealName());
        }
    }

    @Nested
    @DisplayName("isTokenBlacklisted Tests")
    class IsTokenBlacklistedTests {

        @Test
        @DisplayName("Should return false for non-blacklisted token")
        void shouldReturnFalseForNonBlacklistedToken() {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.getUsername());
            loginRequest.setPassword(testPassword);
            LoginResponse loginResponse = authService.login(loginRequest);

            // When
            boolean isBlacklisted = authService.isTokenBlacklisted(loginResponse.getToken());

            // Then
            assertThat(isBlacklisted).isFalse();
        }

        @Test
        @DisplayName("Should return true for blacklisted token")
        void shouldReturnTrueForBlacklistedToken() {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(testUser.getUsername());
            loginRequest.setPassword(testPassword);
            LoginResponse loginResponse = authService.login(loginRequest);
            authService.logout(loginResponse.getToken());

            // When
            boolean isBlacklisted = authService.isTokenBlacklisted(loginResponse.getToken());

            // Then
            assertThat(isBlacklisted).isTrue();
        }
    }
}
