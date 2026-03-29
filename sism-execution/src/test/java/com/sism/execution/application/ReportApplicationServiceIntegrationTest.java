package com.sism.execution.application;

import com.sism.execution.domain.model.report.PlanReport;
import com.sism.execution.domain.model.report.ReportOrgType;
import com.sism.execution.domain.repository.PlanReportRepository;
import com.sism.execution.infrastructure.ExecutionModuleConfig;
import com.sism.organization.domain.OrgType;
import com.sism.organization.domain.SysOrg;
import com.sism.organization.infrastructure.OrganizationModuleConfig;
import com.sism.shared.infrastructure.event.EventStore;
import com.sism.strategy.domain.Indicator;
import com.sism.strategy.domain.enums.IndicatorStatus;
import com.sism.strategy.domain.repository.IndicatorRepository;
import com.sism.strategy.infrastructure.StrategyModuleConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ReportApplicationServiceIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("报告审批集成测试")
class ReportApplicationServiceIntegrationTest {

    private static final AtomicLong TEST_SEQUENCE = new AtomicLong(1);

    @Autowired
    private ReportApplicationService reportApplicationService;

    @Autowired
    private PlanReportRepository planReportRepository;

    @Autowired
    private IndicatorRepository indicatorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EventStore eventStore;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.audit_instance (
                    id BIGINT PRIMARY KEY,
                    status VARCHAR(32) NOT NULL,
                    started_at TIMESTAMP,
                    created_at TIMESTAMP,
                    updated_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    entity_id BIGINT NOT NULL,
                    entity_type VARCHAR(255),
                    flow_def_id BIGINT,
                    is_deleted BOOLEAN NOT NULL,
                    requester_id BIGINT,
                    requester_org_id BIGINT
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.audit_step_instance (
                    id BIGINT PRIMARY KEY,
                    instance_id BIGINT,
                    step_name VARCHAR(128) NOT NULL,
                    approved_at TIMESTAMP,
                    approver_id BIGINT,
                    comment VARCHAR(255),
                    status VARCHAR(255),
                    step_def_id BIGINT,
                    approver_org_id BIGINT,
                    step_no INTEGER,
                    created_at TIMESTAMP
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS public.plan_report_indicator (
                    id BIGINT PRIMARY KEY,
                    report_id BIGINT NOT NULL,
                    indicator_id BIGINT NOT NULL,
                    progress INTEGER,
                    milestone_note TEXT,
                    comment TEXT,
                    created_at TIMESTAMP,
                    CONSTRAINT uq_report_indicator UNIQUE (report_id, indicator_id)
                )
                """);
    }

    @Test
    void approveReport_shouldUpdateIndicatorProgressInDatabase() {
        TestFixture fixture = createSubmittedReportWithIndicator(35);

        PlanReport approved = reportApplicationService.approveReport(fixture.report().getId(), 9001L);
        flushAndClear();

        Indicator persistedIndicator = indicatorRepository.findById(fixture.indicator().getId()).orElseThrow();
        PlanReport persistedReport = planReportRepository.findById(fixture.report().getId()).orElseThrow();

        assertThat(approved.getStatus()).isEqualTo(PlanReport.STATUS_APPROVED);
        assertThat(persistedReport.getStatus()).isEqualTo(PlanReport.STATUS_APPROVED);
        assertThat(persistedIndicator.getProgress()).isEqualTo(35);
        assertThat(eventStore.findByEventType("PlanReportApprovedEvent")).isNotEmpty();
    }

    @Test
    void markWorkflowApproved_shouldUpdateIndicatorProgressInDatabase() {
        TestFixture fixture = createSubmittedReportWithIndicator(68);

        PlanReport approved = reportApplicationService.markWorkflowApproved(fixture.report().getId(), 9002L);
        flushAndClear();

        Indicator persistedIndicator = indicatorRepository.findById(fixture.indicator().getId()).orElseThrow();
        PlanReport persistedReport = planReportRepository.findById(fixture.report().getId()).orElseThrow();

        assertThat(approved.getStatus()).isEqualTo(PlanReport.STATUS_APPROVED);
        assertThat(persistedReport.getStatus()).isEqualTo(PlanReport.STATUS_APPROVED);
        assertThat(persistedIndicator.getProgress()).isEqualTo(68);
    }

    @Test
    void createReport_shouldPersistCreatedByAndHydrateWorkflowApprovalSnapshot() {
        long sequence = TEST_SEQUENCE.getAndIncrement();
        SysOrg targetOrg = persistOrg("target-created-by-" + sequence, OrgType.functional);

        PlanReport created = reportApplicationService.createReport(
                "202603",
                targetOrg.getId(),
                ReportOrgType.FUNC_DEPT,
                40_000L + sequence,
                8_801L
        );
        flushAndClear();

        PlanReport persistedCreated = planReportRepository.findById(created.getId()).orElseThrow();
        assertThat(persistedCreated.getCreatedBy()).isEqualTo(8_801L);

        jdbcTemplate.update(
                """
                INSERT INTO public.audit_instance
                    (id, status, started_at, created_at, updated_at, completed_at, entity_id, entity_type, flow_def_id, is_deleted, requester_id, requester_org_id)
                VALUES (?, 'APPROVED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, 'PLAN_REPORT', 1, FALSE, ?, ?)
                """,
                90_000L + sequence,
                created.getId(),
                8_801L,
                targetOrg.getId()
        );
        jdbcTemplate.update(
                """
                INSERT INTO public.audit_step_instance
                    (id, instance_id, step_name, approved_at, approver_id, comment, status, step_def_id, approver_org_id, step_no, created_at)
                VALUES (?, ?, '部门审批', CURRENT_TIMESTAMP, ?, '同意', 'APPROVED', 1, ?, 2, CURRENT_TIMESTAMP)
                """,
                91_000L + sequence,
                90_000L + sequence,
                9_901L,
                targetOrg.getId()
        );

        reportApplicationService.attachAuditInstance(created.getId(), 90_000L + sequence);
        flushAndClear();

        PlanReport hydrated = reportApplicationService.findReportById(created.getId()).orElseThrow();

        assertThat(hydrated.getSubmittedBy()).isEqualTo(8_801L);
        assertThat(hydrated.getApprovedBy()).isEqualTo(9_901L);
        assertThat(hydrated.getApprovedAt()).isNotNull();
    }

    @Test
    void findReportById_shouldHydrateIndicatorDraftDetails() {
        TestFixture fixture = createSubmittedReportWithIndicator(15);

        PlanReport hydrated = reportApplicationService.findReportById(fixture.report().getId()).orElseThrow();

        assertThat(hydrated.getIndicatorDetails()).hasSize(1);
        assertThat(hydrated.getIndicatorDetails().get(0).indicatorId()).isEqualTo(fixture.indicator().getId());
        assertThat(hydrated.getIndicatorDetails().get(0).progress()).isEqualTo(15);
        assertThat(hydrated.getIndicatorDetails().get(0).comment()).startsWith("填报说明-");
    }

    private TestFixture createSubmittedReportWithIndicator(int progress) {
        long sequence = TEST_SEQUENCE.getAndIncrement();
        SysOrg ownerOrg = persistOrg("owner-org-" + sequence, OrgType.functional);
        SysOrg targetOrg = persistOrg("target-org-" + sequence, OrgType.functional);

        Indicator indicator = Indicator.create("indicator-" + sequence, ownerOrg, targetOrg, "定量");
        indicator.setStatus(IndicatorStatus.DISTRIBUTED);
        indicator = indicatorRepository.save(indicator);

        PlanReport report = PlanReport.createDraft("202603", targetOrg.getId(), ReportOrgType.FUNC_DEPT, 30_000L + sequence);
        report.validate();
        report = planReportRepository.save(report);
        report.submit(8000L + sequence);
        report.clearEvents();
        report = planReportRepository.save(report);

        jdbcTemplate.update(
                """
                INSERT INTO public.plan_report_indicator
                    (id, report_id, indicator_id, progress, milestone_note, comment, created_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                70_000L + sequence,
                report.getId(),
                indicator.getId(),
                progress,
                "里程碑-" + sequence,
                "填报说明-" + sequence
        );

        flushAndClear();
        return new TestFixture(report, indicator);
    }

    private SysOrg persistOrg(String name, OrgType type) {
        SysOrg org = SysOrg.create(name, type);
        entityManager.persist(org);
        return org;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private record TestFixture(PlanReport report, Indicator indicator) {
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({ExecutionModuleConfig.class, StrategyModuleConfig.class, OrganizationModuleConfig.class})
    @ComponentScan(basePackages = {
            "com.sism.execution.application",
            "com.sism.execution.infrastructure.persistence",
            "com.sism.shared.infrastructure.event",
            "com.sism.strategy.infrastructure.persistence",
            "com.sism.organization.infrastructure.persistence"
    })
    static class TestConfig {
    }
}
