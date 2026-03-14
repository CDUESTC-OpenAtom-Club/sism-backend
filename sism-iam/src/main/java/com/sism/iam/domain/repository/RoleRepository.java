package com.sism.iam.domain.repository;

import com.sism.iam.domain.Role;

import java.util.List;
import java.util.Optional;

/**
 * RoleRepository - 角色仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface RoleRepository {

    /**
     * 根据ID查询角色
     */
    Optional<Role> findById(Long id);

    /**
     * 查询所有角色
     */
    List<Role> findAll();

    /**
     * 根据角色标识查询角色
     */
    Optional<Role> findByRoleCode(String roleCode);

    /**
     * 根据角色名称查询角色
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * 根据用户ID查询角色
     */
    List<Role> findByUserId(Long userId);

    /**
     * 保存角色
     */
    Role save(Role role);

    /**
     * 删除角色
     */
    void delete(Role role);

    /**
     * 检查角色是否存在
     */
    boolean existsById(Long id);

    /**
     * 检查角色标识是否存在
     */
    boolean existsByRoleCode(String roleCode);
}
