package com.sism.main.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String url;
    private Long uploadedBy;
    private OffsetDateTime uploadedAt;
}
