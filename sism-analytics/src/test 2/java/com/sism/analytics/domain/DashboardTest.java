package com.sism.analytics.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Dashboard Aggregate Root Tests")
class DashboardTest {

    @Test
    @DisplayName("Should create Dashboard with valid parameters")
    void shouldCreateDashboardWithValidParameters() {
        Dashboard dashboard = Dashboard.create(
                "战略监控仪表板",
                "战略执行情况实时监控",
                1L,
                false,
                "{\"widgets\":[{\"type\":\"chart\",\"config\":{}}]}"
        );

        assertNotNull(dashboard);
        assertEquals("战略监控仪表板", dashboard.getName());
        assertEquals("战略执行情况实时监控", dashboard.getDescription());
        assertEquals(1L, dashboard.getUserId());
        assertFalse(dashboard.isPublic());
        assertNotNull(dashboard.getCreatedAt());
    }

    @Test
    @DisplayName("Should create public Dashboard")
    void shouldCreatePublicDashboard() {
        Dashboard dashboard = Dashboard.create(
                "公共仪表板",
                "公开可见的数据分析仪表板",
                1L,
                true,
                "{\"widgets\":[]}"
        );

        assertTrue(dashboard.isPublic());
    }

    @Test
    @DisplayName("Should throw exception when creating Dashboard with null parameters")
    void shouldThrowExceptionWhenCreatingDashboardWithNullParameters() {
        assertThrows(IllegalArgumentException.class, () ->
                Dashboard.create(null, "描述", 1L, false, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
                Dashboard.create("仪表板名称", "描述", null, false, null)
        );
    }

    @Test
    @DisplayName("Should update Dashboard information")
    void shouldUpdateDashboardInformation() {
        Dashboard dashboard = Dashboard.create(
                "原始仪表板",
                "原始描述",
                1L,
                false,
                "{}"
        );

        dashboard.update(
                "更新后的仪表板",
                "更新后的描述",
                true,
                "{\"widgets\":[{\"type\":\"table\",\"config\":{}}]}"
        );

        assertEquals("更新后的仪表板", dashboard.getName());
        assertEquals("更新后的描述", dashboard.getDescription());
        assertTrue(dashboard.isPublic());
        assertEquals("{\"widgets\":[{\"type\":\"table\",\"config\":{}}]}", dashboard.getConfig());
        assertNotNull(dashboard.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update Dashboard configuration")
    void shouldUpdateDashboardConfiguration() {
        Dashboard dashboard = Dashboard.create(
                "配置更新测试",
                "配置更新描述",
                1L,
                false,
                "{}"
        );

        String newConfig = "{\"widgets\":[{\"type\":\"chart\",\"config\":{\"dataSource\":\"strategic\"}}]}";
        dashboard.updateConfig(newConfig);

        assertEquals(newConfig, dashboard.getConfig());
        assertNotNull(dashboard.getUpdatedAt());
    }

    @Test
    @DisplayName("Should toggle Dashboard visibility")
    void shouldToggleDashboardVisibility() {
        Dashboard dashboard = Dashboard.create(
                "可见性测试",
                "可见性描述",
                1L,
                false,
                "{}"
        );

        dashboard.makePublic();
        assertTrue(dashboard.isPublic());

        dashboard.makePrivate();
        assertFalse(dashboard.isPublic());
    }

    @Test
    @DisplayName("Should copy Dashboard to another user")
    void shouldCopyDashboardToAnotherUser() {
        Dashboard dashboard = Dashboard.create(
                "源仪表板",
                "源仪表板描述",
                1L,
                false,
                "{\"widgets\":[]}"
        );

        Dashboard copied = dashboard.copyToUser(2L);

        assertNotNull(copied);
        assertTrue(copied.getName().contains("副本"));
        assertEquals(dashboard.getDescription(), copied.getDescription());
        assertEquals(2L, copied.getUserId());
        assertFalse(copied.isPublic());
        assertEquals(dashboard.getConfig(), copied.getConfig());
        assertNotNull(copied.getCreatedAt());
        assertNotEquals(dashboard.getCreatedAt(), copied.getCreatedAt());
    }

    @Test
    @DisplayName("Should delete Dashboard")
    void shouldDeleteDashboard() {
        Dashboard dashboard = Dashboard.create(
                "待删除仪表板",
                "待删除描述",
                1L,
                false,
                "{}"
        );

        dashboard.delete();

        assertTrue(dashboard.isDeleted());
        assertNotNull(dashboard.getUpdatedAt());
    }

    @Test
    @DisplayName("Should validate Dashboard with valid parameters")
    void shouldValidateDashboardWithValidParameters() {
        Dashboard dashboard = Dashboard.create(
                "验证测试",
                "验证描述",
                1L,
                true,
                "{\"widgets\":[]}"
        );

        assertDoesNotThrow(dashboard::validate);
    }

    @Test
    @DisplayName("Should validate Dashboard equality")
    void shouldValidateDashboardEquality() {
        Dashboard dashboard1 = Dashboard.create(
                "仪表板1",
                "描述1",
                1L,
                false,
                "{}"
        );
        dashboard1.setId(1L);

        Dashboard dashboard2 = Dashboard.create(
                "仪表板1",
                "描述1",
                1L,
                false,
                "{}"
        );
        dashboard2.setId(2L);

        Dashboard dashboard3 = Dashboard.create(
                "仪表板2",
                "描述2",
                2L,
                true,
                "{}"
        );
        dashboard3.setId(3L);

        assertNotEquals(dashboard1, dashboard2);
        assertNotEquals(dashboard1, dashboard3);
    }
}
