package com.sism.iam.domain.repository;

import com.sism.iam.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository - 用户仓储接口
 * 定义在领域层，由基础设施层实现
 */
public interface UserRepository {

    /**
     * 根据ID查询用户
     */
    Optional<User> findById(Long id);

    /**
     * 查询所有用户
     */
    List<User> findAll();

    /**
     * 根据用户名查询用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据组织ID查询用户
     */
    List<User> findByOrgId(Long orgId);

    /**
     * 根据角色ID查询用户
     */
    List<User> findByRoleId(Long roleId);

    /**
     * 根据激活状态查询用户
     */
    List<User> findByIsActive(Boolean isActive);

    /**
     * 保存用户
     */
    User save(User user);

    /**
     * 删除用户
     */
    void delete(User user);

    /**
     * 检查用户是否存在
     */
    boolean existsById(Long id);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
}
