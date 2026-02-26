package com.sism.vo;

import java.time.OffsetDateTime;

/**
 * Value Object for attachment response
 *
 * Converted to class for compatibility with Java compiler
 * **Validates: Requirements 4.1**
 */
public class AttachmentVO {
    private Long id;
    private String storageDriver;
    private String bucket;
    private String objectKey;
    private String publicUrl;
    private String originalName;
    private String contentType;
    private String fileExt;
    private Long sizeBytes;
    private String sha256;
    private String etag;
    private Long uploadedBy;
    private String uploadedByName;
    private OffsetDateTime uploadedAt;
    private String remark;
    private Boolean isDeleted;
    private OffsetDateTime deletedAt;

    /**
     * Default constructor
     */
    public AttachmentVO() {
    }

    /**
     * Full constructor with validation
     */
    public AttachmentVO(
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
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("Original name cannot be null or blank");
        }
        
        this.id = id;
        this.storageDriver = storageDriver;
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.publicUrl = publicUrl;
        this.originalName = originalName;
        this.contentType = contentType;
        this.fileExt = fileExt;
        this.sizeBytes = sizeBytes;
        this.sha256 = sha256;
        this.etag = etag;
        this.uploadedBy = uploadedBy;
        this.uploadedByName = uploadedByName;
        this.uploadedAt = uploadedAt;
        this.remark = remark;
        this.isDeleted = isDeleted != null ? isDeleted : false;
        this.deletedAt = deletedAt;
    }

    // Getter methods
    public Long getId() { return id; }
    public String getStorageDriver() { return storageDriver; }
    public String getBucket() { return bucket; }
    public String getObjectKey() { return objectKey; }
    public String getPublicUrl() { return publicUrl; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public String getFileExt() { return fileExt; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getEtag() { return etag; }
    public Long getUploadedBy() { return uploadedBy; }
    public String getUploadedByName() { return uploadedByName; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public String getRemark() { return remark; }
    public Boolean getIsDeleted() { return isDeleted; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }

    // Setter methods
    public void setId(Long id) { this.id = id; }
    public void setStorageDriver(String storageDriver) { this.storageDriver = storageDriver; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setFileExt(String fileExt) { this.fileExt = fileExt; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public void setEtag(String etag) { this.etag = etag; }
    public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }
    public void setUploadedByName(String uploadedByName) { this.uploadedByName = uploadedByName; }
    public void setUploadedAt(OffsetDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public void setRemark(String remark) { this.remark = remark; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}