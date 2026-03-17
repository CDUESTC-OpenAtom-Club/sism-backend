package com.sism.strategy.domain;

import com.sism.enums.IndicatorStatus;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.domain.OrgType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Indicator Aggregate Root Tests")
class IndicatorTest {

    private SysOrg ownerOrg;
    private SysOrg targetOrg;

    @BeforeEach
    void setUp() {
        ownerOrg = SysOrg.create("战略发展部", OrgType.admin);
        targetOrg = SysOrg.create("测试学院", OrgType.academic);
    }

    @Test
    @DisplayName("Should create Indicator with valid parameters")
    void shouldCreateIndicatorWithValidParameters() {
        Indicator indicator = Indicator.create("测试指标", "指标描述", BigDecimal.valueOf(100), ownerOrg, targetOrg);

        assertNotNull(indicator);
        assertNotNull(indicator.getDescription());
        assertEquals(BigDecimal.valueOf(100), indicator.getWeight());
        assertEquals(ownerOrg, indicator.getOwnerOrg());
        assertEquals(targetOrg, indicator.getTargetOrg());
        assertEquals(IndicatorStatus.DRAFT, indicator.getStatus());
        assertFalse(indicator.getIsDeleted());
        assertNotNull(indicator.getCreatedAt());
        assertNotNull(indicator.getUpdatedAt());
    }

    @Test
    @DisplayName("Should throw exception when creating Indicator with null name")
    void shouldThrowExceptionWhenCreatingIndicatorWithNullName() {
        assertThrows(IllegalArgumentException.class, () ->
            Indicator.create(null, "指标描述", BigDecimal.valueOf(100), ownerOrg, targetOrg)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating Indicator with negative weight")
    void shouldThrowExceptionWhenCreatingIndicatorWithNegativeWeight() {
        assertThrows(IllegalArgumentException.class, () ->
            Indicator.create("测试指标", "指标描述", BigDecimal.valueOf(-10), ownerOrg, targetOrg)
        );
    }

    @Test
    @DisplayName("Should throw exception when creating Indicator with weight exceeding 100")
    void shouldThrowExceptionWhenCreatingIndicatorWithWeightExceeding100() {
        assertThrows(IllegalArgumentException.class, () ->
            Indicator.create("测试指标", "指标描述", BigDecimal.valueOf(101), ownerOrg, targetOrg)
        );
    }

    @Test
    @DisplayName("Should submit indicator for review successfully")
    void shouldSubmitIndicatorForReviewSuccessfully() {
        Indicator indicator = Indicator.create("测试指标", "指标描述", BigDecimal.valueOf(100), ownerOrg, targetOrg);

        indicator.submitForReview();

        assertEquals(IndicatorStatus.PENDING, indicator.getStatus());
        assertNotNull(indicator.getUpdatedAt());
    }

    @Test
    @DisplayName("Should validate Indicator with valid parameters")
    void shouldValidateIndicatorWithValidParameters() {
        Indicator indicator = Indicator.create("有效指标", "指标描述", BigDecimal.valueOf(100), ownerOrg, targetOrg);

        assertDoesNotThrow(indicator::validate);
    }

    @Test
    @DisplayName("Should validate Indicator with invalid parameters")
    void shouldValidateIndicatorWithInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> {
            Indicator.create(null, null, null, null, null);
        });
    }
}
