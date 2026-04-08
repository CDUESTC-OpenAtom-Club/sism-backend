---
name: sism-iam deep audit report
description: Third-round deep audit report for sism-iam module
type: audit
---

# SISM-IAM Module -- Third-Round Deep Audit Report

## Audit Scope

- **Module:** `sism-iam` at `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/`
- **Related:** Security config in `sism-main`, shared-kernel components referenced by IAM
- **Date:** 2026-04-06
- **Method:** Full read of every source file, security config, and dependency chain

---

## CRITICAL Findings

### C-1. Duplicate TokenBlacklistService: IAM Uses Only In-Memory Version, Dead Redis Version Exists

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/service/TokenBlacklistService.java` (lines 10-44)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-shared-kernel/src/main/java/com/sism/util/TokenBlacklistService.java` (lines 21-107)

**Evidence:**
The IAM module's `JwtTokenService` (line 3) imports `com.sism.iam.application.service.TokenBlacklistService` -- the purely in-memory ConcurrentHashMap version. The shared-kernel has a completely different `com.sism.util.TokenBlacklistService` with Redis support and TTL-based cleanup. The main application's `@ComponentScan` does NOT include `com.sism.util`, so the shared-kernel version is dead code.

```java
// IAM version -- what is actually used:
@Service
public class TokenBlacklistService {
    private final Map<String, Instant> blacklist = new ConcurrentHashMap<>(); // In-memory only!
    // ...
}
```

```java
// Shared-kernel version -- DEAD CODE, never instantiated as a Spring bean:
@Slf4j
@Service
public class TokenBlacklistService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean redisEnabled;
    // Has Redis support, TTL-based purge, etc.
}
```

**Impact:** In a multi-instance deployment (load balancer + multiple app servers), logging out blacklists the token only on the server that handled the request. Other servers will continue to accept the token. The Redis-backed blacklist that would solve this problem exists as dead code but is never wired in.

**Severity:** Critical
**Category:** Security / Architecture

---

### C-2. Token Refresh Does Not Validate User Still Exists or Is Active

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/JwtTokenService.java` (lines 124-147)

**Evidence:**
```java
public Map<String, Object> refreshToken(String refreshToken) {
    if (!validateToken(refreshToken) || !isRefreshToken(refreshToken)) {
        throw new IllegalArgumentException("Invalid refresh token");
    }
    String username = extractUsername(refreshToken);
    Long userId = getUserIdFromToken(refreshToken);
    User user = new User();            // <-- Creates a phantom User from token claims
    user.setId(userId);                 //     No database lookup!
    user.setUsername(username);
    user.setOrgId(getOrgIdFromToken(refreshToken));
    String newAccessToken = generateToken(user);   // New tokens from stale claims
    String newRefreshToken = generateRefreshToken(user);
    // ...
}
```

**Impact:** Once a refresh token is issued, it remains valid for its full lifetime (7 days by default). Even if the admin disables the user account (`isActive = false`), the user can keep refreshing tokens forever because the refresh flow never checks the database. The same applies to deleted users -- their refresh tokens continue to work.

**Severity:** Critical
**Category:** Security / Bug

---

### C-3. CurrentUser.isEnabled() Always Returns True, Bypassing Disabled-User Checks

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/dto/CurrentUser.java` (lines 77-80)

**Evidence:**
```java
@Override
public boolean isEnabled() {
    return true;  // Hardcoded! Ignores actual user.isActive from database
}
```

**Impact:** Spring Security's `UserDetails.isEnabled()` is intended to reject disabled users at the security filter level. By hardcoding `true`, any JWT token for a disabled user continues to pass Spring Security checks. Combined with C-2 (refresh tokens not checking DB), a disabled user retains full API access until their token naturally expires.

**Severity:** Critical
**Category:** Security / Bug

---

## HIGH Findings

