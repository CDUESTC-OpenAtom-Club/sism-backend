package com.sism.repository;

import com.sism.entity.SysUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SysUserRoleRepository extends JpaRepository<SysUserRole, Long> {
}
