package com.sism.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * User Supervisor Relationship Entity
 * Stores the supervisor relationships for users, supporting multi-level supervisors
 * 
 * Fields:
 * - userId: The subordinate user ID
 * - supervisorId: The supervisor user ID  
 * - level: Supervisor level (1 = direct supervisor, 2 = level-2 supervisor)
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user_supervisor", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_level", columnNames = {"user_id", "level"})
}, indexes = {
    @Index(name = "idx_user_supervisor_user", columnList = "user_id"),
    @Index(name = "idx_user_supervisor_level", columnList = "user_id, level"),
    @Index(name = "idx_user_supervisor_supervisor", columnList = "supervisor_id")
})
public class SysUserSupervisor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "supervisor_id", nullable = false)
    private Long supervisorId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // ==================== Constructors ====================

    public SysUserSupervisor() {
    }

    public SysUserSupervisor(Long userId, Long supervisorId, Integer level) {
        this.userId = userId;
        this.supervisorId = supervisorId;
        this.level = level;
    }

    // ==================== Helper Methods ====================

    /**
     * Check if this is a direct supervisor relationship (level 1)
     */
    public boolean isDirectSupervisor() {
        return level != null && level == 1;
    }

    /**
     * Check if this is a level-2 supervisor relationship
     */
    public boolean isLevel2Supervisor() {
        return level != null && level == 2;
    }
}
