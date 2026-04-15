package com.sism.iam.application.service;

import com.sism.iam.domain.User;
import com.sism.iam.domain.repository.UserRepository;
import com.sism.shared.domain.exception.AuthorizationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * UserProfileService - 用户个人中心应用服务
 * 将当前用户解析和资料变更集中在应用层，避免 Controller 直连仓储。
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User findCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }

    @Transactional
    public User updateProfile(User user, String realName) {
        requireUser(user);
        user.setRealName(realName);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User updateAvatar(User user, String avatarUrl) {
        requireUser(user);
        user.setAvatarUrl(avatarUrl);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public User changePassword(User user, String oldPassword, String newPassword, String confirmPassword) {
        requireUser(user);

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        PasswordPolicy.validateLength(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() == null ? 1L : user.getTokenVersion() + 1L);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private void requireUser(User user) {
        if (user == null) {
            throw new AuthorizationException("未登录");
        }
    }
}
