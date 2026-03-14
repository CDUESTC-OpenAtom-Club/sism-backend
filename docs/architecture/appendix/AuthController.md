AuthController
- Base path: /api/v1/auth

- Endpoints
 1) POST /login
   - Summary: User login
   - Description: Authenticate user and return JWT access token; set refresh token cookie if enabled
   - Input: LoginRequest in body (validated @Valid)
   - Output: ApiResponse<LoginResponse>
   - Service: authService.login(request); refreshTokenService.generateRefreshToken(...)

 2) POST /logout
   - Summary: User logout
   - Description: Invalidate access token and revoke refresh token; clear cookie
   - Input: Authorization header (optional)
   - Output: ApiResponse<Void>
   - Service: authService.logout(authorization); refreshTokenService.revokeToken(from cookie)

 3) POST /refresh
   - Summary: Refresh access token
   - Description: Rotate tokens; issue new access token and refresh token; set cookie
   - Input: No body; reads refresh token from cookie
   - Output: ApiResponse<LoginResponse>
   - Service: refreshTokenService.refreshTokens(...);

 4) GET /me
   - Summary: Get current user
   - Description: Return information about the currently authenticated user
   - Input: Authorization header
   - Output: ApiResponse<UserVO>
   - Service: authService.getCurrentUser(authorization)

- DTOs
  - LoginRequest

- Outputs (VOs) and ApiResponse
  - LoginResponse; UserVO; ApiResponse wrapper
- Observations
  - JWT-based authentication with access and refresh tokens
  - Refresh token stored in HttpOnly cookie with SameSite attribute

- Recommendations
  - Consider documenting token rotation behavior and security implications for clients
