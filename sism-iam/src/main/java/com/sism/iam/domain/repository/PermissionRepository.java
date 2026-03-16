package com.sism.iam.domain.repository;

import com.sism.iam.domain.Permission;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * PermissionRepository - 权限仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface PermissionRepository {

    /**
     * 根据ID查询权限
     */
    Optional<Permission> findById(Long id);

    /**
     * 根据ID集合查询权限列表
     */
    Set<Permission> findByIds(Set<Long> ids);

    /**
     * 根据权限代码查询权限
     */
    Optional<Permission> findByPermissionCode(String permCode);

    /**
     * 查询所有权限
     */
    List<Permission> findAll();

    /**
     * 保存权限
     */
    Permission save(Permission permission);

    /**
     * 删除权限
     */
    void delete(Permission permission);

    /**
     * 检查权限是否存在
     */
    boolean existsById(Long id);

    /**
     * 检查权限代码是否存在
     */
    boolean existsByPermissionCode(String permissionCode);
}
