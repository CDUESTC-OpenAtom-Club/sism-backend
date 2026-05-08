package com.sism.iam.domain.access;

import com.sism.iam.domain.access.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * 分页查询角色
     */
    Page<Role> findAll(Pageable pageable);

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
     * 批量统计角色权限数
     */
    Map<Long, Long> countPermissionsByRoleIds(Set<Long> roleIds);

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
