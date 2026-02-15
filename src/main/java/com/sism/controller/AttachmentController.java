package com.sism.controller;

import com.sism.common.ApiResponse;
import com.sism.dto.AttachmentUploadRequest;
import com.sism.service.AttachmentService;
import com.sism.vo.AttachmentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Attachment Controller for SISM (Strategic Indicator Management System).
 * 
 * <p>This controller manages file attachments for supporting documentation in the system.
 * Attachments can be associated with indicators, reports, and other entities to provide
 * evidence and supporting materials.
 * 
 * <h2>Supported Operations</h2>
 * <ul>
 *   <li>File upload with metadata tracking</li>
 *   <li>File metadata retrieval</li>
 *   <li>Search by filename, content type, or uploader</li>
 *   <li>Soft deletion for audit trail</li>
 * </ul>
 * 
 * <h2>File Metadata</h2>
 * <p>Each attachment stores:
 * <ul>
 *   <li>Original filename and file extension</li>
 *   <li>Content type (MIME type)</li>
 *   <li>File size in bytes</li>
 *   <li>Storage location (driver, bucket, object key)</li>
 *   <li>SHA256 hash for integrity verification</li>
 *   <li>Upload timestamp and uploader information</li>
 * </ul>
 * 
 * <h2>Storage Drivers</h2>
 * <ul>
 *   <li><b>FILE</b>: Local filesystem storage</li>
 *   <li><b>S3</b>: Amazon S3 or compatible object storage</li>
 * </ul>
 * 
 * <h2>API Endpoints</h2>
 * <ul>
 *   <li>GET /api/attachments - List all attachments</li>
 *   <li>GET /api/attachments/{id} - Get attachment metadata</li>
 *   <li>GET /api/attachments/user/{userId} - Filter by uploader</li>
 *   <li>GET /api/attachments/content-type/{contentType} - Filter by type</li>
 *   <li>GET /api/attachments/search?keyword=xxx - Search by filename</li>
 *   <li>POST /api/attachments/upload - Upload new file</li>
 *   <li>DELETE /api/attachments/{id} - Soft delete attachment</li>
 * </ul>
 * 
 * @author SISM Development Team
 * @version 1.0
 * @since 1.0
 * @see com.sism.service.AttachmentService
 * @see com.sism.entity.Attachment
 */
@Slf4j
@RestController
@RequestMapping("/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "File attachment management endpoints")
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * Get all attachments
     * GET /api/attachments
     */
    @GetMapping
    @Operation(summary = "Get all attachments", description = "Retrieve all attachments")
    public ResponseEntity<ApiResponse<List<AttachmentVO>>> getAllAttachments() {
        List<AttachmentVO> attachments = attachmentService.getAllAttachments();
        return ResponseEntity.ok(ApiResponse.success(attachments));
    }

    /**
     * Get attachment by ID
     * GET /api/attachments/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get attachment by ID", description = "Retrieve a specific attachment")
    public ResponseEntity<ApiResponse<AttachmentVO>> getAttachmentById(
            @Parameter(description = "Attachment ID") @PathVariable Long id) {
        AttachmentVO attachment = attachmentService.getAttachmentById(id);
        return ResponseEntity.ok(ApiResponse.success(attachment));
    }

    /**
     * Get attachments by uploaded user ID
     * GET /api/attachments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get attachments by user", description = "Retrieve attachments uploaded by a specific user")
    public ResponseEntity<ApiResponse<List<AttachmentVO>>> getAttachmentsByUploadedBy(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<AttachmentVO> attachments = attachmentService.getAttachmentsByUploadedBy(userId);
        return ResponseEntity.ok(ApiResponse.success(attachments));
    }

    /**
     * Get attachments by content type
     * GET /api/attachments/content-type/{contentType}
     */
    @GetMapping("/content-type/{contentType}")
    @Operation(summary = "Get attachments by content type", description = "Retrieve attachments by content type")
    public ResponseEntity<ApiResponse<List<AttachmentVO>>> getAttachmentsByContentType(
            @Parameter(description = "Content type") @PathVariable String contentType) {
        List<AttachmentVO> attachments = attachmentService.getAttachmentsByContentType(contentType);
        return ResponseEntity.ok(ApiResponse.success(attachments));
    }

    /**
     * Search attachments by file name
     * GET /api/attachments/search?keyword=xxx
     */
    @GetMapping("/search")
    @Operation(summary = "Search attachments", description = "Search attachments by file name keyword")
    public ResponseEntity<ApiResponse<List<AttachmentVO>>> searchAttachmentsByFileName(
            @Parameter(description = "Search keyword") @RequestParam String keyword) {
        List<AttachmentVO> attachments = attachmentService.searchAttachmentsByFileName(keyword);
        return ResponseEntity.ok(ApiResponse.success(attachments));
    }

    /**
     * Upload a new attachment
     * POST /api/attachments/upload
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload attachment", description = "Upload a new file attachment")
    public ResponseEntity<ApiResponse<AttachmentVO>> uploadAttachment(
            @Valid @RequestBody AttachmentUploadRequest request) {
        log.info("Uploading attachment: {}", request.getOriginalName());
        AttachmentVO attachment = attachmentService.uploadAttachment(request);
        return ResponseEntity.ok(ApiResponse.success("Attachment uploaded successfully", attachment));
    }

    /**
     * Delete an attachment
     * DELETE /api/attachments/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete attachment", description = "Delete an attachment")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @Parameter(description = "Attachment ID") @PathVariable Long id) {
        log.info("Deleting attachment: {}", id);
        attachmentService.deleteAttachment(id);
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted successfully", null));
    }

    /**
     * Get file metadata
     * GET /api/attachments/{id}/metadata
     */
    @GetMapping("/{id}/metadata")
    @Operation(summary = "Get file metadata", description = "Retrieve file metadata without downloading")
    public ResponseEntity<ApiResponse<AttachmentVO>> getFileMetadata(
            @Parameter(description = "Attachment ID") @PathVariable Long id) {
        AttachmentVO metadata = attachmentService.getFileMetadata(id);
        return ResponseEntity.ok(ApiResponse.success(metadata));
    }
}
