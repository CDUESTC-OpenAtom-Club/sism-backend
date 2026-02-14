package com.sism.service;

import com.sism.dto.ReportCreateRequest;
import com.sism.dto.ReportUpdateRequest;
import com.sism.entity.*;
import com.sism.enums.ReportStatus;
import com.sism.exception.BusinessException;
import com.sism.exception.ResourceNotFoundException;
import com.sism.repository.*;
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
 * Service for progress report management
 * Provides CRUD operations with status workflow management
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final IndicatorRepository indicatorRepository;
    private final MilestoneRepository milestoneRepository;
    private final AdhocTaskRepository adhocTaskRepository;
    private final UserRepository userRepository;

    /**
     * Flag to enable/disable catch-up rule enforcement
     * When true, users must report on milestones in chronological order
     */
    private static final boolean ENFORCE_CATCHUP_RULE = true;


    /**
     * Get report by ID
     * 
     * @param reportId report ID
     * @return report VO
     * @throws ResourceNotFoundException if report not found
     */
    public ReportVO getReportById(Long reportId) {
        ProgressReport report = findReportById(reportId);
        return toReportVO(report);
    }

    /**
     * Get reports by indicator ID
     * Requirements: 3.5 - View report history for an indicator
     * 
     * @param indicatorId indicator ID
     * @return list of reports for the indicator
     */
    public List<ReportVO> getReportsByIndicatorId(Long indicatorId) {
        return reportRepository.findByIndicator_IndicatorId(indicatorId).stream()
                .map(this::toReportVO)
                .collect(Collectors.toList());
    }

    /**
     * Get reports by indicator ID with pagination
     * 
     * @param indicatorId indicator ID
     * @param pageable pagination info
     * @return page of reports
     */
    public Page<ReportVO> getReportsByIndicatorId(Long indicatorId, Pageable pageable) {
        return reportRepository.findByIndicator_IndicatorId(indicatorId, pageable)
                .map(this::toReportVO);
    }

    /**
     * Get reports by status
     * 
     * @param status report status
     * @return list of reports with the status
     */
    public List<ReportVO> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatus(status).stream()
                .map(this::toReportVO)
                .collect(Collectors.toList());
    }

    /**
     * Get reports by status with pagination
     * 
     * @param status report status
     * @param pageable pagination info
     * @return page of reports
     */
    public Page<ReportVO> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable)
                .map(this::toReportVO);
    }

    /**
     * Get reports by reporter ID
     * 
     * @param reporterId reporter user ID
     * @return list of reports by the reporter
     */
    public List<ReportVO> getReportsByReporterId(Long reporterId) {
        return reportRepository.findByReporter_Id(reporterId).stream()
                .map(this::toReportVO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new progress report
     * Requirements: 3.1 - Create report in DRAFT status
     * Requirements: 3.4 - Validate milestone and adhocTask mutual exclusion
     * Catch-up Rule: Must report on earliest unpaired milestone first
     * 
     * @param request report creation request
     * @return created report VO
     */
    @Transactional
    public ReportVO createReport(ReportCreateRequest request) {
        // Validate mutual exclusion constraint
        validateMutualExclusion(request.getMilestoneId(), request.getAdhocTaskId());

        // Validate indicator exists
        Indicator indicator = indicatorRepository.findById(request.getIndicatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Indicator", request.getIndicatorId()));

        // Validate reporter exists
        SysUser reporter = userRepository.findById(request.getReporterId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getReporterId()));

        // Validate milestone if provided
        Milestone milestone = null;
        if (request.getMilestoneId() != null) {
            milestone = milestoneRepository.findById(request.getMilestoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Milestone", request.getMilestoneId()));
            // Verify milestone belongs to the indicator
            if (!milestone.getIndicator().getIndicatorId().equals(request.getIndicatorId())) {
                throw new BusinessException("Milestone does not belong to the specified indicator");
            }
            
            // Enforce catch-up rule: must report on earliest unpaired milestone
            if (ENFORCE_CATCHUP_RULE) {
                validateCatchupRule(request.getIndicatorId(), request.getMilestoneId());
            }
        }

        // Validate adhoc task if provided
        AdhocTask adhocTask = null;
        if (request.getAdhocTaskId() != null) {
            adhocTask = adhocTaskRepository.findById(request.getAdhocTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("AdhocTask", request.getAdhocTaskId()));
        }

        ProgressReport report = new ProgressReport();
        report.setIndicator(indicator);
        report.setMilestone(milestone);
        report.setAdhocTask(adhocTask);
        report.setPercentComplete(request.getPercentComplete());
        report.setAchievedMilestone(request.getAchievedMilestone() != null ? request.getAchievedMilestone() : false);
        report.setNarrative(request.getNarrative());
        report.setReporter(reporter);
        report.setStatus(ReportStatus.DRAFT);
        report.setIsFinal(false);
        report.setVersionNo(1);

        ProgressReport savedReport = reportRepository.save(report);
        reportRepository.flush(); // 强制立即持久化到数据库
        log.info("Created progress report {} in DRAFT status", savedReport.getReportId());

        return toReportVO(savedReport);
    }

    /**
     * Validate catch-up rule: user must report on the earliest unpaired milestone
     * Exception: If the milestone already has an approved report, allow creating
     * additional reports for revision purposes.
     * 
     * @param indicatorId indicator ID
     * @param milestoneId milestone ID being reported on
     * @throws BusinessException if trying to skip an earlier unpaired milestone
     */
    private void validateCatchupRule(Long indicatorId, Long milestoneId) {
        // Check if the milestone already has an approved report (revision scenario)
        boolean hasApprovedReport = reportRepository
                .findByMilestone_MilestoneIdAndStatus(milestoneId, ReportStatus.APPROVED)
                .stream()
                .findAny()
                .isPresent();
        
        // If the milestone already has an approved report, allow creating additional reports
        // This supports the revision workflow where multiple reports can be approved for the same milestone
        if (hasApprovedReport) {
            return;
        }
        
        // Otherwise, enforce the catch-up rule
        milestoneRepository.findFirstUnpairedMilestone(indicatorId).ifPresent(firstUnpaired -> {
            if (!firstUnpaired.getMilestoneId().equals(milestoneId)) {
                throw new BusinessException(
                    String.format("补录规则：必须先填报 '%s' (截止日期: %s)，才能填报后续里程碑",
                        firstUnpaired.getMilestoneName(),
                        firstUnpaired.getDueDate())
                );
            }
        });
    }


    /**
     * Update an existing progress report
     * Requirements: 3.1 - Update report (only DRAFT and RETURNED status)
     * Requirements: 3.4 - Validate milestone and adhocTask mutual exclusion
     * 
     * @param reportId report ID
     * @param request report update request
     * @return updated report VO
     */
    @Transactional
    public ReportVO updateReport(Long reportId, ReportUpdateRequest request) {
        ProgressReport report = findReportById(reportId);

        // Check if report can be updated (only DRAFT and RETURNED status)
        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.RETURNED) {
            throw new BusinessException("Report can only be updated in DRAFT or RETURNED status");
        }

        // Determine the milestone and adhocTask IDs for validation
        Long milestoneId = request.getMilestoneId() != null ? request.getMilestoneId() : 
                (report.getMilestone() != null ? report.getMilestone().getMilestoneId() : null);
        Long adhocTaskId = request.getAdhocTaskId() != null ? request.getAdhocTaskId() :
                (report.getAdhocTask() != null ? report.getAdhocTask().getAdhocTaskId() : null);

        // Handle explicit null setting
        if (request.getMilestoneId() != null && request.getMilestoneId().equals(0L)) {
            milestoneId = null;
        }
        if (request.getAdhocTaskId() != null && request.getAdhocTaskId().equals(0L)) {
            adhocTaskId = null;
        }

        // Validate mutual exclusion constraint
        validateMutualExclusion(milestoneId, adhocTaskId);

        // Update milestone if provided
        if (request.getMilestoneId() != null) {
            if (request.getMilestoneId().equals(0L)) {
                report.setMilestone(null);
            } else {
                Milestone milestone = milestoneRepository.findById(request.getMilestoneId())
                        .orElseThrow(() -> new ResourceNotFoundException("Milestone", request.getMilestoneId()));
                // Verify milestone belongs to the indicator
                if (!milestone.getIndicator().getIndicatorId().equals(report.getIndicator().getIndicatorId())) {
                    throw new BusinessException("Milestone does not belong to the report's indicator");
                }
                report.setMilestone(milestone);
            }
        }

        // Update adhoc task if provided
        if (request.getAdhocTaskId() != null) {
            if (request.getAdhocTaskId().equals(0L)) {
                report.setAdhocTask(null);
            } else {
                AdhocTask adhocTask = adhocTaskRepository.findById(request.getAdhocTaskId())
                        .orElseThrow(() -> new ResourceNotFoundException("AdhocTask", request.getAdhocTaskId()));
                report.setAdhocTask(adhocTask);
            }
        }

        if (request.getPercentComplete() != null) {
            report.setPercentComplete(request.getPercentComplete());
        }
        if (request.getAchievedMilestone() != null) {
            report.setAchievedMilestone(request.getAchievedMilestone());
        }
        if (request.getNarrative() != null) {
            report.setNarrative(request.getNarrative());
        }

        ProgressReport updatedReport = reportRepository.save(report);
        reportRepository.flush(); // 强制立即持久化到数据库
        log.info("Updated progress report {}", updatedReport.getReportId());

        return toReportVO(updatedReport);
    }

    /**
     * Submit a progress report
     * Requirements: 3.2 - Submit report (DRAFT → SUBMITTED)
     * 
     * @param reportId report ID
     * @return submitted report VO
     */
    @Transactional
    public ReportVO submitReport(Long reportId) {
        ProgressReport report = findReportById(reportId);

        // Check if report can be submitted (only DRAFT status)
        if (report.getStatus() != ReportStatus.DRAFT && report.getStatus() != ReportStatus.RETURNED) {
            throw new BusinessException("Report can only be submitted from DRAFT or RETURNED status");
        }

        report.setStatus(ReportStatus.SUBMITTED);
        report.setReportedAt(LocalDateTime.now());

        ProgressReport submittedReport = reportRepository.save(report);
        reportRepository.flush(); // 强制立即持久化到数据库
        log.info("Submitted progress report {} (DRAFT/RETURNED → SUBMITTED)", submittedReport.getReportId());

        return toReportVO(submittedReport);
    }

    /**
     * Withdraw a submitted progress report
     * Requirements: 3.3 - Withdraw report (SUBMITTED → DRAFT)
     * 
     * @param reportId report ID
     * @return withdrawn report VO
     */
    @Transactional
    public ReportVO withdrawReport(Long reportId) {
        ProgressReport report = findReportById(reportId);

        // Check if report can be withdrawn (only SUBMITTED status)
        if (report.getStatus() != ReportStatus.SUBMITTED) {
            throw new BusinessException("Report can only be withdrawn from SUBMITTED status");
        }

        report.setStatus(ReportStatus.DRAFT);
        report.setReportedAt(null);

        ProgressReport withdrawnReport = reportRepository.save(report);
        reportRepository.flush(); // 强制立即持久化到数据库
        log.info("Withdrawn progress report {} (SUBMITTED → DRAFT)", withdrawnReport.getReportId());

        return toReportVO(withdrawnReport);
    }


    /**
     * Validate mutual exclusion constraint
     * Requirements: 3.4 - Milestone and adhocTask cannot both be non-null
     * 
     * @param milestoneId milestone ID
     * @param adhocTaskId adhoc task ID
     * @throws BusinessException if both are non-null
     */
    public void validateMutualExclusion(Long milestoneId, Long adhocTaskId) {
        if (milestoneId != null && adhocTaskId != null) {
            throw new BusinessException("Milestone and adhoc task cannot be associated simultaneously");
        }
    }

    /**
     * Find report entity by ID
     * 
     * @param reportId report ID
     * @return report entity
     * @throws ResourceNotFoundException if report not found
     */
    public ProgressReport findReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ProgressReport", reportId));
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
}