### H-1. NotificationController Alert Event Endpoints Have Zero Authorization Checks

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/interfaces/rest/NotificationController.java` (lines 93-221)

**Evidence:** All alert event CRUD endpoints lack `@PreAuthorize` and `@AuthenticationPrincipal`:
- `POST /api/v1/notifications` -- create alert event (any authenticated user)
- `PATCH /api/v1/notifications/{id}/status` -- change status (any authenticated user)
- `POST /api/v1/notifications/{id}/handle` -- mark handled (any authenticated user, `handledByUserId` is a request param, not from the token)
- `DELETE /api/v1/notifications/{id}` -- delete alert event (any authenticated user)
- `GET /api/v1/notifications` -- list ALL alert events (any authenticated user)
- `GET /api/v1/notifications/indicator/{indicatorId}` (any authenticated user)

**Impact:** Any logged-in user (even a secondary college user with the weakest role) can create, modify, or delete alert events. They can also set `handledByUserId` to any value, falsifying who "handled" an alert.

**Severity:** High
**Category:** Security

---

### H-2. RoleManagementController GET Endpoints Expose All Role Data Without Authorization

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/interfaces/rest/RoleManagementController.java` (lines 46-76, 195-210)

**Evidence:**
```java
@GetMapping                                              // No @PreAuthorize
public ResponseEntity<ApiResponse<PageResult<RoleResponse>>> listRoles(...)

@GetMapping("/{id}")                                     // No @PreAuthorize
public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id)

@GetMapping("/{id}/permissions")                         // No @PreAuthorize
public ResponseEntity<ApiResponse<List<PermissionResponse>>> getRolePermissions(...)
```

**Impact:** Any authenticated user can enumerate all roles, their permissions, and user counts. This is an information disclosure issue -- role/permission structures should be restricted to administrators.

**Severity:** High
**Category:** Security

---

### H-3. AuthService.register() Will Crash -- Missing orgId

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/service/AuthService.java` (lines 68-88)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/domain/User.java` (line 39-40)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/interfaces/dto/RegisterRequest.java` (entire file)

**Evidence:**
```java
// AuthService.register -- orgId never set:
public User register(String username, String password, String realName) {
    User user = new User();
    user.setUsername(username);
    user.setPassword(passwordEncoder.encode(password));
    user.setRealName(realName);
    user.setIsActive(true);
    // orgId is NULL -- but DB column is NOT NULL!
    return userRepository.save(user);  // Will throw constraint violation
}

// User entity:
@Column(name = "org_id", nullable = false)
private Long orgId;

// RegisterRequest DTO -- no orgId field at all:
@Data
public class RegisterRequest {
    private String username;  // No @NotBlank
    private String password;  // No @NotBlank, @Size, @Pattern
    private String realName;  // No @NotBlank
}
```

**Impact:** The admin-only registration endpoint is non-functional -- every attempt will result in a database constraint violation. The RegisterRequest DTO also lacks any validation annotations, so empty/invalid input would pass through to the service layer.

**Severity:** High
**Category:** Bug

---

### H-4. Password Validation Inconsistency Across Two Methods

**Files:**
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/service/AuthService.java` (lines 68-88, 107-122)
- `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/domain/User.java` (lines 118-130)

**Evidence:** Two methods validate passwords with different rules:
1. `AuthService.register()` has NO validation
2. `User.validate()` has length validation only
```java
// User.java:118-130:
public void validate() {
    if (this.username == null || this.username.trim().isEmpty()) {
        throw new IllegalArgumentException("用户名不能为空");
    }
    if (this.password == null || this.password.length() < 6 || this.password.length() > 20) {
        throw new IllegalArgumentException("密码长度必须在6-20个字符之间");
    }
}
```

**Impact:** The registration endpoint accepts passwords shorter than 6 characters, while other methods reject them. This inconsistency could lead to users with invalid passwords in the database.

**Severity:** High
**Category:** Bug

---

## MEDIUM Findings

