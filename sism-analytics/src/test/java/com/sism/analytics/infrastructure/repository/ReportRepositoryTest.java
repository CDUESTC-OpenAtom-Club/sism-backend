package com.sism.analytics.infrastructure.repository;

import com.sism.analytics.domain.Report;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = com.sism.analytics.TestApplication.class)
@DisplayName("ReportRepository 集成测试")
class ReportRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    @DisplayName("should persist report failure message after save and reload")
    void shouldPersistReportFailureMessageAfterSaveAndReload() {
        Report report = Report.create(
                "失败报告",
                Report.TYPE_STRATEGIC,
                Report.FORMAT_PDF,
                1L,
                "{\"year\":2026}",
                "回归测试"
        );
        report.fail("生成失败");

        Report saved = reportRepository.save(report);
        entityManager.flush();
        entityManager.clear();

        Report reloaded = reportRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(Report.STATUS_FAILED);
        assertThat(reloaded.getErrorMessage()).isEqualTo("生成失败");
    }
}
