package com.sism.service;

import com.sism.dto.AuditFlowCreateRequest;
import com.sism.dto.AuditFlowUpdateRequest;
import com.sism.dto.AuditStepCreateRequest;
import com.sism.entity.AuditFlowDef;
import com.sism.entity.AuditStepDef;
import com.sism.enums.AuditEntityType;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.AuditFlowDefRepository;
import com.sism.repository.AuditStepDefRepository;
import com.sism.vo.AuditFlowVO;
import com.sism.vo.AuditStepVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditFlowService
 */
@ExtendWith(MockitoExtension.class)
class AuditFlowServiceTest {

    @Mock
    private AuditFlowDefRepository auditFlowDefRepository;

    @Mock
    private AuditStepDefRepository auditStepDefRepository;

    @InjectMocks
    private AuditFlowService auditFlowService;

    private AuditFlowDef testAuditFlow;
    private AuditStepDef testAuditStep;

    @BeforeEach
    void setUp() {
        testAuditFlow = new AuditFlowDef();
        testAuditFlow.setId(1L);
        testAuditFlow.setFlowName("Indicator Approval Flow");
        testAuditFlow.setFlowCode("INDICATOR_APPROVAL");
        testAuditFlow.setEntityType(AuditEntityType.INDICATOR);
        testAuditFlow.setDescription("Flow for indicator approval");
        testAuditFlow.setCreatedAt(LocalDateTime.now());
        testAuditFlow.setUpdatedAt(LocalDateTime.now());

        testAuditStep = new AuditStepDef();
        testAuditStep.setId(1L);
        testAuditStep.setFlowId(1L);
        testAuditStep.setStepOrder(1);
        testAuditStep.setStepName("Initial Review");
        testAuditStep.setApproverRole("1");
        testAuditStep.setIsRequired(true);
        testAuditStep.setCreatedAt(LocalDateTime.now());
        testAuditStep.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllAuditFlows_ShouldReturnAllFlows() {
        // Arrange
        AuditFlowDef flow2 = new AuditFlowDef();
        flow2.setId(2L);
        flow2.setFlowName("Task Approval Flow");
        flow2.setFlowCode("TASK_APPROVAL");
        flow2.setEntityType(AuditEntityType.TASK);
        flow2.setCreatedAt(LocalDateTime.now());
        flow2.setUpdatedAt(LocalDateTime.now());

        when(auditFlowDefRepository.findAll()).thenReturn(Arrays.asList(testAuditFlow, flow2));
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(2L)).thenReturn(Collections.emptyList());

        // Act
        List<AuditFlowVO> result = auditFlowService.getAllAuditFlows();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFlowCode()).isEqualTo("INDICATOR_APPROVAL");
        assertThat(result.get(1).getFlowCode()).isEqualTo("TASK_APPROVAL");
        verify(auditFlowDefRepository).findAll();
    }

    @Test
    void getAuditFlowById_WhenExists_ShouldReturnFlow() {
        // Arrange
        when(auditFlowDefRepository.findById(1L)).thenReturn(Optional.of(testAuditFlow));
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));

        // Act
        AuditFlowVO result = auditFlowService.getAuditFlowById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFlowCode()).isEqualTo("INDICATOR_APPROVAL");
        assertThat(result.getSteps()).hasSize(1);
        verify(auditFlowDefRepository).findById(1L);
    }

    @Test
    void getAuditFlowById_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(auditFlowDefRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditFlowService.getAuditFlowById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditFlowDefRepository).findById(999L);
    }

    @Test
    void getAuditFlowByCode_WhenExists_ShouldReturnFlow() {
        // Arrange
        when(auditFlowDefRepository.findByFlowCode("INDICATOR_APPROVAL")).thenReturn(Optional.of(testAuditFlow));
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));

        // Act
        AuditFlowVO result = auditFlowService.getAuditFlowByCode("INDICATOR_APPROVAL");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFlowCode()).isEqualTo("INDICATOR_APPROVAL");
        verify(auditFlowDefRepository).findByFlowCode("INDICATOR_APPROVAL");
    }

    @Test
    void getAuditFlowByCode_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(auditFlowDefRepository.findByFlowCode("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditFlowService.getAuditFlowByCode("INVALID"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditFlowDefRepository).findByFlowCode("INVALID");
    }

    @Test
    void getAuditFlowsByEntityType_ShouldReturnMatchingFlows() {
        // Arrange
        when(auditFlowDefRepository.findByEntityType(AuditEntityType.INDICATOR))
                .thenReturn(Arrays.asList(testAuditFlow));
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));

        // Act
        List<AuditFlowVO> result = auditFlowService.getAuditFlowsByEntityType(AuditEntityType.INDICATOR);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntityType()).isEqualTo(AuditEntityType.INDICATOR);
        verify(auditFlowDefRepository).findByEntityType(AuditEntityType.INDICATOR);
    }

    @Test
    void createAuditFlow_WithValidData_ShouldCreateSuccessfully() {
        // Arrange
        AuditFlowCreateRequest request = new AuditFlowCreateRequest(
                "New Flow",
                "NEW_FLOW",
                AuditEntityType.TASK,
                "New flow description"
        );

        when(auditFlowDefRepository.findByFlowCode("NEW_FLOW")).thenReturn(Optional.empty());
        when(auditFlowDefRepository.save(any(AuditFlowDef.class))).thenReturn(testAuditFlow);
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Collections.emptyList());

        // Act
        AuditFlowVO result = auditFlowService.createAuditFlow(request);

        // Assert
        assertThat(result).isNotNull();
        verify(auditFlowDefRepository).findByFlowCode("NEW_FLOW");
        verify(auditFlowDefRepository).save(any(AuditFlowDef.class));
    }

    @Test
    void createAuditFlow_WithDuplicateCode_ShouldThrowException() {
        // Arrange
        AuditFlowCreateRequest request = new AuditFlowCreateRequest(
                "Duplicate Flow",
                "INDICATOR_APPROVAL",
                AuditEntityType.INDICATOR,
                "Duplicate code"
        );

        when(auditFlowDefRepository.findByFlowCode("INDICATOR_APPROVAL")).thenReturn(Optional.of(testAuditFlow));

        // Act & Assert
        assertThatThrownBy(() -> auditFlowService.createAuditFlow(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(auditFlowDefRepository).findByFlowCode("INDICATOR_APPROVAL");
        verify(auditFlowDefRepository, never()).save(any());
    }

    @Test
    void updateAuditFlow_WithValidData_ShouldUpdateSuccessfully() {
        // Arrange
        AuditFlowUpdateRequest request = new AuditFlowUpdateRequest(
                "Updated Flow Name",
                AuditEntityType.TASK,
                "Updated description"
        );

        when(auditFlowDefRepository.findById(1L)).thenReturn(Optional.of(testAuditFlow));
        when(auditFlowDefRepository.save(any(AuditFlowDef.class))).thenReturn(testAuditFlow);
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));

        // Act
        AuditFlowVO result = auditFlowService.updateAuditFlow(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(auditFlowDefRepository).findById(1L);
        verify(auditFlowDefRepository).save(any(AuditFlowDef.class));
    }

    @Test
    void updateAuditFlow_WithPartialData_ShouldUpdateOnlyProvidedFields() {
        // Arrange
        AuditFlowUpdateRequest request = new AuditFlowUpdateRequest();
        request.setFlowName("Partially Updated");

        when(auditFlowDefRepository.findById(1L)).thenReturn(Optional.of(testAuditFlow));
        when(auditFlowDefRepository.save(any(AuditFlowDef.class))).thenReturn(testAuditFlow);
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));

        // Act
        AuditFlowVO result = auditFlowService.updateAuditFlow(1L, request);

        // Assert
        assertThat(result).isNotNull();
        verify(auditFlowDefRepository).findById(1L);
        verify(auditFlowDefRepository).save(any(AuditFlowDef.class));
    }

    @Test
    void updateAuditFlow_WhenNotExists_ShouldThrowException() {
        // Arrange
        AuditFlowUpdateRequest request = new AuditFlowUpdateRequest();
        when(auditFlowDefRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditFlowService.updateAuditFlow(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditFlowDefRepository).findById(999L);
        verify(auditFlowDefRepository, never()).save(any());
    }

    @Test
    void deleteAuditFlow_WhenExists_ShouldDeleteFlowAndSteps() {
        // Arrange
        when(auditFlowDefRepository.findById(1L)).thenReturn(Optional.of(testAuditFlow));
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));
        doNothing().when(auditStepDefRepository).deleteAll(anyList());
        doNothing().when(auditFlowDefRepository).delete(testAuditFlow);

        // Act
        auditFlowService.deleteAuditFlow(1L);

        // Assert
        verify(auditFlowDefRepository).findById(1L);
        verify(auditStepDefRepository).findByFlowIdOrderByStepOrderAsc(1L);
        verify(auditStepDefRepository).deleteAll(anyList());
        verify(auditFlowDefRepository).delete(testAuditFlow);
    }

    @Test
    void deleteAuditFlow_WhenNotExists_ShouldThrowException() {
        // Arrange
        when(auditFlowDefRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditFlowService.deleteAuditFlow(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditFlowDefRepository).findById(999L);
        verify(auditFlowDefRepository, never()).delete(any());
    }

    @Test
    void addAuditStep_WithValidData_ShouldAddSuccessfully() {
        // Arrange
        AuditStepCreateRequest request = new AuditStepCreateRequest(
                1L,
                2,
                "Second Review",
                2L,
                "Second step description"
        );

        when(auditFlowDefRepository.findById(1L)).thenReturn(Optional.of(testAuditFlow));
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));
        when(auditStepDefRepository.save(any(AuditStepDef.class))).thenReturn(testAuditStep);

        // Act
        AuditStepVO result = auditFlowService.addAuditStep(request);

        // Assert
        assertThat(result).isNotNull();
        verify(auditFlowDefRepository).findById(1L);
        verify(auditStepDefRepository).findByFlowIdOrderByStepOrderAsc(1L);
        verify(auditStepDefRepository).save(any(AuditStepDef.class));
    }

    @Test
    void addAuditStep_WithInvalidFlowId_ShouldThrowException() {
        // Arrange
        AuditStepCreateRequest request = new AuditStepCreateRequest(
                999L,
                1,
                "New Step",
                1L,
                "Description"
        );

        when(auditFlowDefRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> auditFlowService.addAuditStep(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(auditFlowDefRepository).findById(999L);
        verify(auditStepDefRepository, never()).save(any());
    }

    @Test
    void addAuditStep_WithDuplicateStepOrder_ShouldThrowException() {
        // Arrange
        AuditStepCreateRequest request = new AuditStepCreateRequest(
                1L,
                1,
                "Duplicate Order",
                2L,
                "Description"
        );

        when(auditFlowDefRepository.findById(1L)).thenReturn(Optional.of(testAuditFlow));
        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L)).thenReturn(Arrays.asList(testAuditStep));

        // Act & Assert
        assertThatThrownBy(() -> auditFlowService.addAuditStep(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
        verify(auditFlowDefRepository).findById(1L);
        verify(auditStepDefRepository).findByFlowIdOrderByStepOrderAsc(1L);
        verify(auditStepDefRepository, never()).save(any());
    }

    @Test
    void getAuditStepsByFlowId_ShouldReturnStepsInOrder() {
        // Arrange
        AuditStepDef step2 = new AuditStepDef();
        step2.setId(2L);
        step2.setFlowId(1L);
        step2.setStepOrder(2);
        step2.setStepName("Second Review");
        step2.setApproverRole("2");
        step2.setIsRequired(true);
        step2.setCreatedAt(LocalDateTime.now());
        step2.setUpdatedAt(LocalDateTime.now());

        when(auditStepDefRepository.findByFlowIdOrderByStepOrderAsc(1L))
                .thenReturn(Arrays.asList(testAuditStep, step2));

        // Act
        List<AuditStepVO> result = auditFlowService.getAuditStepsByFlowId(1L);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStepOrder()).isEqualTo(1);
        assertThat(result.get(1).getStepOrder()).isEqualTo(2);
        verify(auditStepDefRepository).findByFlowIdOrderByStepOrderAsc(1L);
    }
}
