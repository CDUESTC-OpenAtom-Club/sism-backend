package com.sism.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * Value Object for attachment response
 */
@Data
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
}
