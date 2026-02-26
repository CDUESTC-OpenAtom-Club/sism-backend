package com.sism.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for uploading an attachment
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUploadRequest {

    private String storageDriver = "FILE";

    private String bucket;

    @NotBlank(message = "Object key is required")
    private String objectKey;

    private String publicUrl;

    @NotBlank(message = "Original file name is required")
    private String originalName;

    private String contentType;

    private String fileExt;

    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long sizeBytes;

    private String sha256;

    private String etag;

    @NotNull(message = "Uploaded by user ID is required")
    private Long uploadedBy;

    private String remark;
}
