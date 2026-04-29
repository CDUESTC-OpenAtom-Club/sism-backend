package com.sism.iam.application.service;

import com.sism.iam.domain.user.User;
import com.sism.iam.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService Tests")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("changePassword should increment token version")
    void changePasswordShouldIncrementTokenVersion() {
        User user = new User();
        user.setPassword("encoded-old");
        user.setTokenVersion(3L);

        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass-123")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userProfileService.changePassword(user, "old-pass", "new-pass-123", "new-pass-123");

        assertEquals("encoded-new", updated.getPassword());
        assertEquals(4L, updated.getTokenVersion());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword should initialize token version when missing")
    void changePasswordShouldInitializeTokenVersionWhenMissing() {
        User user = new User();
        user.setPassword("encoded-old");
        user.setTokenVersion(null);

        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass-123")).thenReturn("encoded-new");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User updated = userProfileService.changePassword(user, "old-pass", "new-pass-123", "new-pass-123");

        assertEquals(1L, updated.getTokenVersion());
    }

    @Test
    @DisplayName("changePassword should reject wrong current password")
    void changePasswordShouldRejectWrongCurrentPassword() {
        User user = new User();
        user.setPassword("encoded-old");

        when(passwordEncoder.matches("wrong-pass", "encoded-old")).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> userProfileService.changePassword(user, "wrong-pass", "new-pass-123", "new-pass-123")
        );
    }
}
