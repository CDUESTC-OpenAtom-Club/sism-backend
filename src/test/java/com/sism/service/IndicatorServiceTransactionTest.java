package com.sism.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sism.AbstractIntegrationTest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.entity.SysUser;
import com.sism.enums.AuditEntityType;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.exception.BusinessException;
import com.sism.repository.AuditInstanceRepository;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

/**
 * Transaction Atomicity Tests for Indicator Service
 * 
 * **Validates: Requirement 2.3 - Transaction Atomicity**
 * 
 * These tests verify that the updateIndicator method ensures transaction atomicity:
 * - Both indicator update and approval creation succeed together
 * - Both indicator update and approval creation fail together (rollback)
 * - No partial updates occur when approval creation fails
 */
@SpringBootTest
@ActiveProfiles("test")
public class IndicatorServiceTransactionTest extends AbstractIntegrationTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private AuditInstanceRepository auditInstanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrgRepository orgRepository;

    @SpyBean
    private AuditInstanceService auditInstanceService;

    private SysUser testUser;
    private SysOrg testOrg;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Clean up test data
        auditInstanceRepository.deleteAll();
        indicatorRepository.deleteAll();

        // Create test organization
        testOrg = new SysOrg();
        testOrg.setName("测试部门");
        testOrg.setType(com.sism.enums.OrgType.FUNCTIONAL_DEPT);
        testOrg.setIsActive(true);
        testOrg.setSortOrder(1);
        testOrg.setCreatedAt(LocalDateTime.now());
        testOrg.setUpdatedAt(LocalDateTime.now());
        testOrg = orgRepository.save(testOrg);

        // Create test user
        testUser = new SysUser();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("password");
        testUser.setRealName("Test User");
        testUser.setOrg(testOrg);
        testUser.setIsActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should verify updateIndicator method has @Transactional annotation")
    void updateIndicator_shouldHaveTransactionalAnnotation() throws NoSuchMethodException {
        // Verify that the updateIndicator method has @Transactional annotation
        var method = IndicatorService.class.getMethod("updateIndicator", Long.class, IndicatorUpdateRequest.class);
        var transactionalAnnotation = method.getAnnotation(Transactional.class);
        
        assertThat(transactionalAnnotation)
                .as("updateIndicator method should have @Transactional annotation")
                .isNotNull();
    }

    @Test
    @DisplayName("Should rollback indicator update when approval creation fails")
    @Transactional
    void updateIndicator_shouldRollbackWhenApprovalCreationFails() {
        // Given: Create an indicator
        Indicator indicator = createIndicator("计算机学院");
        Indicator savedIndicator = indicatorRepository.save(indicator);
        
        String originalDescription = savedIndicator.getIndicatorDesc();
        String originalStatusAudit = savedIndicator.getStatusAudit();
        
        // Mock approval service to throw exception
        doThrow(new RuntimeException("Simulated approval creation failure"))
                .when(auditInstanceService)
                .createAuditInstance(anyString(), any(AuditEntityType.class), anyLong(), anyLong());

        // When: Try to distribute indicator (which should trigger approval creation)
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setIndicatorDesc("Updated description during distribution");
        request.setStatusAudit(createDistributeStatusAudit());

        // Then: Should throw BusinessException
        assertThatThrownBy(() -> indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("下发失败");

        // Verify: Indicator update was rolled back (description should remain unchanged)
        // Note: In a real transaction rollback scenario, we need to clear the persistence context
        // and re-fetch from database to see the rolled-back state
        indicatorRepository.flush();
        
        Indicator afterFailure = indicatorRepository.findById(savedIndicator.getIndicatorId()).orElseThrow();
        
        // The indicator should NOT have the updated description (rollback occurred)
        assertThat(afterFailure.getIndicatorDesc())
                .as("Indicator description should be rolled back when approval creation fails")
                .isEqualTo(originalDescription);
        
        // The indicator should NOT have the distribute statusAudit (rollback occurred)
        assertThat(afterFailure.getStatusAudit())
                .as("Indicator statusAudit should be rolled back when approval creation fails")
                .isEqualTo(originalStatusAudit);

        // Verify: No approval instance was created
        var approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());
        
        assertThat(approvalInstance)
                .as("No approval instance should exist after rollback")
                .isEmpty();
    }

    @Test
    @DisplayName("Should commit both indicator update and approval creation on success")
    @Transactional
    void updateIndicator_shouldCommitBothOperationsOnSuccess() {
        // Given: Create an indicator
        Indicator indicator = createIndicator("计算机学院");
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // When: Distribute indicator successfully
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setIndicatorDesc("Updated description during distribution");
        request.setStatusAudit(createDistributeStatusAudit());

        var result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // Then: Both indicator update and approval creation should succeed
        indicatorRepository.flush();
        
        Indicator afterUpdate = indicatorRepository.findById(savedIndicator.getIndicatorId()).orElseThrow();
        
        // Verify: Indicator was updated
        assertThat(afterUpdate.getIndicatorDesc())
                .as("Indicator description should be updated")
                .isEqualTo("Updated description during distribution");
        
        assertThat(afterUpdate.getStatusAudit())
                .as("Indicator statusAudit should contain distribute action")
                .contains("distribute");

        // Verify: Approval instance was created
        var approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());
        
        assertThat(approvalInstance)
                .as("Approval instance should be created on successful distribution")
                .isPresent();
        
        assertThat(approvalInstance.get().getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
        assertThat(approvalInstance.get().getEntityId()).isEqualTo(savedIndicator.getIndicatorId());
    }

    @Test
    @DisplayName("Should handle non-distribution updates without triggering approval workflow")
    @Transactional
    void updateIndicator_shouldHandleNonDistributionUpdatesWithoutApproval() {
        // Given: Create an indicator
        Indicator indicator = createIndicator("计算机学院");
        Indicator savedIndicator = indicatorRepository.save(indicator);

        // When: Update indicator without distribution action
        IndicatorUpdateRequest request = new IndicatorUpdateRequest();
        request.setIndicatorDesc("Simple description update");
        request.setProgress(50);

        var result = indicatorService.updateIndicator(savedIndicator.getIndicatorId(), request);

        // Then: Indicator should be updated without approval creation
        indicatorRepository.flush();
        
        Indicator afterUpdate = indicatorRepository.findById(savedIndicator.getIndicatorId()).orElseThrow();
        
        // Verify: Indicator was updated
        assertThat(afterUpdate.getIndicatorDesc())
                .as("Indicator description should be updated")
                .isEqualTo("Simple description update");
        
        assertThat(afterUpdate.getProgress())
                .as("Indicator progress should be updated")
                .isEqualTo(50);

        // Verify: No approval instance was created
        var approvalInstance = auditInstanceRepository
                .findActiveInstanceByEntity(AuditEntityType.INDICATOR, savedIndicator.getIndicatorId());
        
        assertThat(approvalInstance)
                .as("No approval instance should be created for non-distribution updates")
                .isEmpty();
    }

    // ==================== Helper Methods ====================

    /**
     * Create an indicator with specified responsible department
     */
    private Indicator createIndicator(String responsibleDept) {
        Indicator indicator = new Indicator();
        indicator.setTaskId(1L);
        indicator.setLevel(IndicatorLevel.SECONDARY);
        indicator.setOwnerOrg(testOrg);
        indicator.setTargetOrg(testOrg);
        indicator.setIndicatorDesc("Test indicator for " + responsibleDept);
        indicator.setWeightPercent(new BigDecimal("10.00"));
        indicator.setSortOrder(1);
        indicator.setType("QUANTITATIVE");
        indicator.setProgress(0);
        indicator.setStatus(IndicatorStatus.ACTIVE);
        indicator.setIsDeleted(false);
        indicator.setCreatedAt(LocalDateTime.now());
        indicator.setUpdatedAt(LocalDateTime.now());
        indicator.setResponsibleDept(responsibleDept);
        return indicator;
    }

    /**
     * Create statusAudit JSON with "distribute" action
     */
    private String createDistributeStatusAudit() {
        try {
            ArrayNode auditArray = objectMapper.createArrayNode();
            ObjectNode auditRecord = objectMapper.createObjectNode();
            auditRecord.put("action", "distribute");
            auditRecord.put("timestamp", LocalDateTime.now().toString());
            auditRecord.put("userId", testUser.getId());
            auditRecord.put("userName", testUser.getRealName());
            auditArray.add(auditRecord);
            return objectMapper.writeValueAsString(auditArray);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create statusAudit JSON", e);
        }
    }
}
