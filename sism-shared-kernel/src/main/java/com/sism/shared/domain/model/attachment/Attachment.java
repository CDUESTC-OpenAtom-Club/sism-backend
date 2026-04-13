package com.sism.shared.domain.model.attachment;

import com.sism.shared.domain.model.base.AggregateRoot;
import com.sism.shared.domain.exception.BusinessException;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * Attachment aggregate root - DDD model for file attachments
 */
@Getter
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

    @Override
    public boolean canPublish() {
        return objectKey != null && !objectKey.isBlank();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        assertIdUnchanged(this.id, id);
        this.id = id;
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
        setUpdatedAt(java.time.LocalDateTime.now());
    }

    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
        setUpdatedAt(java.time.LocalDateTime.now());
    }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(isDeleted);
    }

    public void configureStorage(String storageDriver, String bucket, String objectKey, String etag, String publicUrl) {
        this.storageDriver = storageDriver;
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.etag = etag;
        this.publicUrl = publicUrl;
        setUpdatedAt(java.time.LocalDateTime.now());
    }

    public void describeFile(String originalName, String contentType, String fileExt, Long sizeBytes, String sha256) {
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileExt = fileExt;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
    }

    public void assignUploader(Long uploadedBy, OffsetDateTime uploadedAt) {
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
    }

    public void updateRemark(String remark) {
        this.remark = remark;
        setUpdatedAt(java.time.LocalDateTime.now());
    }

    @PrePersist
    protected void onCreate() {
        setCreatedAt(java.time.LocalDateTime.now());
        setUpdatedAt(java.time.LocalDateTime.now());
        if (uploadedAt == null) {
            uploadedAt = OffsetDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        setUpdatedAt(java.time.LocalDateTime.now());
    }
}
