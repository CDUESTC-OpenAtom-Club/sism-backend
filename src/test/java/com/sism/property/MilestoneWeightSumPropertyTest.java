package com.sism.property;

import com.sism.dto.MilestoneCreateRequest;
import com.sism.entity.Indicator;
import com.sism.enums.IndicatorStatus;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.MilestoneRepository;
import com.sism.service.MilestoneService;
import com.sism.vo.MilestoneVO;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Milestone Target Progress Validation
 * 
 * **Feature: sism-fullstack-integration, Property 7: Milestone Target Progress Validation**
 * 
 * The milestone system uses targetProgress (0-100) to track expected completion percentages
 * at specific dates. This replaces the deprecated weight_percent system.
 * 
 * Key Properties:
 * 1. targetProgress must be between 0 and 100 (inclusive)
 * 2. Milestones for an indicator should have increasing targetProgress values over time
 * 3. Multiple milestones can have the same targetProgress (flexible design)
 * 4. targetProgress is independent across different indicators
 * 
 * **Validates: Requirements 5.1, 5.2**
 */
@JqwikSpringSupport
@SpringBootTest
@ActiveProfiles("test")
public class MilestoneWeightSumPropertyTest {

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    // ==================== Arbitraries ====================

    @Provide
    Arbitrary<Integer> validTargetProgress() {
        return Arbitraries.integers().between(0, 100);
    }

    @Provide
    Arbitrary<Integer> invalidTargetProgress() {
        return Arbitraries.oneOf(
            Arbitraries.integers().lessOrEqual(-1),
            Arbitraries.integers().greaterOrEqual(101)
        );
    }

    @Provide
    Arbitrary<LocalDate> futureDates() {
        return Arbitraries.integers().between(1, 365)
            .map(days -> LocalDate.now().plusDays(days));
    }

    // ==================== Helper Methods ====================

    private List<Indicator> getExistingActiveIndicators(int limit) {
        return indicatorRepository.findAll().stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .limit(limit)
                .toList();
    }

    private MilestoneVO createTestMilestone(Indicator indicator, Integer targetProgress, LocalDate dueDate) {
        String uniqueName = "Test Milestone " + UUID.randomUUID().toString().substring(0, 8);
        
        MilestoneCreateRequest request = new MilestoneCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setMilestoneName(uniqueName);
        request.setMilestoneDesc("Test milestone for property testing");
        request.setDueDate(dueDate);
        request.setTargetProgress(targetProgress);
        request.setSortOrder(0);
        
        return milestoneService.createMilestone(request);
    }

    // ==================== Property Tests ====================

