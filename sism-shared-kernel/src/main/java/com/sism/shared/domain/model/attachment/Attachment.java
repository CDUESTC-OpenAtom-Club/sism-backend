package com.sism.shared.domain.model.attachment;

import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.shared.domain.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * Attachment aggregate root - DDD model for file attachments
 */
@Getter
@Setter
@Entity
@Table(name = "attachments")
@Access(AccessType.FIELD)
public class Attachment extends AggregateRoot<Long> {

    public static final long MAX_FILE_SIZE = 100L * 1024 * 1024; // 100MB

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "storage_driver", nullable = false, columnDefinition = "varchar(16) default 'FILE'")
    private String storageDriver = "FILE";

    @Column(name = "bucket")
    private String bucket;

    @Column(name = "object_key", nullable = false, columnDefinition = "text")
    private String objectKey;

    @Column(name = "public_url", columnDefinition = "text")
    private String publicUrl;

    @Column(name = "original_name", nullable = false, columnDefinition = "text")
    private String originalName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_ext")
    private String fileExt;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "sha256", columnDefinition = "char(64)")
    private String sha256;

    @Column(name = "etag", columnDefinition = "text")
    private String etag;

    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "remark", columnDefinition = "text")
    private String remark;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean canPublish() {
        return storageDriver != null && "FILE".equals(storageDriver) 
            ? (objectKey != null && Files.exists(Paths.get(objectKey)))
            : (objectKey != null);
    }

    @Override
    public void validate() {
        if (sizeBytes != null && sizeBytes > MAX_FILE_SIZE) {
            throw new BusinessException("File size exceeds maximum limit of 100MB");
        }
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new BusinessException("Original filename cannot be empty");
        }
        if (uploadedBy == null) {
            throw new BusinessException("Uploader information is required");
        }
    }

    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = OffsetDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(isDeleted);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (uploadedAt == null) {
            uploadedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
