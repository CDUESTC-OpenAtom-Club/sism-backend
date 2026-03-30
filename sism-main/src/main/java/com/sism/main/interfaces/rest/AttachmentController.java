package com.sism.main.interfaces.rest;

import com.sism.common.ApiResponse;
import com.sism.main.application.AttachmentApplicationService;
import com.sism.main.interfaces.dto.AttachmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "附件上传与下载接口")
public class AttachmentController {

    private final AttachmentApplicationService attachmentApplicationService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传附件")
    public ResponseEntity<ApiResponse<AttachmentResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("uploadedBy") Long uploadedBy
    ) throws IOException {
        // Upload first persists the file into attachment storage, then returns metadata for later association.
        return ResponseEntity.ok(ApiResponse.success(attachmentApplicationService.upload(file, uploadedBy)));
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "获取附件元数据")
    public ResponseEntity<ApiResponse<AttachmentResponse>> metadata(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(attachmentApplicationService.getMetadata(id)));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "下载附件")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        AttachmentResponse metadata = attachmentApplicationService.getMetadata(id);
        Resource resource = attachmentApplicationService.loadAsResource(id);
        String encodedFileName = URLEncoder.encode(metadata.getFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.parseMediaType(
                        metadata.getFileType() == null || metadata.getFileType().isBlank()
                                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                : metadata.getFileType()
                ))
                .body(resource);
    }
}
