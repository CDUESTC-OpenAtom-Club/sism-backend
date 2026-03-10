package com.sism.service;

import com.sism.entity.Indicator;
import com.sism.entity.SysOrg;
import com.sism.entity.SysUser;
import com.sism.enums.AuditEntityType;
import com.sism.enums.IndicatorStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.IndicatorRepository;
import com.sism.repository.MilestoneRepository;
import com.sism.repository.UserRepository;
import com.sism.vo.IndicatorVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for indicator review workflow methods
 * Tests submitForReview, approveIndicatorReview, and rejectIndicatorReview
 * 
 * Requirements: 2.3, 2.5, 2.6, 2.7, 2.8
 */
@ExtendWith(MockitoExtension.class)
class IndicatorReviewWorkflowTest {

    @Mock
    private IndicatorRepository indicatorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private MilestoneRepository milestoneRepository;

    @InjectMocks
    private IndicatorService indicatorService;

    private Indicator draftIndicator;
    private Indicator pendingReviewIndicator;
    private SysUser strategicUser;
    private SysUser regularUser;
    private SysOrg strategicOrg;
    private SysOrg regularOrg;

    @BeforeEach
    void setUp() {
        // Setup strategic organization
        strategicOrg = new SysOrg();
        strategicOrg.setId(1L);
        strategicOrg.setName("战略发展部");

        // Setup regular organization
        regularOrg = new SysOrg();
        regularOrg.setId(2L);
        regularOrg.setName("计算机学院");

        // Setup strategic user
        strategicUser = new SysUser();
        strategicUser.setId(1L);
        strategicUser.setUsername("strategic_user");
        strategicUser.setOrg(strategicOrg);

        // Setup regular user
        regularUser = new SysUser();
        regularUser.setId(2L);
        regularUser.setUsername("regular_user");
        regularUser.setOrg(regularOrg);

        // Setup draft indicator
        draftIndicator = Indicator.builder()
                .indicatorId(1L)
                .status(IndicatorStatus.DRAFT)
                .indicatorDesc("Test Indicator")
                .weightPercent(new BigDecimal("100"))
                .ownerOrg(strategicOrg)
                .targetOrg(regularOrg)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup pending review indicator
        pendingReviewIndicator = Indicator.builder()
                .indicatorId(2L)
                .status(IndicatorStatus.PENDING_REVIEW)
                .indicatorDesc("Pending Review Indicator")
                .weightPercent(new BigDecimal("100"))
                .ownerOrg(strategicOrg)
                .targetOrg(regularOrg)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== submitForReview Tests ====================

    @Test
    void submitForReview_Success_NoChildren() {
        // Given
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));
        when(indicatorRepository.findByParentIndicatorIdDirect(1L)).thenReturn(new ArrayList<>());
        when(indicatorRepository.save(any(Indicator.class))).thenReturn(draftIndicator);
        when(userRepository.findById(1L)).thenReturn(Optional.of(strategicUser));
        when(milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(1L)).thenReturn(new ArrayList<>());

        // When
        IndicatorVO result = indicatorService.submitForReview(1L, 1L);