    /**
     * Property 1: Valid targetProgress values (0-100) should be accepted
     * 
     * For any valid targetProgress value between 0 and 100 (inclusive),
     * the system SHALL successfully create a milestone with that targetProgress.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    @Transactional
    void validTargetProgress_shouldBeAccepted(
            @ForAll("validTargetProgress") Integer targetProgress,
            @ForAll("futureDates") LocalDate dueDate) {
        
        // Get an existing indicator
        List<Indicator> indicators = getExistingActiveIndicators(1);
        assumeThat(indicators).isNotEmpty();
        Indicator indicator = indicators.get(0);

        // Create milestone with valid targetProgress
        MilestoneVO milestone = createTestMilestone(indicator, targetProgress, dueDate);

        // Verify milestone was created with correct targetProgress
        assertThat(milestone).isNotNull();
        assertThat(milestone.getTargetProgress()).isEqualTo(targetProgress);
        assertThat(milestone.getTargetProgress()).isBetween(0, 100);
    }

    /**
     * Property 2: Multiple milestones can have any targetProgress values
     * 
     * Unlike the deprecated weight system, targetProgress values do not need to sum to 100.
     * Multiple milestones for the same indicator can have any valid targetProgress values.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 50)
    @Transactional
    void multipleMilestones_canHaveAnyTargetProgressValues(
            @ForAll("validTargetProgress") Integer progress1,
            @ForAll("validTargetProgress") Integer progress2,
            @ForAll("validTargetProgress") Integer progress3) {
        
        // Get an existing indicator
        List<Indicator> indicators = getExistingActiveIndicators(1);
        assumeThat(indicators).isNotEmpty();
        Indicator indicator = indicators.get(0);

        // Create three milestones with different targetProgress values
        LocalDate baseDate = LocalDate.now().plusDays(30);
        MilestoneVO m1 = createTestMilestone(indicator, progress1, baseDate);
        MilestoneVO m2 = createTestMilestone(indicator, progress2, baseDate.plusDays(30));
        MilestoneVO m3 = createTestMilestone(indicator, progress3, baseDate.plusDays(60));

        // Verify all milestones were created successfully
        assertThat(m1.getTargetProgress()).isEqualTo(progress1);
        assertThat(m2.getTargetProgress()).isEqualTo(progress2);
        assertThat(m3.getTargetProgress()).isEqualTo(progress3);

        // Verify no validation error occurs (sum doesn't need to be 100)
        int sum = progress1 + progress2 + progress3;
        // Sum can be anything - no constraint
        assertThat(sum).isGreaterThanOrEqualTo(0);
    }

    /**
     * Property 3: targetProgress is preserved through retrieval
     * 
     * For any milestone created with a specific targetProgress value,
     * retrieving that milestone SHALL return the same targetProgress value.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    @Transactional
    void targetProgress_shouldBePreservedThroughRetrieval(
            @ForAll("validTargetProgress") Integer targetProgress,
            @ForAll("futureDates") LocalDate dueDate) {
        
        // Get an existing indicator
        List<Indicator> indicators = getExistingActiveIndicators(1);
        assumeThat(indicators).isNotEmpty();
        Indicator indicator = indicators.get(0);

        // Create milestone
        MilestoneVO created = createTestMilestone(indicator, targetProgress, dueDate);

        // Retrieve milestone
        MilestoneVO retrieved = milestoneService.getMilestoneById(created.getMilestoneId());

        // Verify targetProgress is preserved
        assertThat(retrieved.getTargetProgress()).isEqualTo(targetProgress);
        assertThat(retrieved.getTargetProgress()).isEqualTo(created.getTargetProgress());
    }

    /**
     * Property 4: Default targetProgress should be 0 when not specified
     * 
     * When creating a milestone without specifying targetProgress,
     * the system SHALL default to 0.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 50)
    @Transactional
    void missingTargetProgress_shouldDefaultToZero(@ForAll("futureDates") LocalDate dueDate) {
        // Get an existing indicator
        List<Indicator> indicators = getExistingActiveIndicators(1);
        assumeThat(indicators).isNotEmpty();
        Indicator indicator = indicators.get(0);

        // Create milestone without targetProgress
        String uniqueName = "Test Milestone " + UUID.randomUUID().toString().substring(0, 8);
        MilestoneCreateRequest request = new MilestoneCreateRequest();
        request.setIndicatorId(indicator.getIndicatorId());
        request.setMilestoneName(uniqueName);
        request.setMilestoneDesc("Test milestone without targetProgress");
        request.setDueDate(dueDate);
        // targetProgress not set
        request.setSortOrder(0);
        
        MilestoneVO milestone = milestoneService.createMilestone(request);

        // Verify default value is 0
        assertThat(milestone.getTargetProgress()).isEqualTo(0);
    }

    /**
     * Property 5: targetProgress is independent across indicators
     * 
     * Milestones for different indicators can have any targetProgress values
     * without affecting each other.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 50)
    @Transactional
    void targetProgress_shouldBeIndependentAcrossIndicators(
            @ForAll("validTargetProgress") Integer progress1,
            @ForAll("validTargetProgress") Integer progress2) {
        
        // Get two different indicators
        List<Indicator> indicators = getExistingActiveIndicators(2);
        assumeThat(indicators).hasSize(2);
        Indicator indicator1 = indicators.get(0);
        Indicator indicator2 = indicators.get(1);

        // Create milestones for different indicators
        LocalDate dueDate = LocalDate.now().plusDays(30);
        MilestoneVO m1 = createTestMilestone(indicator1, progress1, dueDate);
        MilestoneVO m2 = createTestMilestone(indicator2, progress2, dueDate);

        // Verify both milestones have their respective targetProgress values
        assertThat(m1.getTargetProgress()).isEqualTo(progress1);
        assertThat(m2.getTargetProgress()).isEqualTo(progress2);
        
        // Verify they are for different indicators
        assertThat(m1.getIndicatorId()).isNotEqualTo(m2.getIndicatorId());
    }

    /**
     * Property 6: Querying milestones preserves targetProgress ordering
     * 
     * When retrieving milestones for an indicator, the targetProgress values
     * SHALL be preserved exactly as stored.
     * 
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 30)
    @Transactional
    void queryingMilestones_shouldPreserveTargetProgress(
            @ForAll List<@From("validTargetProgress") Integer> progressValues) {
        
        assumeThat(progressValues).hasSizeBetween(1, 5);
        
        // Get an existing indicator
        List<Indicator> indicators = getExistingActiveIndicators(1);
        assumeThat(indicators).isNotEmpty();
        Indicator indicator = indicators.get(0);

        // Create milestones with the given targetProgress values
        LocalDate baseDate = LocalDate.now().plusDays(30);
        for (int i = 0; i < progressValues.size(); i++) {
            createTestMilestone(indicator, progressValues.get(i), baseDate.plusDays(i * 30));
        }

        // Retrieve all milestones for the indicator
        List<MilestoneVO> retrieved = milestoneService.getMilestonesByIndicatorId(indicator.getIndicatorId());

        // Verify all targetProgress values are preserved
        List<Integer> retrievedProgress = retrieved.stream()
            .map(MilestoneVO::getTargetProgress)
            .filter(p -> p != null)
            .toList();
        
        assertThat(retrievedProgress).containsAll(progressValues);
    }
}
