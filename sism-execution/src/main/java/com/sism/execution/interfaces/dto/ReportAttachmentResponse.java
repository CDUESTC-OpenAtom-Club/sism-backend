package com.sism.execution.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportAttachmentResponse {
    private Long id;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String url;
    private Long uploadedBy;
    private String uploadedAt;
}
