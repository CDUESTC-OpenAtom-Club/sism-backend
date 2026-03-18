package com.sism.analytics.infrastructure.repository;

import com.sism.analytics.domain.Dashboard;
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
    List<Dashboard> findByUserIdAndNotDeleted(@Param("userId") Long userId);

    /**
     * 根据用户ID查找所有未删除的公开仪表板
     */
    @Query("SELECT d FROM Dashboard d WHERE d.userId = :userId AND d.isPublic = true AND d.deleted = false ORDER BY d.createdAt DESC")
    List<Dashboard> findByUserIdAndPublicAndNotDeleted(@Param("userId") Long userId);

    /**
     * 查找所有未删除的公开仪表板
     */
    @Query("SELECT d FROM Dashboard d WHERE d.isPublic = true AND d.deleted = false ORDER BY d.createdAt DESC")
    List<Dashboard> findAllPublicAndNotDeleted();

    /**
     * 根据名称模糊查找用户的仪表板
     */
    @Query("SELECT d FROM Dashboard d WHERE d.userId = :userId AND d.name LIKE %:name% AND d.deleted = false ORDER BY d.createdAt DESC")
    List<Dashboard> findByUserIdAndNameContainingAndNotDeleted(
            @Param("userId") Long userId,
            @Param("name") String name
    );

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
