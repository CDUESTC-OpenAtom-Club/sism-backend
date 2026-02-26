package com.sism.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Attachment entity
 * Stores file metadata for uploaded attachments
 * Maps to existing attachment table in database
 */
@Getter
@Setter
@Entity
@Table(name = "attachment")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "Storage driver cannot be blank")
    @Size(max = 16, message = "Storage driver must not exceed 16 characters")
    @Column(name = "storage_driver", nullable = false, columnDefinition = "varchar(16) default 'FILE'")
    private String storageDriver = "FILE";

    @Size(max = 128, message = "Bucket must not exceed 128 characters")
    @Column(name = "bucket")
    private String bucket;

    @NotBlank(message = "Object key cannot be blank")
    @Column(name = "object_key", nullable = false, columnDefinition = "text")
    private String objectKey;

    @Column(name = "public_url", columnDefinition = "text")
    private String publicUrl;

    @NotBlank(message = "Original name cannot be blank")
    @Column(name = "original_name", nullable = false, columnDefinition = "text")
    private String originalName;

    @Size(max = 128, message = "Content type must not exceed 128 characters")
    @Column(name = "content_type")
    private String contentType;

    @Size(max = 16, message = "File extension must not exceed 16 characters")
    @Column(name = "file_ext")
    private String fileExt;

    @NotNull(message = "File size cannot be null")
    @Positive(message = "File size must be positive")
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Size(max = 64, message = "SHA256 hash must be 64 characters")
    @Column(name = "sha256", columnDefinition = "char(64)")
    private String sha256;

    @Column(name = "etag", columnDefinition = "text")
    private String etag;

    @NotNull(message = "Uploader cannot be null")
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @NotNull(message = "Upload timestamp cannot be null")
    @PastOrPresent(message = "Upload timestamp cannot be in the future")
    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "remark", columnDefinition = "text")
    private String remark;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
