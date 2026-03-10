package com.sism.entity;

import com.sism.enums.IndicatorStatus;
import com.sism.enums.ProgressApprovalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test to verify Indicator entity defaults to DRAFT status
 * 
 * **Validates: Requirement 2.4** - New indicators default to DRAFT status
 */
public class IndicatorDefaultStatusTest {

    @Test
    public void indicatorBuilder_shouldDefaultToDraftStatus() {
        // Create indicator using builder without explicitly setting status
        Indicator indicator = Indicator.builder()
                .indicatorDesc("Test Indicator")
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Verify default status is DRAFT (not ACTIVE)
        assertThat(indicator.getStatus())
                .as("New indicators should default to DRAFT status")
                .isEqualTo(IndicatorStatus.DRAFT);
    }

    @Test
    public void indicatorBuilder_shouldAllowExplicitStatusOverride() {
        // Create indicator with explicit status
        Indicator indicator = Indicator.builder()
                .indicatorDesc("Test Indicator")
                .weightPercent(new BigDecimal("100"))
                .sortOrder(1)
                .type("定量")
                .status(IndicatorStatus.DISTRIBUTED)
                .progressApprovalStatus(ProgressApprovalStatus.NONE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Verify explicit status is preserved
        assertThat(indicator.getStatus())
                .as("Explicit status should override default")
                .isEqualTo(IndicatorStatus.DISTRIBUTED);
    }
}
