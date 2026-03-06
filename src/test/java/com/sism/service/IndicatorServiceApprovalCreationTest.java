package com.sism.service;

import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.entity.AssessmentCycle;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.entity.StrategicTask;
import com.sism.enums.AuditEntityType;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.exception.BusinessException;
import com.sism.repository.AssessmentCycleRepository;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.util.TestDataFactory;
import com.sism.vo.IndicatorVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for automatic approval instance creation on indicator distribution
 * 
 * **Validates: Requirements 2.1, 2.3**
 * 
 * Task 4.3: Test updateIndicator creates approval instance on distribution
 * - Mock AuditInstanceService
 * - Call updateIndicator with statusAudit containing action="distribute"
 * - Verify createAuditInstance is called with correct parameters
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class IndicatorServiceApprovalCreationTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private SysOrgRepository orgRepository;

    @Autowired
    private AssessmentCycleRepository cycleRepository;

    @SpyBean
    private AuditInstanceService auditInstanceService;

    private StrategicTask testTask;
    private SysOrg testOwnerOrg;
    private SysOrg testTargetOrg;

    @BeforeEach
    void setUp() {
        // Create test cycle
        AssessmentCycle cycle = TestDataFactory.createTestCycle(cycleRepository);
        
        // Create test organizations
        testOwnerOrg = TestDataFactory.createTestOrg(orgRepository, "测试职能部门", OrgType.FUNCTIONAL_DEPT);
        testTargetOrg = TestDataFactory.createTestOrg(orgRepository, "测试二级学院", OrgType.COLLEGE);
        
        // Create test task
        testTask = TestDataFactory.createTestTask(taskRepository, cycleRepository, orgRepository);
    }

    @Test
    @DisplayName("Should create approval instance when updateIndicator is called with distribute action")
    void shouldCreateApprovalInstanceOnDistribution() {
        // Given - Create an indicator to distribute
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("Test Indicator for Distribution");
        createRequest.setWeightPercent(new BigDecimal("15.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);

        // Create update request with distribute action in statusAudit
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]";
        updateRequest.setStatusAudit(statusAuditJson);

        // When - Call updateIndicator with distribution action
        IndicatorVO result = indicatorService.updateIndicator(created.getIndicatorId(), updateRequest);

        // Then - Verify indicator was updated
        assertThat(result).isNotNull();
        assertThat(result.getIndicatorId()).isEqualTo(created.getIndicatorId());
        
        // Verify createAuditInstance was called with correct parameters
        verify(auditInstanceService).createAuditInstance(
            any(String.class),  // flowCode (INDICATOR_COLLEGE_APPROVAL or INDICATOR_DEFAULT_APPROVAL)
            eq(AuditEntityType.INDICATOR),  // entityType
            eq(created.getIndicatorId()),  // entityId
            anyLong()  // submitterId
        );
    }

    @Test
    @DisplayName("Should create approval instance with college flow code for college department")
    void shouldCreateApprovalInstanceWithCollegeFlowCode() {
        // Given - Create an indicator with college department
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("College Indicator for Distribution");
        createRequest.setWeightPercent(new BigDecimal("20.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);

        // Update the indicator to set responsibleDept to a college department
        Indicator indicator = indicatorRepository.findById(created.getIndicatorId()).orElseThrow();
        indicator.setResponsibleDept("计算机学院");
        indicatorRepository.save(indicator);

        // Create update request with distribute action
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]";
        updateRequest.setStatusAudit(statusAuditJson);

        // When - Call updateIndicator with distribution action
        IndicatorVO result = indicatorService.updateIndicator(created.getIndicatorId(), updateRequest);

        // Then - Verify indicator was updated
        assertThat(result).isNotNull();
        assertThat(result.getIndicatorId()).isEqualTo(created.getIndicatorId());
        
        // Verify createAuditInstance was called with INDICATOR_COLLEGE_APPROVAL flow code
        verify(auditInstanceService).createAuditInstance(
            eq("INDICATOR_COLLEGE_APPROVAL"),  // flowCode for college departments
            eq(AuditEntityType.INDICATOR),  // entityType
            eq(created.getIndicatorId()),  // entityId
            anyLong()  // submitterId
        );
    }

    @Test
    @DisplayName("Should create approval instance with default flow code for non-college department")
    void shouldCreateApprovalInstanceWithDefaultFlowCode() {
        // Given - Create an indicator with non-college department
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("Non-College Indicator for Distribution");
        createRequest.setWeightPercent(new BigDecimal("25.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);

        // Update the indicator to set responsibleDept to a non-college department
        Indicator indicator = indicatorRepository.findById(created.getIndicatorId()).orElseThrow();
        indicator.setResponsibleDept("人事处");
        indicatorRepository.save(indicator);

        // Create update request with distribute action
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]";
        updateRequest.setStatusAudit(statusAuditJson);

        // When - Call updateIndicator with distribution action
        IndicatorVO result = indicatorService.updateIndicator(created.getIndicatorId(), updateRequest);

        // Then - Verify indicator was updated
        assertThat(result).isNotNull();
        assertThat(result.getIndicatorId()).isEqualTo(created.getIndicatorId());
        
        // Verify createAuditInstance was called with INDICATOR_DEFAULT_APPROVAL flow code
        verify(auditInstanceService).createAuditInstance(
            eq("INDICATOR_DEFAULT_APPROVAL"),  // flowCode for non-college departments
            eq(AuditEntityType.INDICATOR),  // entityType
            eq(created.getIndicatorId()),  // entityId
            anyLong()  // submitterId
        );
    }


    @Test
    @DisplayName("Should NOT create approval instance when statusAudit is null")
    void shouldNotCreateApprovalInstanceWhenStatusAuditIsNull() {
        // Given - Create an indicator to update
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("Test Indicator for Non-Distribution Update");
        createRequest.setWeightPercent(new BigDecimal("10.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);

        // Create update request with null statusAudit (non-distribution update)
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        updateRequest.setStatusAudit(null);
        updateRequest.setIndicatorDesc("Updated description without distribution");

        // When - Call updateIndicator without distribution action
        IndicatorVO result = indicatorService.updateIndicator(created.getIndicatorId(), updateRequest);

        // Then - Verify indicator was updated
        assertThat(result).isNotNull();
        assertThat(result.getIndicatorId()).isEqualTo(created.getIndicatorId());
        assertThat(result.getIndicatorDesc()).isEqualTo("Updated description without distribution");

        // Verify createAuditInstance was NOT called
        verify(auditInstanceService, never()).createAuditInstance(
            any(String.class),
            any(AuditEntityType.class),
            anyLong(),
            anyLong()
        );
    }
    @Test
    @DisplayName("Should NOT create approval instance when statusAudit contains non-distribute actions")
    void shouldNotCreateApprovalInstanceForNonDistributeActions() {
        // Given - Create an indicator to update
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("Test Indicator for Non-Distribute Action");
        createRequest.setWeightPercent(new BigDecimal("12.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);

        // Test various non-distribute actions
        String[] nonDistributeActions = {
            "[{\"action\":\"edit\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]",
            "[{\"action\":\"update\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]",
            "[{\"action\":\"review\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]",
            "[{\"action\":\"approve\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]",
            "[{\"action\":\"reject\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]"
        };

        for (String statusAuditJson : nonDistributeActions) {
            // Create update request with non-distribute action
            IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
            updateRequest.setStatusAudit(statusAuditJson);
            updateRequest.setIndicatorDesc("Updated with non-distribute action");

            // When - Call updateIndicator with non-distribute action
            IndicatorVO result = indicatorService.updateIndicator(created.getIndicatorId(), updateRequest);

            // Then - Verify indicator was updated
            assertThat(result).isNotNull();
            assertThat(result.getIndicatorId()).isEqualTo(created.getIndicatorId());
        }

        // Verify createAuditInstance was NEVER called for any of the non-distribute actions
        verify(auditInstanceService, never()).createAuditInstance(
            any(String.class),
            any(AuditEntityType.class),
            anyLong(),
            anyLong()
        );
    }

    @Test
    @DisplayName("Should throw BusinessException when approval creation fails")
    void shouldThrowBusinessExceptionWhenApprovalCreationFails() {
        // Given - Create an indicator to distribute
        IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
        createRequest.setTaskId(testTask.getTaskId());
        createRequest.setOwnerOrgId(testOwnerOrg.getId());
        createRequest.setTargetOrgId(testTargetOrg.getId());
        createRequest.setLevel(IndicatorLevel.PRIMARY.name());
        createRequest.setIndicatorDesc("Test Indicator for Failed Approval Creation");
        createRequest.setWeightPercent(new BigDecimal("18.00"));
        createRequest.setYear(2025);

        IndicatorVO created = indicatorService.createIndicator(createRequest);

        // Mock AuditInstanceService to throw exception when createAuditInstance is called
        doThrow(new RuntimeException("Simulated approval service failure"))
            .when(auditInstanceService)
            .createAuditInstance(
                any(String.class),
                eq(AuditEntityType.INDICATOR),
                eq(created.getIndicatorId()),
                anyLong()
            );

        // Create update request with distribute action
        IndicatorUpdateRequest updateRequest = new IndicatorUpdateRequest();
        String statusAuditJson = "[{\"action\":\"distribute\",\"timestamp\":\"2025-01-15T10:00:00\",\"userId\":1}]";
        updateRequest.setStatusAudit(statusAuditJson);

        // When/Then - Call updateIndicator and verify BusinessException is thrown
        assertThatThrownBy(() -> indicatorService.updateIndicator(created.getIndicatorId(), updateRequest))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("下发失败")
            .hasMessageContaining("无法创建审批实例");
    }


}
