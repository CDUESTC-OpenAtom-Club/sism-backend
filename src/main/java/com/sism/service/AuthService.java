package com.sism.service;

import com.sism.dto.LoginRequest;
import com.sism.entity.AppUser;
import com.sism.exception.UnauthorizedException;
import com.sism.util.JwtUtil;
import com.sism.util.TokenBlacklistService;
import com.sism.vo.LoginResponse;
import com.sism.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service for authentication operations
 * Handles login, logout, and token management
 * 
 * Requirements: 8.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    /**
     * Authenticate user and generate JWT token
     * Requirements: 8.1 - Login verification (username/password validation)
     * 
     * @param request login request containing username and password
     * @return login response with JWT token and user info
     * @throws UnauthorizedException if authentication fails
     */
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        // Find user by username
        Optional<AppUser> userOpt = userService.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            log.warn("Login failed: user not found - {}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }
        
        AppUser user = userOpt.get();
        
        // Check if user is active
        if (!user.getIsActive()) {
            log.warn("Login failed: user is inactive - {}", request.getUsername());
            throw new UnauthorizedException("User account is disabled");
        }

        
        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for user - {}", request.getUsername());
            throw new UnauthorizedException("Invalid username or password");
        }
        
        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getOrg().getOrgId()
        );
        
        // Build user VO
        UserVO userVO = buildUserVO(user);
        
        log.info("Login successful for user: {}", request.getUsername());
        return LoginResponse.of(token, jwtExpiration / 1000, userVO);
    }

    /**
     * Logout user by invalidating token
     * Requirements: 8.1 - Logout handling (optional: token blacklist)
     * 
     * @param token JWT token to invalidate
     */
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            // Remove "Bearer " prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            tokenBlacklistService.blacklist(token);
            log.info("Token added to blacklist");
        }
    }

    /**
     * Check if token is blacklisted
     * 
     * @param token JWT token to check
     * @return true if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistService.isBlacklisted(token);
    }

    /**
     * Validate token and return user info
     * 
     * @param token JWT token
     * @return user VO if token is valid
     * @throws UnauthorizedException if token is invalid
     */
    public UserVO validateTokenAndGetUser(String token) {
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedException("Token is required");
        }
        
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // Check blacklist
        if (isTokenBlacklisted(token)) {
            throw new UnauthorizedException("Token has been invalidated");
        }
        
        // Validate token
        if (!jwtUtil.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired token");
        }
        
        // Get user from token
        String username = jwtUtil.extractUsername(token);
        return userService.getUserVOByUsername(username);
    }

    /**
     * Get current user info from token
     * 
     * @param token JWT token
     * @return user VO
     */
    public UserVO getCurrentUser(String token) {
        return validateTokenAndGetUser(token);
    }

    /**
     * Get user entity by username
     * Used by AuthController for refresh token generation
     * 
     * @param username the username
     * @return user entity
     * @throws UnauthorizedException if user not found
     */
    public AppUser getUserByUsername(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    /**
     * Build UserVO from AppUser entity
     */
    private UserVO buildUserVO(AppUser user) {
        UserVO vo = new UserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setIsActive(user.getIsActive());
        
        if (user.getOrg() != null) {
            vo.setOrgId(user.getOrg().getOrgId());
            vo.setOrgName(user.getOrg().getOrgName());
            vo.setOrgType(user.getOrg().getOrgType());
        }
        
        return vo;
    }
}
