package com.sism.analytics.application;

import com.sism.analytics.domain.Dashboard;
import com.sism.analytics.infrastructure.repository.DashboardRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DashboardApplicationService - 仪表板应用服务
 * 负责协调仪表板相关的业务操作
 */
@Service
@RequiredArgsConstructor
public class DashboardApplicationService {

    private final DashboardRepository dashboardRepository;
    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;

    /**
     * 创建仪表板
     */
    @Transactional
    public Dashboard createDashboard(String name, String description, Long userId, boolean isPublic, String config) {
        Dashboard dashboard = Dashboard.create(name, description, userId, isPublic, config);
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 更新仪表板信息
     */
    @Transactional
    public Dashboard updateDashboard(Long dashboardId, String name, String description, boolean isPublic, String config) {
        Dashboard dashboard = findById(dashboardId);
        dashboard.update(name, description, isPublic, config);
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 更新仪表板配置
     */
    @Transactional
    public Dashboard updateDashboardConfig(Long dashboardId, String config) {
        Dashboard dashboard = findById(dashboardId);
        dashboard.updateConfig(config);
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 设置为公开
     */
    @Transactional
    public Dashboard makePublic(Long dashboardId) {
        Dashboard dashboard = findById(dashboardId);
        dashboard.makePublic();
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 设置为私有
     */
    @Transactional
    public Dashboard makePrivate(Long dashboardId) {
        Dashboard dashboard = findById(dashboardId);
        dashboard.makePrivate();
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 删除仪表板
     */
    @Transactional
    public void deleteDashboard(Long dashboardId) {
        Dashboard dashboard = findById(dashboardId);
        dashboard.delete();
        publishAndSaveEvents(dashboard);
        dashboardRepository.save(dashboard);
    }

    /**
     * 复制仪表板到新用户
     */
    @Transactional
    public Dashboard copyDashboardToUser(Long dashboardId, Long targetUserId) {
        Dashboard original = findById(dashboardId);
        Dashboard copied = original.copyToUser(targetUserId);
        publishAndSaveEvents(copied);
        return dashboardRepository.save(copied);
    }

    /**
     * 查找仪表板
     */
    public Optional<Dashboard> findDashboardById(Long dashboardId) {
        return dashboardRepository.findByIdAndNotDeleted(dashboardId);
    }

    /**
     * 查找用户的所有仪表板
     */
    public List<Dashboard> findDashboardsByUserId(Long userId) {
        return dashboardRepository.findByUserIdAndNotDeleted(userId);
    }

    /**
     * 查找用户的所有公开仪表板
     */
    public List<Dashboard> findPublicDashboardsByUserId(Long userId) {
        return dashboardRepository.findByUserIdAndPublicAndNotDeleted(userId);
    }

    /**
     * 查找所有公开的仪表板
     */
    public List<Dashboard> findAllPublicDashboards() {
        return dashboardRepository.findAllPublicAndNotDeleted();
    }

    /**
     * 按名称搜索用户的仪表板
     */
    public List<Dashboard> searchDashboardsByName(Long userId, String name) {
        return dashboardRepository.findByUserIdAndNameContainingAndNotDeleted(userId, name);
    }

    /**
     * 统计用户的仪表板数量
     */
    public long countDashboardsByUserId(Long userId) {
        return dashboardRepository.countByUserIdAndNotDeleted(userId);
    }

    /**
     * 统计公开仪表板数量
     */
    public long countPublicDashboards() {
        return dashboardRepository.countAllPublicAndNotDeleted();
    }

    /**
     * 获取仪表板详情
     */
    private Dashboard findById(Long dashboardId) {
        return dashboardRepository.findByIdAndNotDeleted(dashboardId)
                .orElseThrow(() -> new IllegalArgumentException("Dashboard not found: " + dashboardId));
    }

    /**
     * 发布和保存领域事件
     */
    private void publishAndSaveEvents(Dashboard dashboard) {
        List<DomainEvent> events = dashboard.getDomainEvents();
        if (events != null && !events.isEmpty()) {
            for (DomainEvent event : events) {
                eventStore.save(event);
            }
            eventPublisher.publishAll(events);
            dashboard.clearEvents();
        }
    }
}
