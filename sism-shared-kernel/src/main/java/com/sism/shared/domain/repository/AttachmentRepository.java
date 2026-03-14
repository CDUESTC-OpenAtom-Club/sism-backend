package com.sism.shared.domain.repository;

import com.sism.shared.domain.model.attachment.Attachment;

import java.util.List;

/**
 * Repository interface for Attachment aggregate
 */
public interface AttachmentRepository extends Repository<Attachment, Long> {
    
    List<Attachment> findByUploadedBy(Long uploadedBy);
    
    List<Attachment> findByContentType(String contentType);
    
    List<Attachment> findByOriginalNameContaining(String keyword);
    
    List<Attachment> findByIsDeletedFalse();
}
