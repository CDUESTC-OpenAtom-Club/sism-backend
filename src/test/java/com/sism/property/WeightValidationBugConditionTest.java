package com.sism.property;

import com.sism.AbstractIntegrationTest;
import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.OrgType;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.OrgRepository;
import com.sism.service.IndicatorService;
import com.sism.vo.IndicatorVO;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bug Condition Exploration Test for Weight Validation Bugfix
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6**
 * 
 * **CRITICAL**: This test MUST FAIL on unfixed code - failure confirms the bug exists.
 * 
 * **Property 1: Bug Condition** - Backend Accepts Invalid Weight Distributions
 * 
 * This test encodes the EXPECTED behavior (backend should reject invalid weight distributions).
 * On UNFIXED code, this test will FAIL because the backend incorrectly accepts invalid distributions.
 * After the fix is implemented, this test will PASS, validating the fix works correctly.
 * 
 * **GOAL**: Surface counterexamples that demonstrate the bug exists:
 * - Backend accepts distributions with weights summing to 90% (should reject)
 * - Backend accepts distributions with weights summing to 120% (should reject)
 * - Backend accepts distributions with negative weights (should reject)
 * - Backend accepts distributions with zero total weights (should reject)
 * 
 * **Scoped PBT Approach**: Test concrete failing cases - distribution actions with invalid
 * weight totals that should be rejected but are currently accepted on unfixed code.
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class WeightValidationBugConditionTest extends AbstractIntegrationTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private OrgRepository orgRepository;

    private SysOrg testOrg1;
    private SysOrg testOrg2;
    private SysOrg testOrg3;

    @BeforeEach
    void setUp() {
        // Clean up test data
        indicatorRepository.deleteAll();

        // Create test organizations for distribution targets
        testOrg1 = createOrg("财务部");
        testOrg2 = createOrg("人力资源部");
        testOrg3 = createOrg("技术部");
    }

    /**
     * Property 1.1: Backend Should Reject Weights Summing to 90%
     * 
     * EXPECTED BEHAVIOR: When distributing a parent indicator with 3 child indicators
     * whose weights sum to 90% (30% + 30% + 30%), the backend should reject the request
     * with a 400 error and message "权重总和必须为100%，当前为90%".
     * 
     * ACTUAL BEHAVIOR ON UNFIXED CODE: Backend accepts the request and creates invalid
     * distribution (TEST WILL FAIL).
     * 
     * This failure confirms the bug exists.
     */
    @Property(tries = 5)
    @Label("Bugfix: weight-validation-bugfix, Property 1.1: Weights Sum to 90% Bug")
    void weightsSumTo90_shouldBeRejected() {
        // Create parent indicator
        Indicator parent = createParentIndicator("Parent Indicator for 90% Test");
        
        // Create 3 child indicators of the parent with weights: 30%, 30%, 30% (sum = 90%)
        createChildIndicator(parent, new BigDecimal("30.00"));
        createChildIndicator(parent, new BigDecimal("30.00"));
        createChildIndicator(parent, new BigDecimal("30.00"));

        // EXPECTED BEHAVIOR: Backend should reject batch distribution with validation error
        // On UNFIXED code, this assertion will FAIL (backend accepts invalid distribution)
        assertThatThrownBy(() -> {
            indicatorService.batchDistributeIndicator(
                parent.getIndicatorId(),
                Arrays.asList(testOrg1.getId(), testOrg2.getId(), testOrg3.getId()),
                null
            );
        })
        .as("Backend should reject distribution when parent's children weights sum to 90%")
        .isInstanceOf(Exception.class)
        .hasMessageContaining("权重")
        .hasMessageContaining("100%");

        // Verify no NEW distributed indicators were created (transaction should rollback)
        // The 3 child indicators we created above should still exist, but no new ones from distribution
        List<Indicator> allChildren = indicatorRepository
            .findByParentIndicatorIdDirect(parent.getIndicatorId());
        assertThat(allChildren)
            .as("Only the 3 original child indicators should exist, no new distributed ones")
            .hasSize(3);
    }

    /**
     * Property 1.2: Backend Should Reject Weights Summing to 120%
     * 
     * EXPECTED BEHAVIOR: When distributing a parent indicator with 3 child indicators
     * whose weights sum to 120% (40% + 40% + 40%), the backend should reject the request
     * with a 400 error and message "权重总和必须为100%，当前为120%".
     * 
     * ACTUAL BEHAVIOR ON UNFIXED CODE: Backend accepts the request and creates invalid
     * distribution (TEST WILL FAIL).
     */
    @Property(tries = 5)
    @Label("Bugfix: weight-validation-bugfix, Property 1.2: Weights Sum to 120% Bug")
    void weightsSumTo120_shouldBeRejected() {
        // Create parent indicator
        Indicator parent = createParentIndicator("Parent Indicator for 120% Test");
        
        // Create 3 child indicators of the parent with weights: 40%, 40%, 40% (sum = 120%)
        createChildIndicator(parent, new BigDecimal("40.00"));
        createChildIndicator(parent, new BigDecimal("40.00"));
        createChildIndicator(parent, new BigDecimal("40.00"));

        // EXPECTED BEHAVIOR: Backend should reject batch distribution with validation error
        assertThatThrownBy(() -> {
            indicatorService.batchDistributeIndicator(
                parent.getIndicatorId(),
                Arrays.asList(testOrg1.getId(), testOrg2.getId(), testOrg3.getId()),
                null
            );
        })
        .as("Backend should reject distribution when parent's children weights sum to 120%")
        .isInstanceOf(Exception.class)
        .hasMessageContaining("权重")
        .hasMessageContaining("100%");

        // Verify no NEW distributed indicators were created
        List<Indicator> allChildren = indicatorRepository
            .findByParentIndicatorIdDirect(parent.getIndicatorId());
        assertThat(allChildren)
            .as("Only the 3 original child indicators should exist, no new distributed ones")
            .hasSize(3);
    }

    /**
     * Property 1.3: Backend Should Reject Negative Weights
     * 
     * EXPECTED BEHAVIOR: When distributing a parent indicator with child indicators
     * including negative weights (-50% + 150% = 100%), the backend should reject
     * the request with error "权重值必须在0-100之间".
     * 
     * ACTUAL BEHAVIOR ON UNFIXED CODE: Backend accepts the request despite semantically
     * invalid weights (TEST WILL FAIL).
     */
    @Property(tries = 5)
    @Label("Bugfix: weight-validation-bugfix, Property 1.3: Negative Weights Bug")
    void negativeWeights_shouldBeRejected() {
        // Create parent indicator
        Indicator parent = createParentIndicator("Parent Indicator for Negative Weight Test");
        
        // Create 2 child indicators of the parent with weights: -50%, 150% (sum = 100% but invalid)
        createChildIndicator(parent, new BigDecimal("-50.00"));
        createChildIndicator(parent, new BigDecimal("150.00"));

        // EXPECTED BEHAVIOR: Backend should reject batch distribution with validation error
        assertThatThrownBy(() -> {
            indicatorService.batchDistributeIndicator(
                parent.getIndicatorId(),
                Arrays.asList(testOrg1.getId(), testOrg2.getId()),
                null
            );
        })
        .as("Backend should reject distribution when parent's children have negative or >100 weights")
        .isInstanceOf(Exception.class)
        .hasMessageContaining("权重");

        // Verify no NEW distributed indicators were created
        List<Indicator> allChildren = indicatorRepository
            .findByParentIndicatorIdDirect(parent.getIndicatorId());
        assertThat(allChildren)
            .as("Only the 2 original child indicators should exist, no new distributed ones")
            .hasSize(2);
    }

    /**
     * Property 1.4: Backend Should Reject Zero Total Weights
     * 
     * EXPECTED BEHAVIOR: When distributing a parent indicator with all zero weights
     * (0% + 0% + 0% = 0%), the backend should reject the request with error
     * "权重总和必须为100%，当前为0%".
     * 
     * ACTUAL BEHAVIOR ON UNFIXED CODE: Backend accepts the request and creates
     * meaningless distribution (TEST WILL FAIL).
     */
    @Property(tries = 5)
    @Label("Bugfix: weight-validation-bugfix, Property 1.4: Zero Total Weights Bug")
    void zeroTotalWeights_shouldBeRejected() {
        // Create parent indicator
        Indicator parent = createParentIndicator("Parent Indicator for Zero Weight Test");
        
        // Create 3 child indicators of the parent with weights: 0%, 0%, 0% (sum = 0%)
        createChildIndicator(parent, BigDecimal.ZERO);
        createChildIndicator(parent, BigDecimal.ZERO);
        createChildIndicator(parent, BigDecimal.ZERO);

        // EXPECTED BEHAVIOR: Backend should reject batch distribution with validation error
        assertThatThrownBy(() -> {
            indicatorService.batchDistributeIndicator(
                parent.getIndicatorId(),
                Arrays.asList(testOrg1.getId(), testOrg2.getId(), testOrg3.getId()),
                null
            );
        })
        .as("Backend should reject distribution when parent's children weights sum to 0%")
        .isInstanceOf(Exception.class)
        .hasMessageContaining("权重")
        .hasMessageContaining("100%");

        // Verify no NEW distributed indicators were created
        List<Indicator> allChildren = indicatorRepository
            .findByParentIndicatorIdDirect(parent.getIndicatorId());
        assertThat(allChildren)
            .as("Only the 3 original child indicators should exist, no new distributed ones")
            .hasSize(3);
    }

    // ==================== Helper Methods ====================

    /**
     * Create a test organization
     */
    private SysOrg createOrg(String name) {
        SysOrg org = new SysOrg();
        org.setName(name);
        org.setType(OrgType.FUNCTIONAL_DEPT);
        org.setIsActive(true);
        org.setSortOrder(1);
        org.setCreatedAt(LocalDateTime.now());
        org.setUpdatedAt(LocalDateTime.now());
        return orgRepository.save(org);
    }

    /**
     * Create a parent indicator (to be distributed)
     */
    private Indicator createParentIndicator(String description) {
        Indicator parent = Indicator.builder()
                .taskId(1L)
                .level(IndicatorLevel.PRIMARY)
                .ownerOrg(testOrg1)
                .targetOrg(testOrg1)
                .indicatorDesc(description)
                .weightPercent(new BigDecimal("100.00"))
                .sortOrder(1)
                .type("QUANTITATIVE")
                .progress(0)
                .status(IndicatorStatus.ACTIVE)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return indicatorRepository.save(parent);
    }

    /**
     * Create a child indicator with specified weight
     * This represents the parent indicator's existing child indicators (sub-indicators)
     * whose weights must sum to 100% before distribution can occur
     */
    private Indicator createChildIndicator(Indicator parent, BigDecimal weight) {
        Indicator child = Indicator.builder()
                .taskId(parent.getTaskId())
                .parentIndicatorId(parent.getIndicatorId())
                .level(IndicatorLevel.SECONDARY)
                .ownerOrg(parent.getOwnerOrg())
                .targetOrg(parent.getTargetOrg())
                .indicatorDesc(parent.getIndicatorDesc() + " - Child " + weight + "%")
                .weightPercent(weight)
                .sortOrder(1)
                .type(parent.getType())
                .progress(0)
                .status(IndicatorStatus.ACTIVE)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return indicatorRepository.save(child);
    }
}
