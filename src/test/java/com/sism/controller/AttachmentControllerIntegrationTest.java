package com.sism.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sism.entity.Attachment;
import com.sism.entity.SysUser;
import com.sism.repository.AttachmentRepository;
import com.sism.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AttachmentController security fixes
 * Tests that user identity is extracted from security context, not URL parameters
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttachmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private SysUser testUser1;
    private SysUser testUser2;
    private Attachment attachment1;
    private Attachment attachment2;

    @BeforeEach
    void setUp() {
        // Clean up
        attachmentRepository.deleteAll();
        
        // Create test users
        testUser1 = userRepository.findByUsername("testuser1")
                .orElseGet(() -> {
                    SysUser user = new SysUser();
                    user.setUsername("testuser1");
                    user.setRealName("Test User 1");
                    user.setPasswordHash("hashedpassword");
                    user.setIsActive(true);
                    return userRepository.save(user);
                });

        testUser2 = userRepository.findByUsername("testuser2")
                .orElseGet(() -> {
                    SysUser user = new SysUser();
                    user.setUsername("testuser2");
                    user.setRealName("Test User 2");
                    user.setPasswordHash("hashedpassword");
                    user.setIsActive(true);
                    return userRepository.save(user);
                });

        // Create test attachments
        attachment1 = new Attachment();
        attachment1.setStorageDriver("FILE");
        attachment1.setOriginalName("test1.pdf");
        attachment1.setContentType("application/pdf");
        attachment1.setFileExt("pdf");
        attachment1.setSizeBytes(1024L);
        attachment1.setUploadedBy(testUser1.getId());
        attachment1.setUploadedAt(OffsetDateTime.now());
        attachment1.setIsDeleted(false);
        attachment1 = attachmentRepository.save(attachment1);

        attachment2 = new Attachment();
        attachment2.setStorageDriver("FILE");
        attachment2.setOriginalName("test2.pdf");
        attachment2.setContentType("application/pdf");
        attachment2.setFileExt("pdf");
        attachment2.setSizeBytes(2048L);
        attachment2.setUploadedBy(testUser2.getId());
        attachment2.setUploadedAt(OffsetDateTime.now());
        attachment2.setIsDeleted(false);
        attachment2 = attachmentRepository.save(attachment2);
    }

    @Test
    @WithMockUser(username = "testuser1")
    void getAttachmentsByUploadedBy_shouldReturnOnlyCurrentUserAttachments() throws Exception {
        // When: User1 requests their attachments
        mockMvc.perform(get("/attachments/user")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return only user1's attachments
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].originalName").value("test1.pdf"))
                .andExpect(jsonPath("$.data[0].uploadedBy").value(testUser1.getId()));
    }

    @Test
    @WithMockUser(username = "testuser2")
    void getAttachmentsByUploadedBy_shouldReturnDifferentUserAttachments() throws Exception {
        // When: User2 requests their attachments
        mockMvc.perform(get("/attachments/user")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return only user2's attachments
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].originalName").value("test2.pdf"))
                .andExpect(jsonPath("$.data[0].uploadedBy").value(testUser2.getId()));
    }

    @Test
    void getAttachmentsByUploadedBy_shouldFailWithoutAuthentication() throws Exception {
        // When: Unauthenticated user requests attachments
        mockMvc.perform(get("/attachments/user")
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 401 Unauthorized
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser1")
    void getAttachmentsByUploadedBy_shouldNotAcceptUserIdParameter() throws Exception {
        // When: User1 tries to access user2's attachments via old URL pattern
        // The new endpoint doesn't accept userId parameter, so this should return 404
        mockMvc.perform(get("/attachments/user/" + testUser2.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                // Then: Should return 404 Not Found (endpoint doesn't exist)
                .andExpect(status().isNotFound());
    }
}
