package com.sism.service;

import com.sism.dto.PlanCreateRequest;
import com.sism.dto.PlanUpdateRequest;
import com.sism.entity.Plan;
import com.sism.entity.SysOrg;
import com.sism.enums.PlanLevel;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.PlanRepository;
import com.sism.repository.SysOrgRepository;
import com.sism.vo.PlanVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlanService
 */
@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SysOrgRepository orgRepository;

    @InjectMocks
    private PlanService planService;

    private Plan testPlan;
    private SysOrg testOrg;

    @BeforeEach
    void setUp() {
        testPlan = Plan.builder()
                .id(1L)
                .cycleId(100L)
                .targetOrgId(200L)
                .createdByOrgId(300L)
                .planLevel(PlanLevel.STRAT_TO_FUNC)
                .status("DRAFT")
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testOrg = new SysOrg();
        testOrg.setId(200L);
        testOrg.setName("Test Organization");
    }

    @Test
    void getAllPlans_ShouldReturnNonDeletedPlans() {
        // Arrange
        Plan deletedPlan = Plan.builder()
                .id(2L)
                .cycleId(100L)
                .targetOrgId(200L)
                .createdByOrgId(300L)
                .planLevel(PlanLevel.FUNC_TO_COLLEGE)
                .status("DRAFT")
                .isDeleted(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(planRepository.findAll()).thenReturn(Arrays.asList(testPlan, deletedPlan));

        // Act
        List<PlanVO> result = planService.getAllPlans();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getIsDeleted()).isFalse();
        verify(planRepository).findAll();
    }

    @Test
    void getPlanById_WhenExists_ShouldReturnPlan() {
        // Arrange
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));

        // Act
        PlanVO result = planService.getPlanById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCycleId()).isEqualTo(100L);
        assertThat(result.getTargetOrgId()).isEqualTo(200L);
        verify(planRepository).findById(1L);
    }

    @Test
    void getPlanById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(planRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.getPlanById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(planRepository).findById(999L);
    }

    @Test
    void getPlanById_WhenDeleted_ShouldThrowException() {
        // Arrange
        testPlan.setIsDeleted(true);
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));

        // Act & Assert
        assertThatThrownBy(() -> planService.getPlanById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(planRepository).findById(1L);
    }

    @Test
    void getPlansByCycleId_ShouldReturnPlansForCycle() {
        // Arrange
        when(planRepository.findByCycleId(100L)).thenReturn(Arrays.asList(testPlan));

        // Act
        List<PlanVO> result = planService.getPlansByCycleId(100L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCycleId()).isEqualTo(100L);
        verify(planRepository).findByCycleId(100L);
    }

    @Test
    void getPlansByTargetOrgId_ShouldReturnPlansForOrg() {
        // Arrange
        when(planRepository.findByTargetOrgId(200L)).thenReturn(Arrays.asList(testPlan));

        // Act
        List<PlanVO> result = planService.getPlansByTargetOrgId(200L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTargetOrgId()).isEqualTo(200L);
        verify(planRepository).findByTargetOrgId(200L);
    }

    @Test
    void createPlan_WithValidData_ShouldCreateSuccessfully() {
        // Arrange
        PlanCreateRequest request = new PlanCreateRequest(
                100L,
                200L,
                300L,
                PlanLevel.STRAT_TO_FUNC,
                "DRAFT"
        );

        when(orgRepository.findById(200L)).thenReturn(Optional.of(testOrg));
        when(orgRepository.findById(300L)).thenReturn(Optional.of(testOrg));
        when(planRepository.save(any(Plan.class))).thenReturn(testPlan);

        // Act
        PlanVO result = planService.createPlan(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCycleId()).isEqualTo(100L);
        verify(orgRepository).findById(200L);
        verify(orgRepository).findById(300L);
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void createPlan_WithInvalidTargetOrg_ShouldThrowException() {
        // Arrange
        PlanCreateRequest request = new PlanCreateRequest(
                100L,
                999L,
                300L,
                PlanLevel.STRAT_TO_FUNC,
                "DRAFT"
        );

        when(orgRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.createPlan(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orgRepository).findById(999L);
        verify(planRepository, never()).save(any());
    }

    @Test
    void createPlan_WithInvalidCreatedByOrg_ShouldThrowException() {
        // Arrange
        PlanCreateRequest request = new PlanCreateRequest(
                100L,
                200L,
                999L,
                PlanLevel.STRAT_TO_FUNC,
                "DRAFT"
        );

        when(orgRepository.findById(200L)).thenReturn(Optional.of(testOrg));
        when(orgRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.createPlan(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(orgRepository).findById(200L);
        verify(orgRepository).findById(999L);
        verify(planRepository, never()).save(any());
    }

    @Test
    void updatePlan_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        PlanUpdateRequest request = new PlanUpdateRequest(
                250L,
                PlanLevel.FUNC_TO_COLLEGE,
                "IN_PROGRESS"
        );

        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(orgRepository.findById(250L)).thenReturn(Optional.of(testOrg));
        when(planRepository.save(any(Plan.class))).thenReturn(testPlan);

        // Act
        PlanVO result = planService.updatePlan(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(planRepository).findById(1L);
        verify(orgRepository).findById(250L);
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void updatePlan_WithPartialData_ShouldUpdateOnlyProvidedFields() {
        // Arrange
        PlanUpdateRequest request = new PlanUpdateRequest();
        request.setStatus("APPROVED");

        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.save(any(Plan.class))).thenReturn(testPlan);

        // Act
        PlanVO result = planService.updatePlan(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(planRepository).findById(1L);
        verify(planRepository).save(any(Plan.class));
        verify(orgRepository, never()).findById(any());
    }

    @Test
    void updatePlan_WhenNotExists_ShouldThrowException() {
        // Arrange
        PlanUpdateRequest request = new PlanUpdateRequest();
        when(planRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.updatePlan(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(planRepository).findById(999L);
        verify(planRepository, never()).save(any());
    }

    @Test
    void deletePlan_WhenExists_ShouldSoftDelete() {
        // Arrange
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.save(any(Plan.class))).thenReturn(testPlan);

        // Act
        planService.deletePlan(1L);

        // Assert
        verify(planRepository).findById(1L);
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void deletePlan_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(planRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.deletePlan(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(planRepository).findById(999L);
        verify(planRepository, never()).save(any());
    }

    @Test
    void approvePlan_WhenExists_ShouldSetStatusToApproved() {
        // Arrange
        when(planRepository.findById(1L)).thenReturn(Optional.of(testPlan));
        when(planRepository.save(any(Plan.class))).thenReturn(testPlan);

        // Act
        PlanVO result = planService.approvePlan(1L);

        // Assert
        assertThat(result).isNotNull();
        verify(planRepository).findById(1L);
        verify(planRepository).save(any(Plan.class));
    }

    @Test
    void approvePlan_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(planRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> planService.approvePlan(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(planRepository).findById(999L);
        verify(planRepository, never()).save(any());
    }
}
