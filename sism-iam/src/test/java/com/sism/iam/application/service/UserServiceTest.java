package com.sism.iam.application.service;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 * Tests user management business logic
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    @DisplayName("Should create user with required parameters")
    void shouldCreateUserWithRequiredParameters() {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("john_doe");
        mockUser.setRealName("John Doe");
        mockUser.setOrgId(10L);

        when(userRepository.save(any(User.class)))
                .thenReturn(mockUser);

        User created = userService.createUser("john_doe", "password", "John Doe", "john@example.com", 10L);

        assertNotNull(created);
        assertEquals("john_doe", created.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("john_doe");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        Optional<User> found = userService.findById(userId);

        assertTrue(found.isPresent());
        assertEquals("john_doe", found.get().getUsername());
    }

    @Test
    @DisplayName("Should find user by username")
    void shouldFindUserByUsername() {
        String username = "john_doe";
        User user = new User();
        user.setUsername(username);
        user.setRealName("John Doe");

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));

        Optional<User> found = userService.findByUsername(username);

        assertTrue(found.isPresent());
        assertEquals(username, found.get().getUsername());
    }

    @Test
    @DisplayName("Should lock user")
    void shouldLockUser() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setIsActive(true);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user))
                .thenReturn(user);

        userService.lockUser(userId);

        assertFalse(user.getIsActive());
    }

    @Test
    @DisplayName("Should unlock user")
    void shouldUnlockUser() {
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setIsActive(false);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(userRepository.save(user))
                .thenReturn(user);

        userService.unlockUser(userId);

        assertTrue(user.getIsActive());
    }

    @Test
    @DisplayName("Should throw exception when lock non-existent user")
    void shouldThrowExceptionWhenLockNonExistentUser() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            userService.lockUser(userId);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when unlock non-existent user")
    void shouldThrowExceptionWhenUnlockNonExistentUser() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            userService.unlockUser(userId);
        });

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should return empty when find by non-existent ID")
    void shouldReturnEmptyWhenFindByNonExistentId() {
        Long userId = 999L;

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        Optional<User> found = userService.findById(userId);

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should return empty when find by non-existent username")
    void shouldReturnEmptyWhenFindByNonExistentUsername() {
        String username = "nonexistent";

        when(userRepository.findByUsername(username))
                .thenReturn(Optional.empty());

        Optional<User> found = userService.findByUsername(username);

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should create user with all parameters including org")
    void shouldCreateUserWithAllParameters() {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("john_doe");
        mockUser.setRealName("John Doe");
        mockUser.setOrgId(10L);

        when(userRepository.save(any(User.class)))
                .thenReturn(mockUser);

        User created = userService.createUser(
                "john_doe",
                "password",
                "John Doe",
                "john@example.com",
                10L
        );

        assertNotNull(created);
        assertEquals("john_doe", created.getUsername());
        assertEquals(10L, created.getOrgId());
    }

    @Test
    @DisplayName("Should create user with minimal required parameters")
    void shouldCreateUserWithMinimalParameters() {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("jane_doe");
        mockUser.setRealName("Jane Doe");
        mockUser.setOrgId(20L);

        when(userRepository.save(any(User.class)))
                .thenReturn(mockUser);

        User created = userService.createUser(
                "jane_doe",
                "password",
                "Jane Doe",
                null,
                20L
        );

        assertNotNull(created);
        assertEquals("jane_doe", created.getUsername());
    }

}