### M-1. NotificationService has Fake Stub Methods That Do Nothing

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/service/NotificationService.java` (lines 54-101)

**Evidence:** Methods like `sendSystemNotification()`, `sendEmailNotification()`, `sendSmsNotification()` are all stubs that log a debug message and return. No real notification functionality exists.

```java
@Override
public void sendSystemNotification(Long userId, String title, String content) {
    log.debug("Sending system notification to user {}: {} - {}", userId, title, content);
}
```

**Impact:** The API endpoints accept notification requests but never actually send anything. This is misleading to API consumers and breaks any functionality that relies on notifications (e.g., task assignment alerts, approval reminders).

**Severity:** Medium
**Category:** Bug / Documentation

---

### M-2. UserDetailsServiceImpl.loadUserByUsername() Swallows Exceptions

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/UserDetailsServiceImpl.java` (lines 31-45)

**Evidence:**
```java
try {
    User user = userRepository.findByUsername(username);
    if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
        return null;  // <-- Returns null instead of throwing UsernameNotFoundException
    }
    // ...
} catch (Exception e) {
    log.error("Error loading user by username: {}", username, e);
    return null;   // <-- Returns null instead of rethrowing
}
```

**Impact:** Spring Security expects `UsernameNotFoundException` when a user doesn't exist. Returning `null` from `loadUserByUsername()` causes unexpected behavior in the authentication flow.

**Severity:** Medium
**Category:** Security / Bug

---

### M-3. NotificationController Uses Primitive Types for Query Parameters

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/interfaces/rest/NotificationController.java` (lines 132-158)

**Evidence:** Methods like `getNotificationsByStatus` and `getNotificationsBySeverity` use primitive type parameters. If a `null` is passed, Spring will throw an exception.

```java
public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotificationsByStatus(
        @RequestParam String status)  // primitive String, can't be null
```

**Impact:** API endpoints are brittle -- null values for optional parameters cause 500 errors instead of handling them gracefully.

**Severity:** Medium
**Category:** API Design

---

### M-4. PasswordEncoder is Not a Bean -- Must Be a Static Field

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/service/AuthService.java` (line 24)

**Evidence:**
```java
private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
```

This is declared as a static field instead of a Spring bean. While it works, it's inconsistent with how other beans are wired in the application.

**Severity:** Medium
**Category:** Architecture

---

## LOW Findings

### L-1. NotificationService is a RepositoryWrapper, Not a Service

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/application/service/NotificationService.java` (lines 54-101)

**Evidence:** The class has no business logic -- it just passes through to the repository.

```java
@Override
public List<Notification> getNotificationsByUserId(Long userId) {
    return notificationRepository.findByUserId(userId);
}
```

**Impact:** This class adds unnecessary indirection. Methods should be called directly from the controller or the repository should be injected directly.

**Severity:** Low
**Category:** Code Quality

---

### L-2. NotificationResponse is a Direct Entity Copy

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/interfaces/response/NotificationResponse.java` (entire file)

**Evidence:** The DTO has the exact same fields as the Notification entity.

**Impact:** This violates the principle of DTO design. If the entity changes, the response changes automatically. DTOs should be versioned and decoupled from entity internals.

**Severity:** Low
**Category:** Code Quality

---

### L-3. Boolean isActive Has Three Possible States (true/false/null)

**File:** `/Users/blackevil/Documents/前端架构测试/sism-backend/sism-iam/src/main/java/com/sism/iam/domain/User.java` (line 48)

**Evidence:** The field is declared as `Boolean` (object type), allowing null.

```java
@Column(name = "is_active")
private Boolean isActive = true;
```

**Impact:** Null values can cause unexpected behavior in boolean logic. It should be `boolean` (primitive type) to enforce true/false only.

**Severity:** Low
**Category:** Code Quality

---

## Summary of Findings

| Severity | Count |
|----------|-------|
| Critical | 3 |
| High | 4 |
| Medium | 4 |
| Low | 3 |
| **Total** | **14** |

**Most Critical Issues:**
1. C-1: Duplicate TokenBlacklistService with in-memory only usage
2. C-2: Token refresh doesn't validate user exists/active
3. C-3: CurrentUser.isEnabled() hardcoded true
4. H-1: NotificationController has no auth checks