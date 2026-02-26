package com.sism.service;

import com.sism.dto.AttachmentUploadRequest;
import com.sism.entity.Attachment;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.AttachmentRepository;
import com.sism.repository.SysUserRepository;
import com.sism.vo.AttachmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for attachment management
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final SysUserRepository sysUserRepository;

    /**
     * Get all attachments
     */
    public List<AttachmentVO> getAllAttachments() {
        return attachmentRepository.findAll().stream()
                .filter(a -> !a.getIsDeleted())
                .map(this::toAttachmentVO)
                .collect(Collectors.toList());
    }

    /**
     * Get attachment by ID
     */
    public AttachmentVO getAttachmentById(Long id) {
        Attachment attachment = findAttachmentById(id);
        return toAttachmentVO(attachment);
    }

    /**
     * Get attachments by uploaded user ID
     */
    public List<AttachmentVO> getAttachmentsByUploadedBy(Long userId) {
        return attachmentRepository.findByUploadedBy(userId).stream()
                .filter(a -> !a.getIsDeleted())
                .map(this::toAttachmentVO)
                .collect(Collectors.toList());
    }

    /**
     * Get attachments by content type
     */
    public List<AttachmentVO> getAttachmentsByContentType(String contentType) {
        return attachmentRepository.findByContentType(contentType).stream()
                .filter(a -> !a.getIsDeleted())
                .map(this::toAttachmentVO)
                .collect(Collectors.toList());
    }

    /**
     * Search attachments by original file name
     */
    public List<AttachmentVO> searchAttachmentsByFileName(String keyword) {
        return attachmentRepository.findByOriginalNameContaining(keyword).stream()
                .filter(a -> !a.getIsDeleted())
                .map(this::toAttachmentVO)
                .collect(Collectors.toList());
    }

    /**
     * Upload a new attachment
     */
    @Transactional
    public AttachmentVO uploadAttachment(AttachmentUploadRequest request) {
        log.info("Uploading attachment: {}", request.getOriginalName());

        // Validate user exists
        sysUserRepository.findById(request.getUploadedBy())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUploadedBy()));

        Attachment attachment = new Attachment();
        attachment.setStorageDriver(request.getStorageDriver() != null ? request.getStorageDriver() : "FILE");
        attachment.setBucket(request.getBucket());
        attachment.setObjectKey(request.getObjectKey());
        attachment.setPublicUrl(request.getPublicUrl());
        attachment.setOriginalName(request.getOriginalName());
        attachment.setContentType(request.getContentType());
        attachment.setFileExt(request.getFileExt());
        attachment.setSizeBytes(request.getSizeBytes());
        attachment.setSha256(request.getSha256());
        attachment.setEtag(request.getEtag());
        attachment.setUploadedBy(request.getUploadedBy());
        attachment.setUploadedAt(OffsetDateTime.now());
        attachment.setRemark(request.getRemark());
        attachment.setIsDeleted(false);

        Attachment savedAttachment = attachmentRepository.save(attachment);
        log.info("Successfully uploaded attachment with ID: {}", savedAttachment.getId());

        return toAttachmentVO(savedAttachment);
    }

    /**
     * Delete an attachment (soft delete)
     */
    @Transactional
    public void deleteAttachment(Long id) {
        Attachment attachment = findAttachmentById(id);
        attachment.setIsDeleted(true);
        attachment.setDeletedAt(OffsetDateTime.now());
        attachmentRepository.save(attachment);
        log.info("Soft deleted attachment with ID: {}", id);
    }

    /**
     * Get file metadata
     */
    public AttachmentVO getFileMetadata(Long id) {
        return getAttachmentById(id);
    }

    /**
     * Find attachment entity by ID
     */
    private Attachment findAttachmentById(Long id) {
        return attachmentRepository.findById(id)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", id));
    }

    /**
     * Convert Attachment entity to VO
     */
    private AttachmentVO toAttachmentVO(Attachment attachment) {
        String uploadedByName = null;
        if (attachment.getUploadedBy() != null) {
            uploadedByName = sysUserRepository.findById(attachment.getUploadedBy())
                    .map(user -> user.getUsername())
                    .orElse(null);
        }

        return new AttachmentVO(
                attachment.getId(),
                attachment.getStorageDriver(),
                attachment.getBucket(),
                attachment.getObjectKey(),
                attachment.getPublicUrl(),
                attachment.getOriginalName(),
                attachment.getContentType(),
                attachment.getFileExt(),
                attachment.getSizeBytes(),
                attachment.getSha256(),
                attachment.getEtag(),
                attachment.getUploadedBy(),
                uploadedByName,
                attachment.getUploadedAt(),
                attachment.getRemark(),
                attachment.getIsDeleted(),
                attachment.getDeletedAt()
        );
    }
}
