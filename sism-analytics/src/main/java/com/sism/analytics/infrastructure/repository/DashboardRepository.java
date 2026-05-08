package com.sism.analytics.infrastructure.repository;

import com.sism.analytics.domain.Dashboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DashboardRepository - 仪表板仓储接口
 */
@Repository
public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    /**
     * 根据ID查找未删除的仪表板
     */
    @Query("SELECT d FROM Dashboard d WHERE d.id = :id AND d.deleted = false")
    Optional<Dashboard> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 根据用户ID查找所有未删除的仪表板
     */
    @Query("SELECT d FROM Dashboard d WHERE d.userId = :userId AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<Dashboard> findByUserIdAndNotDeleted(@Param("userId") Long userId, Pageable pageable);

    default List<Dashboard> findByUserIdAndNotDeleted(Long userId) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByUserIdAndNotDeleted(userId, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据用户ID查找所有未删除的公开仪表板
     */
    @Query("SELECT d FROM Dashboard d WHERE d.userId = :userId AND d.isPublic = true AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<Dashboard> findByUserIdAndPublicAndNotDeleted(@Param("userId") Long userId, Pageable pageable);

    default List<Dashboard> findByUserIdAndPublicAndNotDeleted(Long userId) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByUserIdAndPublicAndNotDeleted(userId, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 查找所有未删除的公开仪表板
     */
    @Query("SELECT d FROM Dashboard d WHERE d.isPublic = true AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<Dashboard> findAllPublicAndNotDeleted(Pageable pageable);

    default List<Dashboard> findAllPublicAndNotDeleted() {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findAllPublicAndNotDeleted(AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 根据名称模糊查找用户的仪表板
     */
    @Query("SELECT d FROM Dashboard d WHERE d.userId = :userId AND d.name LIKE CONCAT('%', :name, '%') ESCAPE '\\' AND d.deleted = false ORDER BY d.createdAt DESC")
    Page<Dashboard> findByUserIdAndNameContainingAndNotDeleted(
            @Param("userId") Long userId,
            @Param("name") String name,
            Pageable pageable
    );

    default List<Dashboard> findByUserIdAndNameContainingAndNotDeleted(Long userId, String name) {
        return AnalyticsRepositoryPagingSupport.contentOf(
                findByUserIdAndNameContainingAndNotDeleted(userId, name, AnalyticsRepositoryPagingSupport.firstPage())
        );
    }

    /**
     * 统计用户的仪表板数量
     */
    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.userId = :userId AND d.deleted = false")
    long countByUserIdAndNotDeleted(@Param("userId") Long userId);

    /**
     * 统计公开仪表板数量
     */
    @Query("SELECT COUNT(d) FROM Dashboard d WHERE d.isPublic = true AND d.deleted = false")
    long countAllPublicAndNotDeleted();
}
