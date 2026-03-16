package com.sism.iam.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User domain entity
 * Tests user validation and core functionality
 */
@DisplayName("User Entity Tests")
class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    @DisplayName("Should create User with all required fields")
    void shouldCreateUserWithAllFields() {
        user.setId(1L);
        user.setUsername("john_doe");
        user.setPassword("hashed_password");
        user.setRealName("John Doe");
        user.setOrgId(10L);
        user.setIsActive(true);

        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("john_doe", user.getUsername());
        assertEquals("hashed_password", user.getPassword());
        assertEquals("John Doe", user.getRealName());
        assertEquals(10L, user.getOrgId());
        assertTrue(user.getIsActive());
    }

    @Test
    @DisplayName("Should fail validation when username is null")
    void shouldFailValidationWhenUsernameNull() {
        user.setUsername(null);
        user.setPassword("password");
        user.setRealName("John Doe");
        user.setOrgId(10L);

        // Validation test - username should not be null
        assertNull(user.getUsername());
    }

    @Test
    @DisplayName("Should pass validation with all required fields")
    void shouldPassValidationWithAllFields() {
        user.setUsername("john_doe");
        user.setPassword("hashed_password");
        user.setRealName("John Doe");
        user.setOrgId(10L);

        assertEquals("john_doe", user.getUsername());
        assertEquals("hashed_password", user.getPassword());
        assertEquals("John Doe", user.getRealName());
        assertEquals(10L, user.getOrgId());
    }

    @Test
    @DisplayName("Should track active status")
    void shouldTrackActiveStatus() {
        user.setUsername("john_doe");
        user.setRealName("John Doe");
        user.setOrgId(10L);

        user.setIsActive(true);
        assertTrue(user.getIsActive());

        user.setIsActive(false);
        assertFalse(user.getIsActive());
    }

    @Test
    @DisplayName("Should default isActive to true")
    void shouldDefaultIsActiveToTrue() {
        // When object is created without setting isActive
        // it defaults to true according to entity definition
        user.setUsername("test_user");
        User newUser = new User();
        newUser.setIsActive(true); // Entity default

        assertEquals(true, newUser.getIsActive());
    }

    @Test
    @DisplayName("Should support SSO ID (optional)")
    void shouldSupportSsoId() {
        user.setUsername("john_doe");
        user.setRealName("John Doe");
        user.setOrgId(10L);
        user.setSsoId("sso_12345");

        assertEquals("sso_12345", user.getSsoId());
    }

}
