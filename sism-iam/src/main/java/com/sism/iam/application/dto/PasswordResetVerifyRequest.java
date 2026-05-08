package com.sism.iam.application.dto;

import com.sism.iam.application.service.ContactInfoPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordResetVerifyRequest {

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = ContactInfoPolicy.EMAIL_REGEX, message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
    private String code;
}
