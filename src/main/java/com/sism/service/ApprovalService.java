package com.sism.service;

import com.sism.dto.ApprovalRequest;
import com.sism.entity.*;
import com.sism.enums.ApprovalAction;
import com.sism.enums.MilestoneStatus;
import com.sism.enums.ReportStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.*;
import com.sism.vo.ApprovalRecordVO;
import com.sism.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for approval workflow management
 * Provides approval operations with milestone status update and final version management
 * 
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final ReportRepository reportRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final MilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final AuditInstanceService auditInstanceService;
    private final PlanRepository planRepository;


    /**
     * Get pending approval reports (SUBMITTED status)
     * Requirements: 4.1 - View pending approval list
     * 
     * @return list of reports awaiting approval
     */
    public List<ReportVO> getPendingApprovalReports() {
        return reportRepository.findByStatus(ReportStatus.SUBMITTED).stream()
                .map(this::toReportVO)
                .collect(Collectors.toList());
    }

    /**
     * Get pending approval reports with pagination
     * Requirements: 4.1 - View pending approval list
     * 
     * @param pageable pagination info
     * @return page of reports awaiting approval
     */
    public Page<ReportVO> getPendingApprovalReports(Pageable pageable) {
        return reportRepository.findByStatus(ReportStatus.SUBMITTED, pageable)
                .map(this::toReportVO);
    }

    /**
     * Get approval records by report ID
     * 
     * @param reportId report ID
     * @return list of approval records for the report
     */
    public List<ApprovalRecordVO> getApprovalRecordsByReportId(Long reportId) {
        return approvalRecordRepository.findByReport_ReportIdOrderByActedAtDesc(reportId).stream()
                .map(this::toApprovalRecordVO)
                .collect(Collectors.toList());
    }

    /**
     * Process approval action
     * Requirements: 4.2, 4.3, 4.4 - Approve, reject, or return report
     * 
     * @param request approval request
     * @return updated report VO
     */
    @Transactional
        public ReportVO processApproval(ApprovalRequest request, Long approverId) {
            ProgressReport report = reportRepository.findById(request.getReportId())
                    .orElseThrow(() -> new ResourceNotFoundException("ProgressReport", request.getReportId()));

            // Validate report is in SUBMITTED status
            if (report.getStatus() != ReportStatus.SUBMITTED) {
                throw new BusinessException("Report must be in SUBMITTED status for approval");
            }

            // Validate approver exists
            SysUser approver = userRepository.findById(approverId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", approverId));

            // Validate comment is provided for REJECT and RETURN actions
            if ((request.getAction() == ApprovalAction.REJECT || request.getAction() == ApprovalAction.RETURN)
                    && (request.getComment() == null || request.getComment().trim().isEmpty())) {
                throw new BusinessException("Comment is required for reject and return actions");
            }

            // Process based on action type
            switch (request.getAction()) {
                case APPROVE:
                    return approveReport(report, approver, request.getComment());
                case REJECT:
                    return rejectReport(report, approver, request.getComment());
                case RETURN:
                    return returnReport(report, approver, request.getComment());
                default:
                    throw new BusinessException("Unknown approval action: " + request.getAction());
            }
        }

    /**
     * Approve a report
     * Requirements: 4.2 - Approve report (SUBMITTED → APPROVED, set isFinal=true)
     * Requirements: 4.5 - Update milestone status to COMPLETED if achievedMilestone=true
     * Requirements: 4.6 - Manage final version (only one final per milestone)
     * 
     * @param report report to approve
     * @param approver approver user
     * @param comment approval comment
     * @return approved report VO
     */
    @Transactional
    public ReportVO approveReport(ProgressReport report, SysUser approver, String comment) {
        Long milestoneId = report.getMilestone() != null ? report.getMilestone().getMilestoneId() : null;
        
        // Handle final version management FIRST (Requirements: 4.6)
        // Clear isFinal flag for all existing approved reports of the same milestone
        // Using @Modifying query to ensure database-level update that bypasses Hibernate cache
        if (milestoneId != null) {
            int updatedCount = reportRepository.clearAllFinalFlagsForMilestone(milestoneId);
            if (updatedCount > 0) {
                log.info("Cleared isFinal flag for {} existing approved report(s) of milestone {}", 
                        updatedCount, milestoneId);
            }
        }

        // Update report status and set as final
        report.setStatus(ReportStatus.APPROVED);
        report.setIsFinal(true);
        ProgressReport savedReport = reportRepository.saveAndFlush(report);

        // Update milestone status if achieved (Requirements: 4.5)
        if (milestoneId != null && Boolean.TRUE.equals(report.getAchievedMilestone())) {
            updateMilestoneStatusToCompleted(milestoneId);
        }

        // Create approval record
        createApprovalRecord(savedReport, approver, ApprovalAction.APPROVE, comment);

        log.info("Approved progress report {} (SUBMITTED → APPROVED)", savedReport.getReportId());
        return toReportVO(savedReport);
    }


    /**
     * Reject a report
     * Requirements: 4.3 - Reject report (SUBMITTED → REJECTED)
     * 
     * @param report report to reject
     * @param approver approver user
     * @param comment rejection reason
     * @return rejected report VO
     */
    @Transactional
    public ReportVO rejectReport(ProgressReport report, SysUser approver, String comment) {
        report.setStatus(ReportStatus.REJECTED);
        ProgressReport savedReport = reportRepository.save(report);

        // Create approval record
        createApprovalRecord(savedReport, approver, ApprovalAction.REJECT, comment);

        log.info("Rejected progress report {} (SUBMITTED → REJECTED)", savedReport.getReportId());
        return toReportVO(savedReport);
    }

    /**
     * Return a report for modification
     * Requirements: 4.4 - Return report (SUBMITTED → RETURNED)
     * 
     * @param report report to return
     * @param approver approver user
     * @param comment return reason
     * @return returned report VO
     */
    @Transactional
    public ReportVO returnReport(ProgressReport report, SysUser approver, String comment) {
        report.setStatus(ReportStatus.RETURNED);
        ProgressReport savedReport = reportRepository.save(report);

        // Create approval record
        createApprovalRecord(savedReport, approver, ApprovalAction.RETURN, comment);

        log.info("Returned progress report {} (SUBMITTED → RETURNED)", savedReport.getReportId());
        return toReportVO(savedReport);
    }

    /**
     * Update final version for milestone
     * Requirements: 4.6 - Only one final version per milestone
     * Sets isFinal=false for all other approved reports of the same milestone
     * 
     * @param milestoneId milestone ID
     * @param currentReportId current report ID (to exclude from update)
     */
    @Transactional
    public void updateFinalVersionForMilestone(Long milestoneId, Long currentReportId) {
        int updatedCount = reportRepository.clearFinalFlagForMilestone(milestoneId, currentReportId);
        if (updatedCount > 0) {
            log.info("Cleared isFinal flag for {} report(s) of milestone {} (excluding report {})", 
                    updatedCount, milestoneId, currentReportId);
        }
    }

    /**
     * Update milestone status to COMPLETED
     * Requirements: 4.5 - Update milestone status when report is approved with achievedMilestone=true
     * 
     * @param milestoneId milestone ID
     */
    @Transactional
    public void updateMilestoneStatusToCompleted(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone", milestoneId));
        
        milestone.setStatus(MilestoneStatus.COMPLETED);
        milestoneRepository.save(milestone);
        log.info("Updated milestone {} status to COMPLETED", milestoneId);
    }

    /**
     * Create approval record
     * 
     * @param report report being approved
     * @param approver approver user
     * @param action approval action
     * @param comment approval comment
     */
    private void createApprovalRecord(ProgressReport report, SysUser approver, 
                                       ApprovalAction action, String comment) {
        ApprovalRecord record = new ApprovalRecord();
        record.setReport(report);
        record.setApprover(approver);
        record.setAction(action);
        record.setComment(comment);
        record.setActedAt(LocalDateTime.now());
        approvalRecordRepository.save(record);
    }


    /**
     * Convert ProgressReport entity to ReportVO
     */
    private ReportVO toReportVO(ProgressReport report) {
        ReportVO vo = new ReportVO();
        vo.setReportId(report.getReportId());
        vo.setIndicatorId(report.getIndicator().getIndicatorId());
        vo.setIndicatorDesc(report.getIndicator().getIndicatorDesc());
        
        if (report.getMilestone() != null) {
            vo.setMilestoneId(report.getMilestone().getMilestoneId());
            vo.setMilestoneName(report.getMilestone().getMilestoneName());
        }
        
        if (report.getAdhocTask() != null) {
            vo.setAdhocTaskId(report.getAdhocTask().getAdhocTaskId());
            vo.setAdhocTaskTitle(report.getAdhocTask().getTaskTitle());
        }
        
        vo.setPercentComplete(report.getPercentComplete());
        vo.setAchievedMilestone(report.getAchievedMilestone());
        vo.setNarrative(report.getNarrative());
        vo.setReporterId(report.getReporter().getId());
        vo.setReporterName(report.getReporter().getRealName());
        vo.setReporterOrgId(report.getReporter().getOrg().getId());
        vo.setReporterOrgName(report.getReporter().getOrg().getName());
        vo.setStatus(report.getStatus());
        vo.setIsFinal(report.getIsFinal());
        vo.setVersionNo(report.getVersionNo());
        vo.setReportedAt(report.getReportedAt());
        vo.setCreatedAt(report.getCreatedAt());
        vo.setUpdatedAt(report.getUpdatedAt());
        
        return vo;
    }

    /**
     * Convert ApprovalRecord entity to ApprovalRecordVO
     */
    private ApprovalRecordVO toApprovalRecordVO(ApprovalRecord record) {
        ApprovalRecordVO vo = new ApprovalRecordVO();
        vo.setApprovalId(record.getApprovalId());
        vo.setReportId(record.getReport().getReportId());
        vo.setApproverId(record.getApprover().getId());
        vo.setApproverName(record.getApprover().getRealName());
        vo.setApproverOrgId(record.getApprover().getOrg().getId());
        vo.setApproverOrgName(record.getApprover().getOrg().getName());
        vo.setAction(record.getAction());
        vo.setComment(record.getComment());
        vo.setActedAt(record.getActedAt());
        return vo;
    }

    // ==================== Plan Approval Workflow Methods ====================

    /**
     * Approve a plan at the current approval step
     * 
     * @param instanceId The audit instance ID
     * @param approverId The approver's user ID
     * @param comment Optional approval comment
     */
    @Transactional
    public void approvePlan(Long instanceId, Long approverId, String comment) {
        log.info("Approving plan: instanceId={}, approverId={}", instanceId, approverId);

        // Process approval through audit instance service
        AuditInstance instance = auditInstanceService.approve(instanceId, approverId, comment);

        // If workflow is completed, update plan status to APPROVED
        if ("APPROVED".equals(instance.getStatus())) {
            updatePlanStatus(instance.getEntityId(), "APPROVED");
            log.info("Plan {} approved and workflow completed", instance.getEntityId());
        } else {
            log.info("Plan {} approval in progress", instance.getEntityId());
        }
    }

    /**
     * Reject a plan at the current approval step
     * 
     * @param instanceId The audit instance ID
     * @param approverId The approver's user ID
     * @param comment Rejection reason (required)
     */
    @Transactional
    public void rejectPlan(Long instanceId, Long approverId, String comment) {
        log.info("Rejecting plan: instanceId={}, approverId={}", instanceId, approverId);

        // Process rejection through audit instance service
        AuditInstance instance = auditInstanceService.reject(instanceId, approverId, comment);

        // Update plan status to REJECTED
        updatePlanStatus(instance.getEntityId(), "REJECTED");
        log.info("Plan {} rejected", instance.getEntityId());
    }

    /**
     * Get pending plan approvals for a user
     * 
     * @param userId The user ID
     * @return List of audit instances pending approval
     */
    public List<AuditInstance> getPendingPlanApprovalsForUser(Long userId) {
        return auditInstanceService.getPendingApprovalsForUser(userId);
    }

    /**
     * Get approval status for a plan
     * 
     * @param planId The plan ID
     * @return The active audit instance, or empty if none exists
     */
    public java.util.Optional<AuditInstance> getPlanApprovalStatus(Long planId) {
        return auditInstanceService.getActiveInstanceByEntity(
                com.sism.enums.AuditEntityType.PLAN, planId);
    }

    /**
     * Count pending plan approvals for a user
     * 
     * @param userId The user ID
     * @return Count of pending approvals
     */
    public long countPendingPlanApprovalsForUser(Long userId) {
        return auditInstanceService.countPendingApprovalsForUser(userId);
    }

    /**
     * Update plan status
     * 
     * @param planId The plan ID
     * @param status The new status
     */
    private void updatePlanStatus(Long planId, String status) {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", planId));
        
        plan.setStatus(status);
        plan.setUpdatedAt(LocalDateTime.now());
        planRepository.save(plan);
        
        log.info("Updated plan {} status to {}", planId, status);
    }
}