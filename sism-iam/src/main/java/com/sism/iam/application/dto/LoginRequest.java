package com.sism.iam.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * LoginRequest - 登录请求DTO
 */
@Data
public class LoginRequest {
    @JsonAlias("username")
    @NotBlank(message = "账号不能为空")
    @Size(max = 100, message = "账号长度不能超过100个字符")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度必须在8到128个字符之间")
    private String password;
}
