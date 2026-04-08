# sism-organization 模块审计问题修复状态报告

**审查日期:** 2026-04-06  
**原审计报告:** `sism-organization-module-audit-report.md`  
**复核口径:** 以当前代码与 `mvn -pl sism-organization test` 结果为准

---

## 修复状态总览

| 状态 | 数量 | 占比 |
|------|------|------|
| ✅ 已修复 | 3 | 27% |
| ⚠️ 部分修复 | 0 | 0% |
| ❌ 未修复 | 8 | 73% |
| **合计** | **11** | 100% |

---

## 已修复问题

| # | 问题 | 严重等级 | 证据 |
|---|------|----------|------|
| 1 | 所有 API 端点完全缺少权限控制 | 🔴 Critical | `OrganizationController` 已补齐 `@PreAuthorize`，读接口限定 `ADMIN/ORG_MANAGER`，写接口限定 `ADMIN` |
| 2 | 用户列表接口无权限控制 | 🔴 High | `GET /{id}/users` 已受 `@PreAuthorize` 保护 |
| 3 | getParentOrg() 总是返回 null，组织树构建 NPE | 🔴 High | `SysOrg.getParentOrg()` 与 `OrganizationApplicationService.buildTree()` 已改为安全父节点引用/`parentOrgId` 构树，新增测试已覆盖 |

---

## 未修复问题

| # | 问题 | 严重等级 | 说明 |
|---|------|----------|------|
| 4 | 重复的 OrgType 枚举 | 🟠 Medium | 仍存在并行定义 |
| 5 | 重复的方法定义 | 🟠 Medium | 命名别名与重复入口仍存在 |
| 6 | 占位方法未实现 | 🟡 Low | 描述类占位接口仍未完整实现 |
| 7 | 组织树每次请求重新构建 | 🟠 Medium | 尚未引入缓存 |
| 8 | 查询所有组织无分页 | 🟡 Low | 仍返回列表 |
| 9 | 模块依赖 sism-iam | 🟠 Medium | 仍直接依赖用户仓储 |
| 10 | API 路径别名较多 | 🟠 Medium | `/organizations`、`/orgs`、`/departments` 仍并存 |
| 11 | 异常消息混合中英文 | 🟡 Low | 未统一整理 |

---

## 验证结果

```bash
mvn -pl sism-organization -Dtest=SysOrgTest,OrganizationApplicationServiceTest,OrganizationControllerSecurityTest test
mvn -pl sism-organization test
```

结论：权限与组织树可用性问题已收口，模块从“0% 修复”提升到“关键问题已修复”状态。
