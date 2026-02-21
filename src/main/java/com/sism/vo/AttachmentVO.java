package com.sism.vo;

import java.time.OffsetDateTime;

/**
 * Value Object for attachment response
 *
 * Converted to record for immutability and simplicity
 * **Validates: Requirements 4.1**
 *
 * @param id              Attachment ID
 * @param storageDriver   Storage driver name
 * @param bucket          Bucket name
 * @param objectKey       Object key
 * @param publicUrl       Public URL
 * @param originalName    Original file name
 * @param contentType     Content type (MIME type)
 * @param fileExt         File extension
 * @param sizeBytes       File size in bytes
 * @param sha256          SHA256 hash
 * @param etag            ETag
 * @param uploadedBy      Uploader user ID
 * @param uploadedByName  Uploader user name
 * @param uploadedAt      Upload timestamp
 * @param remark          Remark
 * @param isDeleted       Whether deleted
 * @param deletedAt       Deletion timestamp
 */
public record AttachmentVO(
    Long id,
    String storageDriver,
    String bucket,
    String objectKey,
    String publicUrl,
    String originalName,
    String contentType,
    String fileExt,
    Long sizeBytes,
    String sha256,
    String etag,
    Long uploadedBy,
    String uploadedByName,
    OffsetDateTime uploadedAt,
    String remark,
    Boolean isDeleted,
    OffsetDateTime deletedAt
) {
    /**
     * Compact constructor with validation
     */
    public AttachmentVO {
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Original name cannot be null or blank");
        }
        // Default isDeleted to false if null
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}
