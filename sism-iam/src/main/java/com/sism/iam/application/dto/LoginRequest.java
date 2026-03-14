package com.sism.iam.application.dto;

import lombok.Data;

/**
 * LoginRequest - 登录请求DTO
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
