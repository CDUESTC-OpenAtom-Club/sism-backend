package com.sism.util;

import com.sism.entity.*;
import com.sism.enums.*;
import com.sism.repository.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Factory class for creating test data
 * Provides reusable methods for setting up test entities
 */
@Component
public class TestDataFactory {

    /**
     * Create a test user
     */
    public static SysUser createTestUser(SysUserRepository userRepository, 
                                         SysOrgRepository orgRepository,
                                         String username) {
        // Get or create an organization
        SysOrg org = orgRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    SysOrg newOrg = new SysOrg();
                    newOrg.setName("测试组织");
                    newOrg.setType(OrgType.FUNCTIONAL_DEPT);
                    newOrg.setIsActive(true);
                    newOrg.setSortOrder(1);
                    return orgRepository.save(newOrg);
                });

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash("$2a$10$test"); // BCrypt encoded "test"
        user.setRealName("测试用户");
        user.setOrg(org);
        user.setIsActive(true);
        return userRepository.save(user);
    }

    /**
     * Create a test organization
     */
    public static SysOrg createTestOrg(SysOrgRepository orgRepository, 
                                       String name, 
                                       OrgType type) {
        SysOrg org = new SysOrg();
        org.setName(name);
        org.setType(type);
        org.setIsActive(true);
        org.setSortOrder(1);
        return orgRepository.save(org);
    }

    /**
     * Create a test assessment cycle
     */
    public static AssessmentCycle createTestCycle(AssessmentCycleRepository cycleRepository) {
        AssessmentCycle cycle = new AssessmentCycle();
        cycle.setCycleName("2026年度考核周期");
        cycle.setYear(2026);
        cycle.setStartDate(LocalDate.of(2026, 1, 1));
        cycle.setEndDate(LocalDate.of(2026, 12, 31));
        cycle.setDescription("测试周期");
        return cycleRepository.save(cycle);
    }

    /**
     * Create a test strategic task
     */
    public static StrategicTask createTestTask(TaskRepository taskRepository,
                                               AssessmentCycleRepository cycleRepository,
                                               SysOrgRepository orgRepository) {
        AssessmentCycle cycle = cycleRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> createTestCycle(cycleRepository));

        SysOrg org = orgRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> createTestOrg(orgRepository, "测试组织", OrgType.FUNCTIONAL_DEPT));

        StrategicTask task = StrategicTask.builder()
                .taskName("测试任务")
                .taskDesc("测试任务描述")
                .taskType(TaskType.BASIC)
                .cycleId(cycle.getCycleId())
                .planId(1L) // Default plan ID
                .org(org)
                .createdByOrg(org)
                .sortOrder(1)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return taskRepository.save(task);
    }

    /**
     * Create a test indicator
     */
    public static Indicator createTestIndicator(IndicatorRepository indicatorRepository,
                                                TaskRepository taskRepository,
                                                AssessmentCycleRepository cycleRepository,
                                                SysOrgRepository orgRepository) {
        StrategicTask task = taskRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> createTestTask(taskRepository, cycleRepository, orgRepository));

        SysOrg org = orgRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> createTestOrg(orgRepository, "测试组织", OrgType.FUNCTIONAL_DEPT));

        Indicator indicator = Indicator.builder()
                .taskId(task.getTaskId())
                .ownerOrg(org)
                .targetOrg(org)
                .indicatorDesc("测试指标描述")
                .level(IndicatorLevel.STRAT_TO_FUNC)
                .status(IndicatorStatus.ACTIVE)
                .weightPercent(new java.math.BigDecimal("100"))
                .sortOrder(1)
                .type("QUANTITATIVE")
                .type1("QUANTITATIVE")
                .type2("BASIC")
                .isQualitative(false)
                .isDeleted(false)
                .year(2026)
                .progress(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return indicatorRepository.save(indicator);
    }

    /**
     * Create a test milestone
     */
    public static Milestone createTestMilestone(MilestoneRepository milestoneRepository,
                                                IndicatorRepository indicatorRepository,
                                                TaskRepository taskRepository,
                                                AssessmentCycleRepository cycleRepository,
                                                SysOrgRepository orgRepository) {
        Indicator indicator = indicatorRepository.findAll().stream()
                .filter(i -> !i.getIsDeleted())
                .findFirst()
                .orElseGet(() -> createTestIndicator(indicatorRepository, taskRepository, 
                                                     cycleRepository, orgRepository));

        Milestone milestone = new Milestone();
        milestone.setIndicator(indicator);
        milestone.setMilestoneName("测试里程碑");
        milestone.setMilestoneDesc("测试里程碑描述");
        milestone.setDueDate(LocalDate.now().plusMonths(3));
        milestone.setTargetProgress(50);
        milestone.setStatus(MilestoneStatus.NOT_STARTED);
        milestone.setSortOrder(1);
        milestone.setIsPaired(false);
        return milestoneRepository.save(milestone);
    }

    /**
     * Create a test progress report
     */
    public static ProgressReport createTestReport(ReportRepository reportRepository,
                                                  IndicatorRepository indicatorRepository,
                                                  SysUserRepository userRepository,
                                                  TaskRepository taskRepository,
                                                  AssessmentCycleRepository cycleRepository,
                                                  SysOrgRepository orgRepository) {
        Indicator indicator = indicatorRepository.findAll().stream()
                .filter(i -> !i.getIsDeleted())
                .findFirst()
                .orElseGet(() -> createTestIndicator(indicatorRepository, taskRepository, 
                                                     cycleRepository, orgRepository));

        SysUser user = userRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> createTestUser(userRepository, orgRepository, "test_reporter"));

        ProgressReport report = new ProgressReport();
        report.setIndicator(indicator);
        report.setReporter(user);
        report.setPercentComplete(new java.math.BigDecimal("75"));
        report.setNarrative("测试进度报告");
        report.setStatus(ReportStatus.DRAFT);
        report.setIsFinal(false);
        report.setVersionNo(1);
        report.setAchievedMilestone(false);
        report.setReportedAt(LocalDateTime.now());
        return reportRepository.save(report);
    }
}
