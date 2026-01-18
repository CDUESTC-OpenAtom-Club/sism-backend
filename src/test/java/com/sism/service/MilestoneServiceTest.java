package com.sism.service;

import com.sism.dto.MilestoneCreateRequest;
import com.sism.dto.MilestoneUpdateRequest;
import com.sism.entity.Indicator;
import com.sism.entity.Milestone;
import com.sism.enums.IndicatorStatus;
import com.sism.enums.MilestoneStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.MilestoneRepository;
import com.sism.vo.MilestoneVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MilestoneService
 * Tests core CRUD operations and weight validation logic
 * 
 * Requirements: 4.2 - Service layer unit test coverage
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MilestoneServiceTest {

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    private Indicator testIndicator;

    @BeforeEach
    void setUp() {
        // Get existing active indicator from database
        testIndicator = indicatorRepository.findAll().stream()
                .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active indicators found in test database"));
    }

    @Nested
    @DisplayName("getMilestoneById Tests")
    class GetMilestoneByIdTests {

        @Test
        @DisplayName("Should return milestone when exists")
        void shouldReturnMilestoneWhenExists() {
            // Given
            Milestone existingMilestone = milestoneRepository.findAll().stream()
                    .findFirst()
                    .orElseThrow();

            // When
            MilestoneVO result = milestoneService.getMilestoneById(existingMilestone.getMilestoneId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMilestoneId()).isEqualTo(existingMilestone.getMilestoneId());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when milestone not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            Long nonExistentId = 999999L;

            // When/Then
            assertThatThrownBy(() -> milestoneService.getMilestoneById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Milestone");
        }
    }

    @Nested
    @DisplayName("getMilestonesByIndicatorId Tests")
    class GetMilestonesByIndicatorIdTests {

        @Test
        @DisplayName("Should return milestones for given indicator")
        void shouldReturnMilestonesForIndicator() {
            // Given
            Long indicatorId = testIndicator.getIndicatorId();

            // When
            List<MilestoneVO> result = milestoneService.getMilestonesByIndicatorId(indicatorId);

            // Then
            assertThat(result).allMatch(m -> m.getIndicatorId().equals(indicatorId));
        }
    }

    @Nested
    @DisplayName("getMilestonesByStatus Tests")
    class GetMilestonesByStatusTests {

        @Test
        @DisplayName("Should return milestones with given status")
        void shouldReturnMilestonesWithStatus() {
            // Given
            MilestoneStatus status = MilestoneStatus.NOT_STARTED;

            // When
            List<MilestoneVO> result = milestoneService.getMilestonesByStatus(status);

            // Then
            assertThat(result).allMatch(m -> m.getStatus() == status);
        }
    }

    @Nested
    @DisplayName("createMilestone Tests")
    class CreateMilestoneTests {

        @Test
        @DisplayName("Should create milestone with valid request")
        void shouldCreateMilestoneWithValidRequest() {
            // Given
            String uniqueName = "Test Milestone " + System.currentTimeMillis();
            MilestoneCreateRequest request = new MilestoneCreateRequest();
            request.setIndicatorId(testIndicator.getIndicatorId());
            request.setMilestoneName(uniqueName);
            request.setMilestoneDesc("Test milestone description");
            request.setDueDate(LocalDate.now().plusMonths(3));
            request.setWeightPercent(new BigDecimal("25.00"));
            request.setSortOrder(1);

            // When
            MilestoneVO result = milestoneService.createMilestone(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getMilestoneId()).isNotNull();
            assertThat(result.getMilestoneName()).isEqualTo(uniqueName);
            assertThat(result.getStatus()).isEqualTo(MilestoneStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("Should throw exception when indicator not found")
        void shouldThrowExceptionWhenIndicatorNotFound() {
            // Given
            MilestoneCreateRequest request = new MilestoneCreateRequest();
            request.setIndicatorId(999999L);
            request.setMilestoneName("Test");
            request.setDueDate(LocalDate.now().plusMonths(1));

            // When/Then
            assertThatThrownBy(() -> milestoneService.createMilestone(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Indicator");
        }

        @Test
        @DisplayName("Should throw exception when duplicate milestone name")
        void shouldThrowExceptionWhenDuplicateName() {
            // Given - Create first milestone
            String duplicateName = "Duplicate Milestone " + System.currentTimeMillis();
            MilestoneCreateRequest request1 = new MilestoneCreateRequest();
            request1.setIndicatorId(testIndicator.getIndicatorId());
            request1.setMilestoneName(duplicateName);
            request1.setDueDate(LocalDate.now().plusMonths(1));
            request1.setWeightPercent(new BigDecimal("10.00"));

            milestoneService.createMilestone(request1);

            // Create second milestone with same name
            MilestoneCreateRequest request2 = new MilestoneCreateRequest();
            request2.setIndicatorId(testIndicator.getIndicatorId());
            request2.setMilestoneName(duplicateName);
            request2.setDueDate(LocalDate.now().plusMonths(2));
            request2.setWeightPercent(new BigDecimal("10.00"));

            // When/Then
            assertThatThrownBy(() -> milestoneService.createMilestone(request2))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("updateMilestone Tests")
    class UpdateMilestoneTests {

        @Test
        @DisplayName("Should update milestone description")
        void shouldUpdateMilestoneDescription() {
            // Given - Create a milestone to update
            String uniqueName = "Update Test " + System.currentTimeMillis();
            MilestoneCreateRequest createRequest = new MilestoneCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setMilestoneName(uniqueName);
            createRequest.setMilestoneDesc("Original description");
            createRequest.setDueDate(LocalDate.now().plusMonths(1));
            createRequest.setWeightPercent(new BigDecimal("10.00"));

            MilestoneVO created = milestoneService.createMilestone(createRequest);

            MilestoneUpdateRequest updateRequest = new MilestoneUpdateRequest();
            updateRequest.setMilestoneDesc("Updated description");

            // When
            MilestoneVO result = milestoneService.updateMilestone(
                    created.getMilestoneId(), updateRequest);

            // Then
            assertThat(result.getMilestoneDesc()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("Should update milestone status")
        void shouldUpdateMilestoneStatus() {
            // Given
            String uniqueName = "Status Update Test " + System.currentTimeMillis();
            MilestoneCreateRequest createRequest = new MilestoneCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setMilestoneName(uniqueName);
            createRequest.setDueDate(LocalDate.now().plusMonths(1));
            createRequest.setWeightPercent(new BigDecimal("10.00"));

            MilestoneVO created = milestoneService.createMilestone(createRequest);

            MilestoneUpdateRequest updateRequest = new MilestoneUpdateRequest();
            updateRequest.setStatus(MilestoneStatus.IN_PROGRESS);

            // When
            MilestoneVO result = milestoneService.updateMilestone(
                    created.getMilestoneId(), updateRequest);

            // Then
            assertThat(result.getStatus()).isEqualTo(MilestoneStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("deleteMilestone Tests")
    class DeleteMilestoneTests {

        @Test
        @DisplayName("Should delete milestone")
        void shouldDeleteMilestone() {
            // Given - Create a milestone to delete
            String uniqueName = "Delete Test " + System.currentTimeMillis();
            MilestoneCreateRequest createRequest = new MilestoneCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setMilestoneName(uniqueName);
            createRequest.setDueDate(LocalDate.now().plusMonths(1));
            createRequest.setWeightPercent(new BigDecimal("5.00"));

            MilestoneVO created = milestoneService.createMilestone(createRequest);
            Long milestoneId = created.getMilestoneId();

            // When
            milestoneService.deleteMilestone(milestoneId);

            // Then
            assertThat(milestoneRepository.findById(milestoneId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateMilestoneStatus Tests")
    class UpdateMilestoneStatusTests {

        @Test
        @DisplayName("Should update status to COMPLETED")
        void shouldUpdateStatusToCompleted() {
            // Given
            String uniqueName = "Status Test " + System.currentTimeMillis();
            MilestoneCreateRequest createRequest = new MilestoneCreateRequest();
            createRequest.setIndicatorId(testIndicator.getIndicatorId());
            createRequest.setMilestoneName(uniqueName);
            createRequest.setDueDate(LocalDate.now().plusMonths(1));
            createRequest.setWeightPercent(new BigDecimal("10.00"));

            MilestoneVO created = milestoneService.createMilestone(createRequest);

            // When
            MilestoneVO result = milestoneService.updateMilestoneStatus(
                    created.getMilestoneId(), MilestoneStatus.COMPLETED);

            // Then
            assertThat(result.getStatus()).isEqualTo(MilestoneStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("Weight Validation Tests")
    class WeightValidationTests {

        @Test
        @DisplayName("Should calculate total weight for indicator")
        void shouldCalculateTotalWeight() {
            // Given
            Long indicatorId = testIndicator.getIndicatorId();

            // When
            BigDecimal totalWeight = milestoneService.calculateTotalWeight(indicatorId);

            // Then
            assertThat(totalWeight).isNotNull();
            assertThat(totalWeight).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should validate weight sum")
        void shouldValidateWeightSum() {
            // Given
            Long indicatorId = testIndicator.getIndicatorId();

            // When
            var result = milestoneService.validateWeightSum(indicatorId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.actualSum()).isNotNull();
            assertThat(result.expectedSum()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Should check if weights are complete")
        void shouldCheckIfWeightsComplete() {
            // Given
            Long indicatorId = testIndicator.getIndicatorId();

            // When
            boolean hasCompleteWeights = milestoneService.hasCompleteWeights(indicatorId);

            // Then - just verify it returns a boolean without error
            assertThat(hasCompleteWeights).isIn(true, false);
        }
    }

    @Nested
    @DisplayName("Pairing Mechanism Tests")
    class PairingMechanismTests {

        @Test
        @DisplayName("Should get pairing status for indicator")
        void shouldGetPairingStatus() {
            // Given
            Long indicatorId = testIndicator.getIndicatorId();

            // When
            var pairingStatus = milestoneService.getPairingStatus(indicatorId);

            // Then
            assertThat(pairingStatus).isNotNull();
            assertThat(pairingStatus.totalMilestones()).isGreaterThanOrEqualTo(0);
            assertThat(pairingStatus.pairedCount()).isGreaterThanOrEqualTo(0);
            assertThat(pairingStatus.unpairedCount()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should get unpaired milestones")
        void shouldGetUnpairedMilestones() {
            // Given
            Long indicatorId = testIndicator.getIndicatorId();

            // When
            List<MilestoneVO> unpairedMilestones = milestoneService.getUnpairedMilestones(indicatorId);

            // Then
            assertThat(unpairedMilestones).isNotNull();
        }
    }

    @Nested
    @DisplayName("Overdue and Upcoming Milestones Tests")
    class OverdueAndUpcomingTests {

        @Test
        @DisplayName("Should get overdue milestones")
        void shouldGetOverdueMilestones() {
            // When
            List<MilestoneVO> overdueMilestones = milestoneService.getOverdueMilestones();

            // Then
            assertThat(overdueMilestones).isNotNull();
        }

        @Test
        @DisplayName("Should get upcoming milestones within days")
        void shouldGetUpcomingMilestones() {
            // When
            List<MilestoneVO> upcomingMilestones = milestoneService.getUpcomingMilestones(30);

            // Then
            assertThat(upcomingMilestones).isNotNull();
        }
    }
}
