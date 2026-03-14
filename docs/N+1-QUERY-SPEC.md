# N+1查询检测规范

## 概述

本文档定义了项目中的N+1查询编码规范，用于防止性能问题的产生。

## N+1查询定义

N+1查询是指在执行一次查询后，再循环执行N次查询来获取关联数据的情况。这种模式会严重降低系统性能。

## 禁止的模式

### 1. 禁止在循环中调用Repository

```java
// ❌ 错误示例 - N+1查询
for (Long id : ids) {
    Indicator indicator = indicatorRepository.findById(id);  // N+1!
}

// ✅ 正确示例 - 使用批量查询
List<Indicator> indicators = indicatorRepository.findByIdIn(ids);
Map<Long, Indicator> indicatorMap = indicators.stream()
    .collect(Collectors.toMap(Indicator::getId, i -> i));
```

### 2. 禁止在Stream.forEach中调用Repository

```java
// ❌ 错误示例 - N+1查询
userList.stream()
    .forEach(user -> {
        List<Role> roles = roleRepository.findByUserId(user.getId());  // N+1!
    });

// ✅ 正确示例 - 使用JOIN查询
List<Role> roles = roleRepository.findByUserIds(userList.stream()
    .map(User::getId)
    .collect(Collectors.toList()));
Map<Long, List<Role>> roleMap = roles.stream()
    .collect(Collectors.groupingBy(Role::getUserId));
```

### 3. 禁止在递归中调用Repository

```java
// ❌ 错误示例 - 递归中的N+1查询
public List<Org> getOrgTree(Long parentId) {
    List<Org> children = orgRepository.findByParentId(parentId);  // 每次递归调用
    for (Org child : children) {
        child.setChildren(getOrgTree(child.getId()));  // 递归 N+1!
    }
    return children;
}

// ✅ 正确示例 - 使用CTE或递归查询一次性获取所有数据
public List<Org> getOrgTree(Long rootId) {
    return orgRepository.findAllWithChildren(rootId);  // 一次性查询
}
```

## 检测机制

项目集成了`NPlusOneQueryDetector`组件，用于在测试环境中自动检测N+1查询：

1. 所有集成测试必须继承`NPlusOneSafeIntegrationTest`
2. 测试执行过程中会自动记录SQL执行次数
3. 如果同一SQL执行超过5次，测试将失败并抛出`NPlusOneQueryException`

## 正确的批量查询模式

### 使用JOIN FETCH

```java
@Query("SELECT DISTINCT i FROM Indicator i LEFT JOIN FETCH i.targets WHERE i.id IN :ids")
List<Indicator> findAllWithTargetsByIdIn(@Param("ids") List<Long> ids);
```

### 使用EntityGraph

```java
@EntityGraph(attributePaths = {"roles", "permissions"})
Optional<User> findWithRolesById(Long id);
```

### 使用Batch注解

```java
@BatchSize(size = 100)
@OneToMany(mappedBy = "user")
private List<Role> roles;
```

## 验收标准

- [x] 所有集成测试继承NPlusOneSafeIntegrationTest
- [x] N+1查询检测器实现并可抛出异常
- [x] 测试环境自动启用N+1检测
- [x] 编码规范文档已创建
