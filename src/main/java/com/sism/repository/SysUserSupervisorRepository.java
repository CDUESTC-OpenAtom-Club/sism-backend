package com.sism.repository;

import com.sism.entity.SysUserSupervisor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SysUserSupervisor entity
 * Manages user-supervisor relationships
 */
@Repository
public interface SysUserSupervisorRepository extends JpaRepository<SysUserSupervisor, Long> {

    /**
     * Find all supervisors for a user
     */
    List<SysUserSupervisor> findByUserId(Long userId);

    /**
     * Find supervisor by user and level
     */
    Optional<SysUserSupervisor> findByUserIdAndLevel(Long userId, Integer level);

    /**
     * Find direct supervisor (level 1) for a user
     */
    @Query("SELECT s FROM SysUserSupervisor s WHERE s.userId = :userId AND s.level = 1")
    Optional<SysUserSupervisor> findDirectSupervisor(@Param("userId") Long userId);

    /**
     * Find level-2 supervisor for a user
     */
    @Query("SELECT s FROM SysUserSupervisor s WHERE s.userId = :userId AND s.level = 2")
    Optional<SysUserSupervisor> findLevel2Supervisor(@Param("userId") Long userId);

    /**
     * Find all subordinates of a supervisor
     */
    List<SysUserSupervisor> findBySupervisorId(Long supervisorId);

    /**
     * Check if a supervisor relationship exists
     */
    boolean existsByUserIdAndLevel(Long userId, Integer level);

    /**
     * Delete supervisor relationship by user and level
     */
    void deleteByUserIdAndLevel(Long userId, Integer level);
}
