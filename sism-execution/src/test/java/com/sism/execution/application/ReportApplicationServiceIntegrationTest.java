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

    private TestFixture createSubmittedReportWithIndicator(int progress) {
        long sequence = TEST_SEQUENCE.getAndIncrement();
        SysOrg ownerOrg = persistOrg("owner-org-" + sequence, OrgType.functional);
        SysOrg targetOrg = persistOrg("target-org-" + sequence, OrgType.functional);

        Indicator indicator = Indicator.create("indicator-" + sequence, ownerOrg, targetOrg);
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
