package com.sism.property;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Property-based tests for Milestone Weight Sum Validation
 * 
 * **Feature: sism-fullstack-integration, Property 7: Milestone Weight Sum Validation**
 * 
 * NOTE: This test class is disabled because the weight_percent field was removed from Milestone entity.
 * The feature was redesigned to use targetProgress instead of weight percentages.
 * 
 * TODO: Rewrite these tests to validate targetProgress behavior instead of weight sums.
 */
@Disabled("Milestone weight_percent field removed - feature redesigned to use targetProgress")
@SpringBootTest
@ActiveProfiles("test")
public class MilestoneWeightSumPropertyTest {

    @Test
    void placeholder() {
        // This test class is disabled pending redesign for targetProgress feature
    }
}
