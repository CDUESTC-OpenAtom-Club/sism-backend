package com.sism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 密码重置请求DTO
 *
 * @author SISM Development Team
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPasswordResetRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码长度至少8位")
    private String newPassword;
}
