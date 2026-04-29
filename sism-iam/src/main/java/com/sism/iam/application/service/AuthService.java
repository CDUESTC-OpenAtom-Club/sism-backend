package com.sism.iam.application.service;

import com.sism.iam.application.JwtTokenService;
import com.sism.iam.application.dto.LoginRequest;
import com.sism.iam.application.dto.LoginResponse;
import com.sism.iam.domain.user.User;
import com.sism.iam.domain.user.UserRepository;
import com.sism.organization.domain.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * AuthService - 认证服务
 * 处理用户登录、注册等认证逻辑
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final OrganizationRepository organizationRepository;

    /**
     * 用户登录
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("请输入用户名");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("请输入密码");
        }

        String clientKey = "global";
        loginAttemptService.assertNotBlocked(request.getUsername(), clientKey);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(request.getUsername(), clientKey);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        if (!user.getIsActive()) {
            throw new IllegalStateException("账号已被禁用，请联系管理员");
        }

        loginAttemptService.recordSuccess(request.getUsername(), clientKey);

        List<String> roleCodes = userRepository.findRoleCodesByUserId(user.getId());

        String accessToken = jwtTokenService.generateToken(user, roleCodes);
        String refreshToken = jwtTokenService.generateRefreshToken(user, roleCodes);
        var organization = organizationRepository.findById(user.getOrgId()).orElse(null);

        return LoginResponse.fromUser(
                user,
                roleCodes,
                organization != null ? organization.getName() : null,
                organization != null ? organization.getType().name() : null,
                accessToken,
                refreshToken,
                jwtTokenService.getExpirationSeconds()
        );
    }

    /**
     * 用户注册
     */
    @Transactional
    public User register(String username, String password, String realName) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("请输入用户名");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("请输入密码");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setIsActive(true);

        return userRepository.save(user);
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        return jwtTokenService.validateToken(token);
    }

    public void logout(String token) {
        jwtTokenService.blacklistToken(token);
    }

    /**
     * 获取当前用户ID
     */
    public Long getUserIdFromToken(String token) {
        return jwtTokenService.getUserIdFromToken(token);
    }

    /**
     * 刷新访问令牌
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        return jwtTokenService.refreshToken(refreshToken);
    }

    /**
     * 获取当前用户权限编码列表
     */
    @Transactional(readOnly = true)
    public List<String> getPermissionCodes(Long userId) {
        if (userId == null || userId <= 0) {
            return List.of();
        }
        return userRepository.findPermissionCodesByUserId(userId);
    }
}
