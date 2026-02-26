package com.sism.repository;

import com.sism.entity.SysRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysRolePermissionRepository extends JpaRepository<SysRolePermission, Long> {
}