        // Then
        assertNotNull(result);
        verify(indicatorRepository).save(argThat(indicator -> 
            indicator.getStatus() == IndicatorStatus.PENDING_REVIEW
        ));
        verify(auditLogService).logUpdate(
            eq(AuditEntityType.INDICATOR),
            eq(1L),
            any(Map.class),
            any(Map.class),
            eq(strategicUser),
            eq(strategicOrg),
            eq("提交指标进行审核")
        );
    }

    @Test
    void submitForReview_Success_WithValidChildren() {
        // Given
        Indicator child1 = Indicator.builder()
                .indicatorId(10L)
                .weightPercent(new BigDecimal("60"))
                .build();
        Indicator child2 = Indicator.builder()
                .indicatorId(11L)
                .weightPercent(new BigDecimal("40"))
                .build();
        List<Indicator> children = List.of(child1, child2);

        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));
        when(indicatorRepository.findByParentIndicatorIdDirect(1L)).thenReturn(children);
        when(indicatorRepository.save(any(Indicator.class))).thenReturn(draftIndicator);
        when(userRepository.findById(1L)).thenReturn(Optional.of(strategicUser));
        when(milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(1L)).thenReturn(new ArrayList<>());

        // When
        IndicatorVO result = indicatorService.submitForReview(1L, 1L);

        // Then
        assertNotNull(result);
        verify(indicatorRepository).save(any(Indicator.class));
    }

    @Test
    void submitForReview_Failure_InvalidStatus() {
        // Given
        pendingReviewIndicator.setStatus(IndicatorStatus.DISTRIBUTED);
        when(indicatorRepository.findById(2L)).thenReturn(Optional.of(pendingReviewIndicator));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            indicatorService.submitForReview(2L, 1L);
        });
        assertEquals("只能提交状态为 DRAFT 的指标进行审核", exception.getMessage());
        verify(indicatorRepository, never()).save(any());
    }

    @Test
    void submitForReview_Failure_InvalidWeightSum() {
        // Given
        Indicator child1 = Indicator.builder()
                .indicatorId(10L)
                .weightPercent(new BigDecimal("60"))
                .build();
        Indicator child2 = Indicator.builder()
                .indicatorId(11L)
                .weightPercent(new BigDecimal("30"))
                .build();
        List<Indicator> children = List.of(child1, child2);

        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));
        when(indicatorRepository.findByParentIndicatorIdDirect(1L)).thenReturn(children);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            indicatorService.submitForReview(1L, 1L);
        });
        assertTrue(exception.getMessage().contains("子指标权重总和必须等于 100%"));
        verify(indicatorRepository, never()).save(any());
    }

    @Test
    void submitForReview_Failure_UserNotFound() {
        // Given
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));
        when(indicatorRepository.findByParentIndicatorIdDirect(1L)).thenReturn(new ArrayList<>());
        when(indicatorRepository.save(any(Indicator.class))).thenReturn(draftIndicator);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            indicatorService.submitForReview(1L, 999L);
        });
        verify(auditLogService, never()).logUpdate(any(), any(), any(), any(), any(), any(), any());
    }

    // ==================== approveIndicatorReview Tests ====================

    @Test
    void approveIndicatorReview_Success() {
        // Given
        when(indicatorRepository.findById(2L)).thenReturn(Optional.of(pendingReviewIndicator));
        when(userRepository.findById(1L)).thenReturn(Optional.of(strategicUser));
        when(indicatorRepository.save(any(Indicator.class))).thenReturn(pendingReviewIndicator);
        when(milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(2L)).thenReturn(new ArrayList<>());

        // When
        IndicatorVO result = indicatorService.approveIndicatorReview(2L, 1L);

        // Then
        assertNotNull(result);
        verify(indicatorRepository).save(argThat(indicator -> 
            indicator.getStatus() == IndicatorStatus.DISTRIBUTED
        ));
        verify(auditLogService).logUpdate(
            eq(AuditEntityType.INDICATOR),
            eq(2L),
            any(Map.class),
            any(Map.class),
            eq(strategicUser),
            eq(strategicOrg),
            eq("审核通过指标定义")
        );
    }

    @Test
    void approveIndicatorReview_Failure_InvalidStatus() {
        // Given
        draftIndicator.setStatus(IndicatorStatus.DRAFT);
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            indicatorService.approveIndicatorReview(1L, 1L);
        });
        assertEquals("只能审核通过状态为 PENDING_REVIEW 的指标", exception.getMessage());
        verify(indicatorRepository, never()).save(any());
    }

    @Test
    void approveIndicatorReview_Failure_NotStrategicUser() {
        // Given
        when(indicatorRepository.findById(2L)).thenReturn(Optional.of(pendingReviewIndicator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            indicatorService.approveIndicatorReview(2L, 2L);
        });
        assertEquals("只有战略发展部用户可以审核通过指标", exception.getMessage());
        verify(indicatorRepository, never()).save(any());
    }

    @Test
    void approveIndicatorReview_Failure_UserHasNoOrganization() {
        // Given
        SysUser userWithoutOrg = new SysUser();
        userWithoutOrg.setId(3L);
        userWithoutOrg.setUsername("no_org_user");
        userWithoutOrg.setOrg(null);

        when(indicatorRepository.findById(2L)).thenReturn(Optional.of(pendingReviewIndicator));
        when(userRepository.findById(3L)).thenReturn(Optional.of(userWithoutOrg));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            indicatorService.approveIndicatorReview(2L, 3L);
        });
        assertEquals("只有战略发展部用户可以审核通过指标", exception.getMessage());
        verify(indicatorRepository, never()).save(any());
    }

    // ==================== rejectIndicatorReview Tests ====================

    @Test
    void rejectIndicatorReview_Success_WithReason() {
        // Given
        String reason = "指标描述不清晰";
        when(indicatorRepository.findById(2L)).thenReturn(Optional.of(pendingReviewIndicator));
        when(userRepository.findById(1L)).thenReturn(Optional.of(strategicUser));
        when(indicatorRepository.save(any(Indicator.class))).thenReturn(pendingReviewIndicator);
        when(milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(2L)).thenReturn(new ArrayList<>());

        // When
        IndicatorVO result = indicatorService.rejectIndicatorReview(2L, reason, 1L);

        // Then
        assertNotNull(result);
        verify(indicatorRepository).save(argThat(indicator -> 
            indicator.getStatus() == IndicatorStatus.DRAFT
        ));
        verify(auditLogService).logUpdate(
            eq(AuditEntityType.INDICATOR),
            eq(2L),
            any(Map.class),
            any(Map.class),
            eq(strategicUser),
            eq(strategicOrg),
            eq("驳回指标定义: " + reason)
        );
    }

    @Test
    void rejectIndicatorReview_Success_WithoutReason() {
        // Given
        when(indicatorRepository.findById(2L)).thenReturn(Optional.of(pendingReviewIndicator));
        when(userRepository.findById(1L)).thenReturn(Optional.of(strategicUser));
        when(indicatorRepository.save(any(Indicator.class))).thenReturn(pendingReviewIndicator);
        when(milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(2L)).thenReturn(new ArrayList<>());

        // When
        IndicatorVO result = indicatorService.rejectIndicatorReview(2L, null, 1L);

        // Then
        assertNotNull(result);
        verify(auditLogService).logUpdate(
            eq(AuditEntityType.INDICATOR),
            eq(2L),
            any(Map.class),
            any(Map.class),
            eq(strategicUser),
            eq(strategicOrg),
            eq("驳回指标定义: 无原因")
        );
    }

    @Test
    void rejectIndicatorReview_Failure_InvalidStatus() {
        // Given
        draftIndicator.setStatus(IndicatorStatus.DRAFT);
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            indicatorService.rejectIndicatorReview(1L, "test reason", 1L);
        });
        assertEquals("只能驳回状态为 PENDING_REVIEW 的指标", exception.getMessage());
        verify(indicatorRepository, never()).save(any());
    }

    @Test
    void rejectIndicatorReview_Failure_NotStrategicUser() {
        // Given
        when(indicatorRepository.findById(2L)).thenReturn(Optional.of(pendingReviewIndicator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            indicatorService.rejectIndicatorReview(2L, "test reason", 2L);
        });
        assertEquals("只有战略发展部用户可以驳回指标", exception.getMessage());
        verify(indicatorRepository, never()).save(any());
    }

    // ==================== State Transition Tests ====================

    @Test
    void testFullReviewWorkflow_SubmitApprove() {
        // Given - Start with DRAFT
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));
        when(indicatorRepository.findByParentIndicatorIdDirect(1L)).thenReturn(new ArrayList<>());
        when(indicatorRepository.save(any(Indicator.class))).thenAnswer(invocation -> {
            Indicator saved = invocation.getArgument(0);
            return saved;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(strategicUser));
        when(milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(1L)).thenReturn(new ArrayList<>());

        // When - Submit for review
        indicatorService.submitForReview(1L, 1L);

        // Then - Verify save was called for submit
        verify(indicatorRepository, atLeastOnce()).save(any(Indicator.class));

        // Given - Now indicator is PENDING_REVIEW
        draftIndicator.setStatus(IndicatorStatus.PENDING_REVIEW);
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));

        // When - Approve review
        indicatorService.approveIndicatorReview(1L, 1L);

        // Then - Verify save was called again and final status is DISTRIBUTED
        verify(indicatorRepository, atLeast(2)).save(any(Indicator.class));
        assertEquals(IndicatorStatus.DISTRIBUTED, draftIndicator.getStatus());
    }

    @Test
    void testFullReviewWorkflow_SubmitReject() {
        // Given - Start with DRAFT
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));
        when(indicatorRepository.findByParentIndicatorIdDirect(1L)).thenReturn(new ArrayList<>());
        when(indicatorRepository.save(any(Indicator.class))).thenAnswer(invocation -> {
            Indicator saved = invocation.getArgument(0);
            return saved;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(strategicUser));
        when(milestoneRepository.findByIndicator_IndicatorIdOrderByDueDateAsc(1L)).thenReturn(new ArrayList<>());

        // When - Submit for review
        indicatorService.submitForReview(1L, 1L);

        // Then - Verify save was called for submit
        verify(indicatorRepository, atLeastOnce()).save(any(Indicator.class));

        // Given - Now indicator is PENDING_REVIEW
        draftIndicator.setStatus(IndicatorStatus.PENDING_REVIEW);
        when(indicatorRepository.findById(1L)).thenReturn(Optional.of(draftIndicator));

        // When - Reject review
        indicatorService.rejectIndicatorReview(1L, "需要修改", 1L);

        // Then - Verify save was called again and final status is DRAFT
        verify(indicatorRepository, atLeast(2)).save(any(Indicator.class));
        assertEquals(IndicatorStatus.DRAFT, draftIndicator.getStatus());
    }
}
