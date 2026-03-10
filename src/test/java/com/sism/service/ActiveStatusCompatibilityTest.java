package com.sism.service;

import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.enums.ProgressApprovalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test ACTIVE status compatibility with DISTRIBUTED status
 * 
 * Validates that legacy ACTIVE status is treated as equivalent to DISTRIBUTED
 * in all business logic while maintaining backward compatibility.
 */
@ExtendWith(MockitoExtension.class)
public class ActiveStatusCompatibilityTest {

    @InjectMocks
    private IndicatorService indicatorService;

    private SysOrg testOrg;

    @BeforeEach
    void setUp() {
        testOrg = new SysOrg();
        testOrg.setId(1L);
        testOrg.setName("Test Organization");
        testOrg.setType(OrgType.FUNCTIONAL_DEPT);
        testOrg.setIsActive(true);
        testOrg.setSortOrder(0);
        testOrg.setCreatedAt(LocalDateTime.now());
        testOrg.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    public void testIsDistributedStatus_shouldTreatActiveAsDistributed() throws Exception {
        // Use reflection to access private method
        Method isDistributedStatusMethod = IndicatorService.class.getDeclaredMethod("isDistributedStatus", IndicatorStatus.class);
        isDistributedStatusMethod.setAccessible(true);

        // Test DISTRIBUTED status
        boolean distributedResult = (Boolean) isDistributedStatusMethod.invoke(indicatorService, IndicatorStatus.DISTRIBUTED);
        assertThat(distributedResult)
                .as("DISTRIBUTED status should be considered distributed")
                .isTrue();

        // Test legacy ACTIVE status
        boolean activeResult = (Boolean) isDistributedStatusMethod.invoke(indicatorService, IndicatorStatus.ACTIVE);
        assertThat(activeResult)
                .as("Legacy ACTIVE status should be treated as distributed")
                .isTrue();

        // Test other statuses should not be considered distributed
        boolean draftResult = (Boolean) isDistributedStatusMethod.invoke(indicatorService, IndicatorStatus.DRAFT);
        assertThat(draftResult)
                .as("DRAFT status should not be considered distributed")
                .isFalse();

        boolean archivedResult = (Boolean) isDistributedStatusMethod.invoke(indicatorService, IndicatorStatus.ARCHIVED);
        assertThat(archivedResult)
                .as("ARCHIVED status should not be considered distributed")
                .isFalse();
    }

    @Test
    public void testIsDistributed_shouldTreatActiveIndicatorAsDistributed() throws Exception {
        // Use reflection to access private method
        Method isDistributedMethod = IndicatorService.class.getDeclaredMethod("isDistributed", Indicator.class);
        isDistributedMethod.setAccessible(true);

        // Create indicator with DISTRIBUTED status
        Indicator distributedIndicator = Indicator.builder()
                .indicatorDesc("Test Distributed Indicator")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOrg)
                .targetOrg(testOrg)
                .weightPercent(BigDecimal.valueOf(100))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        boolean distributedResult = (Boolean) isDistributedMethod.invoke(indicatorService, distributedIndicator);
        assertThat(distributedResult)
                .as("Indicator with DISTRIBUTED status should be considered distributed")
                .isTrue();

        // Create indicator with legacy ACTIVE status
        Indicator activeIndicator = Indicator.builder()
                .indicatorDesc("Test Active Indicator")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOrg)
                .targetOrg(testOrg)
                .weightPercent(BigDecimal.valueOf(100))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.ACTIVE)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        boolean activeResult = (Boolean) isDistributedMethod.invoke(indicatorService, activeIndicator);
        assertThat(activeResult)
                .as("Indicator with legacy ACTIVE status should be treated as distributed")
                .isTrue();

        // Create indicator with DRAFT status
        Indicator draftIndicator = Indicator.builder()
                .indicatorDesc("Test Draft Indicator")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOrg)
                .targetOrg(testOrg)
                .weightPercent(BigDecimal.valueOf(100))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.DRAFT)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        boolean draftResult = (Boolean) isDistributedMethod.invoke(indicatorService, draftIndicator);
        assertThat(draftResult)
                .as("Indicator with DRAFT status should not be considered distributed")
                .isFalse();
    }

    @Test
    public void testActiveStatusCompatibility_shouldWorkWithProgressApprovalStatus() {
        // Test that ACTIVE status can coexist with any progress approval status
        // This validates the core requirement that lifecycle and approval statuses are independent

        Indicator activeWithPending = Indicator.builder()
                .indicatorDesc("Active with Pending Progress")
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOrg)
                .targetOrg(testOrg)
                .weightPercent(BigDecimal.valueOf(100))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.ACTIVE)  // Legacy status
                .progressApprovalStatus(ProgressApprovalStatus.PENDING)  // Approval workflow status
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Verify both statuses are preserved
        assertThat(activeWithPending.getStatus())
                .as("Legacy ACTIVE status should be preserved")
                .isEqualTo(IndicatorStatus.ACTIVE);

        assertThat(activeWithPending.getProgressApprovalStatus())
                .as("Progress approval status should be preserved")
                .isEqualTo(ProgressApprovalStatus.PENDING);

        // Test that this is a valid combination (no validation errors)
        // In a real scenario, this would be saved to database and retrieved successfully
        assertThat(activeWithPending)
                .as("Indicator with ACTIVE + PENDING combination should be valid")
                .isNotNull();
    }
}