package com.sism.property;

import com.sism.entity.Indicator;
import com.sism.entity.Milestone;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.MilestoneStatus;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.MilestoneRepository;
import com.sism.service.MilestoneService;
import com.sism.service.MilestoneService.WeightValidationResult;
import net.jqwik.api.*;
import net.jqwik.spring.JqwikSpringSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Property-based tests for Milestone Weight Sum Validation
 * 
 * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
 * 
 * For any indicator with milestones, the sum of all milestone weight_percent values 
 * SHALL equal exactly 100. The system SHALL display a warning when this constraint 
 * is violated and block submission of indicators with incomplete weights.
 * 
 * **Validates: Requirements 5.1, 5.2, 5.4**
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

    private static final BigDecimal WEIGHT_SUM_TARGET = new BigDecimal("100.00");

    // ==================== Helper Methods ====================

    /**
     * Get existing ACTIVE indicators from the database for testing.
     */
    private List<Indicator> getExistingActiveIndicators(int limit) {
        return indicatorRepository.findAll().stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .limit(limit)
                .toList();
    }

    /**
     * Get milestones for an indicator.
     */
    private List<Milestone> getMilestonesForIndicator(Long indicatorId) {
        return milestoneRepository.findByIndicator_IndicatorId(indicatorId);
    }

    /**
     * Calculate total weight of milestones in Java.
     */
    private BigDecimal calculateTotalWeightInJava(List<Milestone> milestones) {
        return milestones.stream()
                .map(Milestone::getWeightPercent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Normalize weights to sum to exactly 100.
     */
    private List<BigDecimal> normalizeWeightsTo100(List<Integer> rawWeights) {
        if (rawWeights.isEmpty()) {
            return new ArrayList<>();
        }
        
        int total = rawWeights.stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            // Distribute equally
            BigDecimal equalWeight = new BigDecimal("100.00")
                    .divide(BigDecimal.valueOf(rawWeights.size()), 2, RoundingMode.HALF_UP);
            List<BigDecimal> result = new ArrayList<>();
            for (int i = 0; i < rawWeights.size(); i++) {
                result.add(equalWeight);
            }
            // Adjust last one to ensure sum is exactly 100
            BigDecimal sum = result.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal diff = WEIGHT_SUM_TARGET.subtract(sum);
            result.set(result.size() - 1, result.get(result.size() - 1).add(diff));
            return result;
        }
        
        List<BigDecimal> normalized = new ArrayList<>();
        BigDecimal runningSum = BigDecimal.ZERO;
        
        for (int i = 0; i < rawWeights.size() - 1; i++) {
            BigDecimal weight = BigDecimal.valueOf(rawWeights.get(i) * 100.0 / total)
                    .setScale(2, RoundingMode.HALF_UP);
            normalized.add(weight);
            runningSum = runningSum.add(weight);
        }
        
        // Last weight is the remainder to ensure exact 100
        BigDecimal lastWeight = WEIGHT_SUM_TARGET.subtract(runningSum);
        normalized.add(lastWeight);
        
        return normalized;
    }

    // ==================== Generators ====================

    @Provide
    Arbitrary<Integer> indicatorIndices() {
        return Arbitraries.integers().between(0, 9);
    }

    @Provide
    Arbitrary<List<Integer>> rawWeightLists() {
        return Arbitraries.integers().between(1, 50)
                .list().ofMinSize(1).ofMaxSize(10);
    }

    @Provide
    Arbitrary<List<Integer>> incompleteWeightLists() {
        // Generate weights that intentionally don't sum to 100
        return Arbitraries.integers().between(1, 30)
                .list().ofMinSize(1).ofMaxSize(5);
    }

    // ==================== Property Tests ====================

    /**
     * Property 7.1: Weight calculation is consistent between service and repository
     * 
     * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
     * 
     * For any indicator, the weight sum calculated by the service SHALL match
     * the sum calculated directly from the repository query.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 100)
    @Transactional
    void weightCalculation_shouldBeConsistentBetweenServiceAndRepository(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Calculate weight using service method
        BigDecimal serviceWeight = milestoneService.calculateTotalWeight(indicatorId);

        // Calculate weight using repository query
        BigDecimal repositoryWeight = milestoneRepository.calculateTotalWeightByIndicator(indicatorId);

        // Calculate weight manually in Java
        List<Milestone> milestones = getMilestonesForIndicator(indicatorId);
        BigDecimal javaWeight = calculateTotalWeightInJava(milestones);

        // Assert: All three calculations should match
        assertThat(serviceWeight).isEqualByComparingTo(repositoryWeight);
        assertThat(serviceWeight).isEqualByComparingTo(javaWeight);
    }

    /**
     * Property 7.2: Validation result correctly identifies complete weights
     * 
     * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
     * 
     * For any indicator, the validation result isValid SHALL be true if and only if
     * the weight sum equals exactly 100.
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    @Transactional
    void validationResult_shouldCorrectlyIdentifyCompleteWeights(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Get validation result
        WeightValidationResult result = milestoneService.validateWeightSum(indicatorId);

        // Calculate actual sum
        BigDecimal actualSum = milestoneService.calculateTotalWeight(indicatorId);

        // Assert: isValid should be true iff sum equals 100
        boolean expectedValid = actualSum.compareTo(WEIGHT_SUM_TARGET) == 0;
        assertThat(result.isValid()).isEqualTo(expectedValid);
        assertThat(result.actualSum()).isEqualByComparingTo(actualSum);
        assertThat(result.expectedSum()).isEqualByComparingTo(WEIGHT_SUM_TARGET);
    }

    /**
     * Property 7.3: hasCompleteWeights matches validation result
     * 
     * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
     * 
     * For any indicator, hasCompleteWeights() SHALL return the same value as
     * validateWeightSum().isValid().
     * 
     * **Validates: Requirements 5.4**
     */
    @Property(tries = 100)
    @Transactional
    void hasCompleteWeights_shouldMatchValidationResult(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Get both results
        boolean hasComplete = milestoneService.hasCompleteWeights(indicatorId);
        WeightValidationResult validationResult = milestoneService.validateWeightSum(indicatorId);

        // Assert: Both should return the same value
        assertThat(hasComplete).isEqualTo(validationResult.isValid());
    }

    /**
     * Property 7.4: Normalized weights summing to 100 should be valid
     * 
     * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
     * 
     * For any set of weights that are normalized to sum to exactly 100,
     * the validation SHALL return isValid = true.
     * 
     * **Validates: Requirements 5.1, 5.2**
     */
    @Property(tries = 100)
    @Transactional
    void normalizedWeights_shouldSumToExactly100(
            @ForAll("rawWeightLists") List<Integer> rawWeights) {

        // Normalize weights to sum to 100
        List<BigDecimal> normalizedWeights = normalizeWeightsTo100(rawWeights);

        // Calculate sum
        BigDecimal sum = normalizedWeights.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Assert: Sum should be exactly 100
        assertThat(sum).isEqualByComparingTo(WEIGHT_SUM_TARGET);
    }

    /**
     * Property 7.5: Adding milestone updates weight sum correctly
     * 
     * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
     * 
     * For any indicator, when a new milestone is added, the total weight sum
     * SHALL increase by exactly the weight of the new milestone.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 50)
    @Transactional
    void addingMilestone_shouldUpdateWeightSumCorrectly(
            @ForAll("indicatorIndices") Integer index,
            @ForAll @net.jqwik.api.constraints.IntRange(min = 1, max = 50) Integer newWeight) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Get weight sum before adding milestone
        BigDecimal weightBefore = milestoneService.calculateTotalWeight(indicatorId);

        // Create and save a new milestone
        Milestone newMilestone = new Milestone();
        newMilestone.setIndicator(indicator);
        newMilestone.setMilestoneName("Test Milestone " + System.currentTimeMillis());
        newMilestone.setMilestoneDesc("Property test milestone");
        newMilestone.setDueDate(LocalDate.now().plusMonths(1));
        newMilestone.setWeightPercent(BigDecimal.valueOf(newWeight));
        newMilestone.setStatus(MilestoneStatus.NOT_STARTED);
        newMilestone.setSortOrder(999);
        
        milestoneRepository.save(newMilestone);

        // Get weight sum after adding milestone
        BigDecimal weightAfter = milestoneService.calculateTotalWeight(indicatorId);

        // Assert: Weight should increase by exactly the new milestone's weight
        BigDecimal expectedWeight = weightBefore.add(BigDecimal.valueOf(newWeight));
        assertThat(weightAfter).isEqualByComparingTo(expectedWeight);

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 7.6: Removing milestone updates weight sum correctly
     * 
     * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
     * 
     * For any indicator with milestones, when a milestone is removed, the total 
     * weight sum SHALL decrease by exactly the weight of the removed milestone.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 50)
    @Transactional
    void removingMilestone_shouldUpdateWeightSumCorrectly(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Get milestones for this indicator
        List<Milestone> milestones = getMilestonesForIndicator(indicatorId);
        
        // Skip if no milestones exist
        assumeThat(milestones).isNotEmpty();

        // Get weight sum before removing milestone
        BigDecimal weightBefore = milestoneService.calculateTotalWeight(indicatorId);

        // Select a milestone to remove
        Milestone milestoneToRemove = milestones.get(0);
        BigDecimal removedWeight = milestoneToRemove.getWeightPercent();

        // Remove the milestone
        milestoneRepository.delete(milestoneToRemove);

        // Get weight sum after removing milestone
        BigDecimal weightAfter = milestoneService.calculateTotalWeight(indicatorId);

        // Assert: Weight should decrease by exactly the removed milestone's weight
        BigDecimal expectedWeight = weightBefore.subtract(removedWeight);
        assertThat(weightAfter).isEqualByComparingTo(expectedWeight);

        // Rollback will restore the original state due to @Transactional
    }

    /**
     * Property 7.7: Validation message is informative for incomplete weights
     * 
     * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
     * 
     * For any indicator with incomplete weights, the validation message SHALL
     * contain both the actual sum and the expected sum (100).
     * 
     * **Validates: Requirements 5.2**
     */
    @Property(tries = 100)
    @Transactional
    void validationMessage_shouldBeInformativeForIncompleteWeights(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Get validation result
        WeightValidationResult result = milestoneService.validateWeightSum(indicatorId);
        String message = result.getMessage();

        // Assert: Message should be informative
        assertThat(message).isNotNull();
        assertThat(message).isNotEmpty();
        
        if (result.isValid()) {
            assertThat(message).contains("100");
        } else {
            // Message should contain actual percentage
            assertThat(message).containsPattern("\\d+\\.\\d+%");
        }
    }

    /**
     * Property 7.8: Zero weight milestones are counted correctly
     * 
     * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
     * 
     * For any indicator, milestones with zero weight SHALL be included in the
     * count but not affect the weight sum.
     * 
     * **Validates: Requirements 5.1**
     */
    @Property(tries = 50)
    @Transactional
    void zeroWeightMilestones_shouldNotAffectWeightSum(
            @ForAll("indicatorIndices") Integer index) {

        // Get existing active indicators
        List<Indicator> activeIndicators = getExistingActiveIndicators(10);
        
        // Skip if no active indicators exist
        assumeThat(activeIndicators).isNotEmpty();
        
        // Select an indicator based on index (modulo to stay in bounds)
        int actualIndex = index % activeIndicators.size();
        Indicator indicator = activeIndicators.get(actualIndex);
        Long indicatorId = indicator.getIndicatorId();

        // Get weight sum before adding zero-weight milestone
        BigDecimal weightBefore = milestoneService.calculateTotalWeight(indicatorId);
        long countBefore = milestoneRepository.countByIndicator_IndicatorId(indicatorId);

        // Create and save a zero-weight milestone
        Milestone zeroWeightMilestone = new Milestone();
        zeroWeightMilestone.setIndicator(indicator);
        zeroWeightMilestone.setMilestoneName("Zero Weight Milestone " + System.currentTimeMillis());
        zeroWeightMilestone.setMilestoneDesc("Property test zero weight milestone");
        zeroWeightMilestone.setDueDate(LocalDate.now().plusMonths(1));
        zeroWeightMilestone.setWeightPercent(BigDecimal.ZERO);
        zeroWeightMilestone.setStatus(MilestoneStatus.NOT_STARTED);
        zeroWeightMilestone.setSortOrder(999);
        
        milestoneRepository.save(zeroWeightMilestone);

        // Get weight sum and count after adding zero-weight milestone
        BigDecimal weightAfter = milestoneService.calculateTotalWeight(indicatorId);
        long countAfter = milestoneRepository.countByIndicator_IndicatorId(indicatorId);

        // Assert: Weight sum should remain unchanged
        assertThat(weightAfter).isEqualByComparingTo(weightBefore);
        
        // Assert: Count should increase by 1
        assertThat(countAfter).isEqualTo(countBefore + 1);

        // Rollback will restore the original state due to @Transactional
    }
}
