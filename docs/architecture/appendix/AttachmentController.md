AttachmentController
- Base path: /api/v1/attachments

- Endpoints
 1) GET / (list all)
   - Summary: Get all attachments
   - Description: Retrieve all attachments
   - Output: ApiResponse<List<AttachmentVO>>
   - Service: attachmentService.getAllAttachments()

 2) GET /{id}
   - Summary: Get attachment by ID
   - Description: Retrieve a specific attachment
   - Input: PathVariable Long id
   - Output: ApiResponse<AttachmentVO>
   - Service: attachmentService.getAttachmentById(id)

 3) GET /user
   - Summary: Get attachments by current user
   - Description: Retrieve attachments uploaded by the currently authenticated user
   - Output: ApiResponse<List<AttachmentVO>>
   - Service: attachmentService.getAttachmentsByUploadedBy(userId)

 4) GET /content-type/{contentType}
   - Summary: Get attachments by content type
   - Description: Retrieve attachments by content type
   - Input: PathVariable String contentType
   - Output: ApiResponse<List<AttachmentVO>>
   - Service: attachmentService.getAttachmentsByContentType(contentType)

 5) GET /search
   - Summary: Search attachments
   - Description: Search attachments by file name keyword
   - Input: RequestParam String keyword
   - Output: ApiResponse<List<AttachmentVO>>
   - Service: attachmentService.searchAttachmentsByFileName(keyword)

 6) POST /upload
   - Summary: Upload attachment
   - Description: Upload a new file attachment
   - Input: @Valid @RequestBody AttachmentUploadRequest request
   - Output: ApiResponse<AttachmentVO>
   - Service: attachmentService.uploadAttachment(request)

 7) DELETE /{id}
   - Summary: Delete attachment
   - Description: Soft delete an attachment
   - Input: PathVariable Long id
   - Output: ApiResponse<Void>
   - Service: attachmentService.deleteAttachment(id)

 8) GET /{id}/metadata
   - Summary: Get file metadata
   - Description: Retrieve file metadata without downloading
   - Input: PathVariable Long id
   - Output: ApiResponse<AttachmentVO>
   - Service: attachmentService.getFileMetadata(id)

- DTOs
  - AttachmentUploadRequest (used in upload)

- Outputs (VOs) and ApiResponse
  - AttachmentVO
  - ApiResponse wrapper

- Observations
  - Uses getCurrentUserId() helper for user-scoped queries
  - Attachment upload uses @Valid validation on DTO

- Recommendations
  - Ensure sensitive metadata exposure is minimized on public endpoints
