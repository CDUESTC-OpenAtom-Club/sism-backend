package com.sism.service;

import com.sism.dto.AttachmentUploadRequest;
import com.sism.entity.Attachment;
import com.sism.entity.SysUser;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.AttachmentRepository;
import com.sism.repository.SysUserRepository;
import com.sism.vo.AttachmentVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AttachmentService
 */
@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private SysUserRepository sysUserRepository;

    @InjectMocks
    private AttachmentService attachmentService;

    private Attachment testAttachment;
    private SysUser testUser;

    @BeforeEach
    void setUp() {
        testAttachment = new Attachment();
        testAttachment.setId(1L);
        testAttachment.setStorageDriver("FILE");
        testAttachment.setBucket("test-bucket");
        testAttachment.setObjectKey("/uploads/test.pdf");
        testAttachment.setPublicUrl("https://example.com/test.pdf");
        testAttachment.setOriginalName("test.pdf");
        testAttachment.setContentType("application/pdf");
        testAttachment.setFileExt("pdf");
        testAttachment.setSizeBytes(1024L);
        testAttachment.setSha256("abc123");
        testAttachment.setEtag("etag123");
        testAttachment.setUploadedBy(100L);
        testAttachment.setUploadedAt(OffsetDateTime.now());
        testAttachment.setRemark("Test attachment");
        testAttachment.setIsDeleted(false);

        testUser = new SysUser();
        testUser.setId(100L);
        testUser.setUsername("testuser");
    }

    @Test
    void getAllAttachments_ShouldReturnNonDeletedAttachments() {
        // Arrange
        Attachment deletedAttachment = new Attachment();
        deletedAttachment.setId(2L);
        deletedAttachment.setStorageDriver("FILE");
        deletedAttachment.setObjectKey("/uploads/deleted.pdf");
        deletedAttachment.setOriginalName("deleted.pdf");
        deletedAttachment.setSizeBytes(512L);
        deletedAttachment.setUploadedBy(100L);
        deletedAttachment.setUploadedAt(OffsetDateTime.now());
        deletedAttachment.setIsDeleted(true);

        when(attachmentRepository.findAll()).thenReturn(Arrays.asList(testAttachment, deletedAttachment));

        // Act
        List<AttachmentVO> result = attachmentService.getAllAttachments();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getIsDeleted()).isFalse();
        verify(attachmentRepository).findAll();
    }

    @Test
    void getAttachmentById_WhenExists_ShouldReturnAttachment() {
        // Arrange
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(testAttachment));
        when(sysUserRepository.findById(100L)).thenReturn(Optional.of(testUser));

        // Act
        AttachmentVO result = attachmentService.getAttachmentById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOriginalName()).isEqualTo("test.pdf");
        assertThat(result.getUploadedByName()).isEqualTo("testuser");
        verify(attachmentRepository).findById(1L);
    }

    @Test
    void getAttachmentById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(attachmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> attachmentService.getAttachmentById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(attachmentRepository).findById(999L);
    }

    @Test
    void getAttachmentById_WhenDeleted_ShouldThrowException() {
        // Arrange
        testAttachment.setIsDeleted(true);
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(testAttachment));

        // Act & Assert
        assertThatThrownBy(() -> attachmentService.getAttachmentById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(attachmentRepository).findById(1L);
    }

    @Test
    void getAttachmentsByUploadedBy_ShouldReturnUserAttachments() {
        // Arrange
        when(attachmentRepository.findByUploadedBy(100L)).thenReturn(Arrays.asList(testAttachment));
        when(sysUserRepository.findById(100L)).thenReturn(Optional.of(testUser));

        // Act
        List<AttachmentVO> result = attachmentService.getAttachmentsByUploadedBy(100L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUploadedBy()).isEqualTo(100L);
        verify(attachmentRepository).findByUploadedBy(100L);
    }

    @Test
    void getAttachmentsByContentType_ShouldReturnMatchingAttachments() {
        // Arrange
        when(attachmentRepository.findByContentType("application/pdf")).thenReturn(Arrays.asList(testAttachment));
        when(sysUserRepository.findById(100L)).thenReturn(Optional.of(testUser));

        // Act
        List<AttachmentVO> result = attachmentService.getAttachmentsByContentType("application/pdf");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContentType()).isEqualTo("application/pdf");
        verify(attachmentRepository).findByContentType("application/pdf");
    }

    @Test
    void searchAttachmentsByFileName_ShouldReturnMatchingAttachments() {
        // Arrange
        when(attachmentRepository.findByOriginalNameContaining("test")).thenReturn(Arrays.asList(testAttachment));
        when(sysUserRepository.findById(100L)).thenReturn(Optional.of(testUser));

        // Act
        List<AttachmentVO> result = attachmentService.searchAttachmentsByFileName("test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginalName()).contains("test");
        verify(attachmentRepository).findByOriginalNameContaining("test");
    }

    @Test
    void uploadAttachment_WithValidData_ShouldUploadSuccessfully() {
        // Arrange
        AttachmentUploadRequest request = new AttachmentUploadRequest(
                "FILE",
                "test-bucket",
                "/uploads/new.pdf",
                "https://example.com/new.pdf",
                "new.pdf",
                "application/pdf",
                "pdf",
                2048L,
                "def456",
                "etag456",
                100L,
                "New attachment"
        );

        when(sysUserRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);

        // Act
        AttachmentVO result = attachmentService.uploadAttachment(request);

        // Assert
        assertThat(result).isNotNull();
        verify(sysUserRepository, atLeastOnce()).findById(100L);
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    void uploadAttachment_WithInvalidUser_ShouldThrowException() {
        // Arrange
        AttachmentUploadRequest request = new AttachmentUploadRequest(
                "FILE",
                "test-bucket",
                "/uploads/new.pdf",
                "https://example.com/new.pdf",
                "new.pdf",
                "application/pdf",
                "pdf",
                2048L,
                "def456",
                "etag456",
                999L,
                "New attachment"
        );

        when(sysUserRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> attachmentService.uploadAttachment(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(sysUserRepository).findById(999L);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void uploadAttachment_WithDefaultStorageDriver_ShouldUseFILE() {
        // Arrange
        AttachmentUploadRequest request = new AttachmentUploadRequest(
                null,
                "test-bucket",
                "/uploads/new.pdf",
                "https://example.com/new.pdf",
                "new.pdf",
                "application/pdf",
                "pdf",
                2048L,
                "def456",
                "etag456",
                100L,
                "New attachment"
        );

        when(sysUserRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);

        // Act
        AttachmentVO result = attachmentService.uploadAttachment(request);

        // Assert
        assertThat(result).isNotNull();
        verify(sysUserRepository, atLeastOnce()).findById(100L);
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    void deleteAttachment_WhenExists_ShouldSoftDelete() {
        // Arrange
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(testAttachment));
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(testAttachment);

        // Act
        attachmentService.deleteAttachment(1L);

        // Assert
        verify(attachmentRepository).findById(1L);
        verify(attachmentRepository).save(any(Attachment.class));
    }

    @Test
    void deleteAttachment_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(attachmentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> attachmentService.deleteAttachment(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(attachmentRepository).findById(999L);
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    void getFileMetadata_ShouldReturnAttachmentVO() {
        // Arrange
        when(attachmentRepository.findById(1L)).thenReturn(Optional.of(testAttachment));
        when(sysUserRepository.findById(100L)).thenReturn(Optional.of(testUser));

        // Act
        AttachmentVO result = attachmentService.getFileMetadata(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOriginalName()).isEqualTo("test.pdf");
        verify(attachmentRepository).findById(1L);
    }
}
