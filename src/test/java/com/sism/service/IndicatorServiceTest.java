package com.sism.service;

import com.sism.dto.IndicatorCreateRequest;
import com.sism.dto.IndicatorUpdateRequest;
import com.sism.entity.Indicator;
import com.sism.entity.Org;
import com.sism.entity.StrategicTask;
import com.sism.enums.IndicatorLevel;
import com.sism.enums.IndicatorStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.OrgRepository;
import com.sism.repository.TaskRepository;
import com.sism.vo.IndicatorVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for IndicatorService
 * Tests core CRUD operations and business logic
 * 
 * Requirements: 4.2 - Service layer unit test coverage
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IndicatorServiceTest {

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private OrgRepository orgRepository;

    private StrategicTask testTask;
    private Org testOwnerOrg;
    private Org testTargetOrg;

    @BeforeEach
    void setUp() {
        // Get existing test data from database
        testTask = taskRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No tasks found in test database"));
        
        List<Org> orgs = orgRepository.findByIsActiveTrue();
        assertThat(orgs).hasSizeGreaterThanOrEqualTo(2);
        testOwnerOrg = orgs.get(0);
        testTargetOrg = orgs.get(1);
    }

    @Nested
    @DisplayName("getIndicatorById Tests")
    class GetIndicatorByIdTests {

        @Test
        @DisplayName("Should return indicator when exists")
        void shouldReturnIndicatorWhenExists() {
            // Given
            Indicator existingIndicator = indicatorRepository.findAll().stream()
                    .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                    .findFirst()
                    .orElseThrow();

            // When
            IndicatorVO result = indicatorService.getIndicatorById(existingIndicator.getIndicatorId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIndicatorId()).isEqualTo(existingIndicator.getIndicatorId());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when indicator not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            Long nonExistentId = 999999L;

            // When/Then
            assertThatThrownBy(() -> indicatorService.getIndicatorById(nonExistentId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Indicator");
        }
    }

    @Nested
    @DisplayName("getAllActiveIndicators Tests")
    class GetAllActiveIndicatorsTests {

        @Test
        @DisplayName("Should return only active indicators")
        void shouldReturnOnlyActiveIndicators() {
            // When
            List<IndicatorVO> result = indicatorService.getAllActiveIndicators();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).allMatch(i -> i.getStatus() == IndicatorStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("getIndicatorsByTaskId Tests")
    class GetIndicatorsByTaskIdTests {

        @Test
        @DisplayName("Should return indicators for given task")
        void shouldReturnIndicatorsForTask() {
            // Given
            Long taskId = testTask.getTaskId();

            // When
            List<IndicatorVO> result = indicatorService.getIndicatorsByTaskId(taskId);

            // Then
            assertThat(result).allMatch(i -> i.getTaskId().equals(taskId));
        }
    }

    @Nested
    @DisplayName("createIndicator Tests")
    class CreateIndicatorTests {

        @Test
        @DisplayName("Should create indicator with valid request")
        void shouldCreateIndicatorWithValidRequest() {
            // Given
            IndicatorCreateRequest request = new IndicatorCreateRequest();
            request.setTaskId(testTask.getTaskId());
            request.setOwnerOrgId(testOwnerOrg.getOrgId());
            request.setTargetOrgId(testTargetOrg.getOrgId());
            request.setLevel(IndicatorLevel.STRAT_TO_FUNC);
            request.setIndicatorDesc("Test Indicator Description");
            request.setWeightPercent(new BigDecimal("25.00"));
            request.setYear(2025);

            // When
            IndicatorVO result = indicatorService.createIndicator(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIndicatorId()).isNotNull();
            assertThat(result.getIndicatorDesc()).isEqualTo("Test Indicator Description");
            assertThat(result.getStatus()).isEqualTo(IndicatorStatus.ACTIVE);
            assertThat(result.getWeightPercent()).isEqualByComparingTo(new BigDecimal("25.00"));
        }

        @Test
        @DisplayName("Should throw exception when task not found")
        void shouldThrowExceptionWhenTaskNotFound() {
            // Given
            IndicatorCreateRequest request = new IndicatorCreateRequest();
            request.setTaskId(999999L);
            request.setOwnerOrgId(testOwnerOrg.getOrgId());
            request.setTargetOrgId(testTargetOrg.getOrgId());
            request.setLevel(IndicatorLevel.STRAT_TO_FUNC);
            request.setIndicatorDesc("Test");

            // When/Then
            assertThatThrownBy(() -> indicatorService.createIndicator(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Strategic Task");
        }

        @Test
        @DisplayName("Should throw exception when owner org not found")
        void shouldThrowExceptionWhenOwnerOrgNotFound() {
            // Given
            IndicatorCreateRequest request = new IndicatorCreateRequest();
            request.setTaskId(testTask.getTaskId());
            request.setOwnerOrgId(999999L);
            request.setTargetOrgId(testTargetOrg.getOrgId());
            request.setLevel(IndicatorLevel.STRAT_TO_FUNC);
            request.setIndicatorDesc("Test");

            // When/Then
            assertThatThrownBy(() -> indicatorService.createIndicator(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Owner Organization");
        }
    }

    @Nested
    @DisplayName("updateIndicator Tests")
    class UpdateIndicatorTests {

        @Test
        @DisplayName("Should update indicator description")
        void shouldUpdateIndicatorDescription() {
            // Given
            Indicator existingIndicator = indicatorRepository.findAll().stream()
                    .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                    .findFirst()
                    .orElseThrow();

            IndicatorUpdateRequest request = new IndicatorUpdateRequest();
            request.setIndicatorDesc("Updated Description");

            // When
            IndicatorVO result = indicatorService.updateIndicator(
                    existingIndicator.getIndicatorId(), request);

            // Then
            assertThat(result.getIndicatorDesc()).isEqualTo("Updated Description");
        }

        @Test
        @DisplayName("Should throw exception when updating archived indicator")
        void shouldThrowExceptionWhenUpdatingArchivedIndicator() {
            // Given - find or create an archived indicator
            Indicator archivedIndicator = indicatorRepository.findAll().stream()
                    .filter(i -> i.getStatus() == IndicatorStatus.ARCHIVED)
                    .findFirst()
                    .orElse(null);

            if (archivedIndicator == null) {
                // Create one for testing
                Indicator activeIndicator = indicatorRepository.findAll().stream()
                        .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                        .findFirst()
                        .orElseThrow();
                activeIndicator.setStatus(IndicatorStatus.ARCHIVED);
                archivedIndicator = indicatorRepository.save(activeIndicator);
            }

            IndicatorUpdateRequest request = new IndicatorUpdateRequest();
            request.setIndicatorDesc("Should fail");

            Long indicatorId = archivedIndicator.getIndicatorId();

            // When/Then
            assertThatThrownBy(() -> indicatorService.updateIndicator(indicatorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("archived");
        }

        @Test
        @DisplayName("Should throw exception when indicator is its own parent")
        void shouldThrowExceptionWhenIndicatorIsOwnParent() {
            // Given
            Indicator existingIndicator = indicatorRepository.findAll().stream()
                    .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                    .findFirst()
                    .orElseThrow();

            IndicatorUpdateRequest request = new IndicatorUpdateRequest();
            request.setParentIndicatorId(existingIndicator.getIndicatorId());

            Long indicatorId = existingIndicator.getIndicatorId();

            // When/Then
            assertThatThrownBy(() -> indicatorService.updateIndicator(indicatorId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("own parent");
        }
    }

    @Nested
    @DisplayName("deleteIndicator Tests")
    class DeleteIndicatorTests {

        @Test
        @DisplayName("Should soft delete indicator by setting status to ARCHIVED")
        void shouldSoftDeleteIndicator() {
            // Given - Create a new indicator to delete
            IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
            createRequest.setTaskId(testTask.getTaskId());
            createRequest.setOwnerOrgId(testOwnerOrg.getOrgId());
            createRequest.setTargetOrgId(testTargetOrg.getOrgId());
            createRequest.setLevel(IndicatorLevel.STRAT_TO_FUNC);
            createRequest.setIndicatorDesc("Indicator to delete");
            createRequest.setWeightPercent(new BigDecimal("10.00"));
            createRequest.setYear(2025);

            IndicatorVO created = indicatorService.createIndicator(createRequest);

            // When
            indicatorService.deleteIndicator(created.getIndicatorId());

            // Then
            Indicator deleted = indicatorRepository.findById(created.getIndicatorId()).orElseThrow();
            assertThat(deleted.getStatus()).isEqualTo(IndicatorStatus.ARCHIVED);
        }

        @Test
        @DisplayName("Should throw exception when deleting already archived indicator")
        void shouldThrowExceptionWhenDeletingArchivedIndicator() {
            // Given
            Indicator archivedIndicator = indicatorRepository.findAll().stream()
                    .filter(i -> i.getStatus() == IndicatorStatus.ARCHIVED)
                    .findFirst()
                    .orElse(null);

            if (archivedIndicator == null) {
                // Create one for testing
                IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
                createRequest.setTaskId(testTask.getTaskId());
                createRequest.setOwnerOrgId(testOwnerOrg.getOrgId());
                createRequest.setTargetOrgId(testTargetOrg.getOrgId());
                createRequest.setLevel(IndicatorLevel.STRAT_TO_FUNC);
                createRequest.setIndicatorDesc("To archive");
                createRequest.setWeightPercent(new BigDecimal("5.00"));
                createRequest.setYear(2025);

                IndicatorVO created = indicatorService.createIndicator(createRequest);
                indicatorService.deleteIndicator(created.getIndicatorId());
                archivedIndicator = indicatorRepository.findById(created.getIndicatorId()).orElseThrow();
            }

            Long indicatorId = archivedIndicator.getIndicatorId();

            // When/Then
            assertThatThrownBy(() -> indicatorService.deleteIndicator(indicatorId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already archived");
        }
    }

    @Nested
    @DisplayName("searchIndicators Tests")
    class SearchIndicatorsTests {

        @Test
        @DisplayName("Should return indicators matching keyword")
        void shouldReturnIndicatorsMatchingKeyword() {
            // Given - Create indicator with specific description
            IndicatorCreateRequest createRequest = new IndicatorCreateRequest();
            createRequest.setTaskId(testTask.getTaskId());
            createRequest.setOwnerOrgId(testOwnerOrg.getOrgId());
            createRequest.setTargetOrgId(testTargetOrg.getOrgId());
            createRequest.setLevel(IndicatorLevel.STRAT_TO_FUNC);
            createRequest.setIndicatorDesc("UniqueSearchKeyword123");
            createRequest.setWeightPercent(new BigDecimal("5.00"));
            createRequest.setYear(2025);

            indicatorService.createIndicator(createRequest);

            // When
            List<IndicatorVO> result = indicatorService.searchIndicators("UniqueSearchKeyword123");

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).anyMatch(i -> i.getIndicatorDesc().contains("UniqueSearchKeyword123"));
        }
    }

    @Nested
    @DisplayName("Distribution Tests")
    class DistributionTests {

        @Test
        @DisplayName("Should check distribution eligibility correctly")
        void shouldCheckDistributionEligibilityCorrectly() {
            // Given
            Indicator stratToFuncIndicator = indicatorRepository.findAll().stream()
                    .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                    .filter(i -> i.getLevel() == IndicatorLevel.STRAT_TO_FUNC)
                    .findFirst()
                    .orElse(null);

            if (stratToFuncIndicator != null) {
                // When
                var eligibility = indicatorService.checkDistributionEligibility(
                        stratToFuncIndicator.getIndicatorId());

                // Then
                assertThat(eligibility.canDistribute()).isTrue();
            }
        }

        @Test
        @DisplayName("Should not allow distribution of FUNC_TO_COLLEGE level indicators")
        void shouldNotAllowDistributionOfFuncToCollegeIndicators() {
            // Given
            Indicator funcToCollegeIndicator = indicatorRepository.findAll().stream()
                    .filter(i -> i.getStatus() == IndicatorStatus.ACTIVE)
                    .filter(i -> i.getLevel() == IndicatorLevel.FUNC_TO_COLLEGE)
                    .findFirst()
                    .orElse(null);

            if (funcToCollegeIndicator != null) {
                // When
                var eligibility = indicatorService.checkDistributionEligibility(
                        funcToCollegeIndicator.getIndicatorId());

                // Then
                assertThat(eligibility.canDistribute()).isFalse();
                assertThat(eligibility.reason()).contains("STRAT_TO_FUNC");
            }
        }
    }
}
