package com.sism.iam.application.dto;

import com.sism.iam.application.service.ContactInfoPolicy;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateContactRequest {

    @Pattern(regexp = ContactInfoPolicy.EMAIL_REGEX, message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = ContactInfoPolicy.PHONE_REGEX, message = "手机号格式不正确")
    private String phone;
}
