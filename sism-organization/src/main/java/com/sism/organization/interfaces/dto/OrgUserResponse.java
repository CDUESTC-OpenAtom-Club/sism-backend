package com.sism.organization.interfaces.dto;

import com.sism.iam.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Minimal user response for organization membership lookups.
 */
@Schema(description = "Organization user response DTO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrgUserResponse {

    @Schema(description = "User ID", example = "1001")
    private Long id;

    @Schema(description = "Username", example = "alice")
    private String username;

    @Schema(description = "Real name", example = "Alice Zhang")
    private String realName;

    @Schema(description = "Whether the account is active", example = "true")
    private Boolean isActive;

    public static OrgUserResponse fromUser(User user) {
        if (user == null) {
            return null;
        }
        return OrgUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .isActive(user.getIsActive())
                .build();
    }
}
