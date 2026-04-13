package com.sism.iam.domain.repository;

import com.sism.iam.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
     * 分页查询所有用户
     */
    Page<User> findAll(Pageable pageable);

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
     * 根据用户ID查询角色ID列表
     */
    List<Long> findRoleIdsByUserId(Long userId);

    /**
     * 根据用户ID查询角色编码列表
     */
    List<String> findRoleCodesByUserId(Long userId);

    /**
     * 根据用户ID查询权限编码列表
     */
    default List<String> findPermissionCodesByUserId(Long userId) {
        return List.of();
    }

    /**
     * 批量统计角色关联用户数
     */
    default Map<Long, Long> countUsersByRoleIds(Set<Long> roleIds) {
        return Map.of();
    }

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
