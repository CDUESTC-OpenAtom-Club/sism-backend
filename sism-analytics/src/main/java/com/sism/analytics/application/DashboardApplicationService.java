package com.sism.analytics.application;

import com.sism.analytics.domain.Dashboard;
import com.sism.analytics.infrastructure.repository.DashboardRepository;
import com.sism.shared.domain.model.base.DomainEvent;
import com.sism.shared.infrastructure.event.DomainEventPublisher;
import com.sism.shared.infrastructure.event.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

/**
 * DashboardApplicationService - 仪表板应用服务
 * 负责协调仪表板相关的业务操作
 */
@Service
@RequiredArgsConstructor
public class DashboardApplicationService extends BaseApplicationService {

    private final DashboardRepository dashboardRepository;
    private final DomainEventPublisher eventPublisher;
    private final EventStore eventStore;

    /**
     * 创建仪表板
     */
    @Transactional
    public Dashboard createDashboard(String name, String description, Long userId, boolean isPublic, String config) {
        requirePositiveUserId(userId, "User ID");
        Dashboard dashboard = Dashboard.create(name, description, userId, isPublic, config);
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 更新仪表板信息
     */
    @Transactional
    public Dashboard updateDashboard(Long dashboardId, Long currentUserId, String name, String description, boolean isPublic, String config) {
        Dashboard dashboard = findOwnedByCurrentUser(dashboardId, currentUserId);
        dashboard.update(name, description, isPublic, config);
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 更新仪表板配置
     */
    @Transactional
    public Dashboard updateDashboardConfig(Long dashboardId, Long currentUserId, String config) {
        Dashboard dashboard = findOwnedByCurrentUser(dashboardId, currentUserId);
        dashboard.updateConfig(config);
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 设置为公开
     */
    @Transactional
    public Dashboard makePublic(Long dashboardId, Long currentUserId) {
        Dashboard dashboard = findOwnedByCurrentUser(dashboardId, currentUserId);
        dashboard.makePublic();
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 设置为私有
     */
    @Transactional
    public Dashboard makePrivate(Long dashboardId, Long currentUserId) {
        Dashboard dashboard = findOwnedByCurrentUser(dashboardId, currentUserId);
        dashboard.makePrivate();
        publishAndSaveEvents(dashboard);
        return dashboardRepository.save(dashboard);
    }

    /**
     * 删除仪表板
     */
    @Transactional
    public void deleteDashboard(Long dashboardId, Long currentUserId) {
        Dashboard dashboard = findOwnedByCurrentUser(dashboardId, currentUserId);
        dashboard.delete();
        publishAndSaveEvents(dashboard);
        dashboardRepository.save(dashboard);
    }

    /**
     * 复制仪表板到新用户
     */
    @Transactional
    public Dashboard copyDashboardToUser(Long dashboardId, Long currentUserId, Long targetUserId) {
        requirePositiveUserId(targetUserId, "Target user ID");
        Dashboard original = findOwnedByCurrentUser(dashboardId, currentUserId);
        Dashboard copied = original.copyToUser(targetUserId);
        publishAndSaveEvents(copied);
        return dashboardRepository.save(copied);
    }

    /**
     * 查找仪表板
     */
    public Optional<Dashboard> findDashboardById(Long dashboardId, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dashboardRepository.findByIdAndNotDeleted(dashboardId)
                .filter(dashboard -> canAccessDashboard(dashboard, currentUserId));
    }

    /**
     * 查找用户的所有仪表板
     */
    public List<Dashboard> findDashboardsByUserId(Long userId, Long currentUserId) {
        requireUserOwnership(userId, currentUserId);
        return dashboardRepository.findByUserIdAndNotDeleted(userId);
    }

    public Page<Dashboard> findDashboardsByUserId(Long userId, Long currentUserId, int pageNum, int pageSize) {
        requireUserOwnership(userId, currentUserId);
        return dashboardRepository.findByUserIdAndNotDeleted(userId, AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 查找用户的所有公开仪表板
     */
    public List<Dashboard> findPublicDashboardsByUserId(Long userId, Long currentUserId) {
        requireUserOwnership(userId, currentUserId);
        return dashboardRepository.findByUserIdAndPublicAndNotDeleted(userId);
    }

    public Page<Dashboard> findPublicDashboardsByUserId(Long userId, Long currentUserId, int pageNum, int pageSize) {
        requireUserOwnership(userId, currentUserId);
        return dashboardRepository.findByUserIdAndPublicAndNotDeleted(userId, AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 查找所有公开的仪表板
     */
    public List<Dashboard> findAllPublicDashboards() {
        return dashboardRepository.findAllPublicAndNotDeleted();
    }

    public Page<Dashboard> findAllPublicDashboards(int pageNum, int pageSize) {
        return dashboardRepository.findAllPublicAndNotDeleted(AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 按名称搜索用户的仪表板
     */
    public List<Dashboard> searchDashboardsByName(Long userId, Long currentUserId, String name) {
        requireUserOwnership(userId, currentUserId);
        return dashboardRepository.findByUserIdAndNameContainingAndNotDeleted(userId, name);
    }

    public Page<Dashboard> searchDashboardsByName(Long userId, Long currentUserId, String name, int pageNum, int pageSize) {
        requireUserOwnership(userId, currentUserId);
        return dashboardRepository.findByUserIdAndNameContainingAndNotDeleted(
                userId,
                name,
                AnalyticsPaginationSupport.toPageable(pageNum, pageSize));
    }

    /**
     * 统计用户的仪表板数量
     */
    public long countDashboardsByUserId(Long userId, Long currentUserId) {
        requireUserOwnership(userId, currentUserId);
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
    private Dashboard findOwnedByCurrentUser(Long dashboardId, Long currentUserId) {
        requirePositiveUserId(currentUserId, "Current user ID");
        return dashboardRepository.findByIdAndNotDeleted(dashboardId)
                .filter(dashboard -> currentUserId.equals(dashboard.getUserId()))
                .orElseThrow(() -> new AccessDeniedException("No permission to access dashboard: " + dashboardId));
    }

    private boolean canAccessDashboard(Dashboard dashboard, Long currentUserId) {
        return dashboard.isPublic() || currentUserId.equals(dashboard.getUserId());
    }

    @Override
    protected void requireUserOwnership(Long requestedUserId, Long currentUserId) {
        requireUserOwnership(requestedUserId, currentUserId, "No permission to access another user's dashboards");
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
