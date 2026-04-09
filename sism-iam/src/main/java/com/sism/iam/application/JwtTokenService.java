package com.sism.iam.application;

import com.sism.iam.domain.User;
import com.sism.iam.domain.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtTokenService {

    @Value("${jwt.secret:SismSecretKeyForJWTTokenGeneration2024VeryLongSecretKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return generateToken(user, extractRoleCodes(user.getRoles()));
    }

    public String generateToken(User user, List<String> roleCodes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("orgId", user.getOrgId());
        claims.put("roles", normalizeRoleCodes(roleCodes));

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token) && !blacklistedTokens.contains(token);
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    public Long getOrgIdFromToken(String token) {
        return extractClaims(token).get("orgId", Long.class);
    }

    public List<String> getRolesFromToken(String token) {
        Object rawRoles = extractClaims(token).get("roles");
        if (rawRoles instanceof Collection<?> roles) {
            return roles.stream()
                    .map(String::valueOf)
                    .filter(role -> !role.isBlank())
                    .toList();
        }
        return List.of();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String username = extractUsername(refreshToken);
        Long userId = getUserIdFromToken(refreshToken);

        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setOrgId(getOrgIdFromToken(refreshToken));

        String newAccessToken = generateToken(user);
        String newRefreshToken = generateRefreshToken(user);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("refreshToken", newRefreshToken);
        result.put("expiresIn", expiration / 1000);
        result.put("tokenType", "Bearer");

        return result;
    }

    public String generateRefreshToken(User user) {
        return generateRefreshToken(user, extractRoleCodes(user.getRoles()));
    }

    public String generateRefreshToken(User user, List<String> roleCodes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("orgId", user.getOrgId());
        claims.put("roles", normalizeRoleCodes(roleCodes));
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public long getExpirationSeconds() {
        return expiration / 1000;
    }

    public void blacklistToken(String token) {
        if (token != null && !token.isBlank()) {
            blacklistedTokens.add(token);
        }
    }

    private List<String> extractRoleCodes(Collection<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        return normalizeRoleCodes(roles.stream()
                .map(Role::getRoleCode)
                .toList());
    }

    private List<String> normalizeRoleCodes(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }

        return roleCodes.stream()
                .filter(roleCode -> roleCode != null && !roleCode.isBlank())
                .distinct()
                .toList();
    }
}
