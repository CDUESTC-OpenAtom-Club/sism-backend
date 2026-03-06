# 脚本清理归档 - 2026-03-06

本目录包含在脚本清理过程中移除的脚本文件。

---

## 📦 归档文件

### auto-setup-server.sh
- **原因**: 功能已被 `scripts/deployment/setup-server.sh` 整合
- **状态**: 已废弃
- **替代方案**: 使用 `scripts/deployment/setup-server.sh`

### auto-setup-sudoers.sh
- **原因**: 功能已被 `scripts/deployment/setup-server.sh` 整合
- **状态**: 已废弃
- **替代方案**: 使用 `scripts/deployment/setup-server.sh`

### check-database.sh
- **原因**: 
  - 功能与 `scripts/deployment/init-database.sh` 重复
  - 硬编码数据库凭证（安全风险）
  - 使用 `./mvnw` 启动应用的方式不合理
- **状态**: 已废弃
- **替代方案**: 
  - 数据库初始化: `scripts/deployment/init-database.sh`
  - 健康检查: `scripts/deployment/health-check.sh`

### fix-runner-git-permissions.sh
- **原因**: 一次性修复脚本，问题已解决
- **状态**: 已归档（保留以备将来参考）
- **说明**: 用于修复 GitHub Actions self-hosted runner 的 Git 权限问题

---

## 🔄 清理详情

### 清理日期
2026-03-06

### 清理原因
1. 消除功能重复
2. 移除安全隐患（硬编码凭证）
3. 简化脚本结构
4. 提高可维护性

### 影响范围
- 无外部引用
- 无 CI/CD 依赖
- 无文档引用

---

## 📊 清理效果

### 清理前
- scripts/ 目录: ~20 个文件
- 结构: 混乱，功能重复

### 清理后
- scripts/ 目录: ~13 个文件
- 结构: 清晰，按功能分类
  - `deployment/` - 部署脚本
  - `testing/` - 测试脚本
  - `sync/` - 数据同步工具

---

## ⚠️ 恢复说明

如果需要恢复这些脚本：

```bash
# 恢复单个文件
cp archive/scripts-cleanup-20260306/<filename> scripts/

# 恢复所有文件
cp archive/scripts-cleanup-20260306/*.sh scripts/
```

**注意**: 不建议恢复这些脚本，请使用推荐的替代方案。

---

## 📚 相关文档

- **清理分析报告**: `../../scripts/CLEANUP_ANALYSIS.md`
- **部署脚本文档**: `../../scripts/deployment/README.md`
- **测试脚本文档**: `../../scripts/testing/README.md`

---

**归档人**: Kiro AI Assistant  
**归档日期**: 2026-03-06
