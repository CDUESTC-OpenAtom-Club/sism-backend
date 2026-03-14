# 测试脚本

本目录包含用于测试 SISM 后端功能的脚本。

---

## 📋 脚本列表

### test-approval-workflow.sh
**用途**: 测试两级主管审批流程

**使用方式**:
```bash
./test-approval-workflow.sh
```

**功能**:
- 模拟填报人提交计划
- 模拟一级主管审批
- 模拟二级主管审批
- 验证审批流程完成

**前置条件**:
- 后端服务运行在 `http://localhost:8080`
- 测试用户已创建（admin, zhangsan, lisi）
- 测试计划 ID 9001 存在

---

### test-approval-workflow.ps1
**用途**: Windows 版本的审批流程测试脚本

**使用方式**:
```powershell
.\test-approval-workflow.ps1
```

**功能**: 与 `.sh` 版本相同，适用于 Windows PowerShell 环境

---

### test-ci-locally.sh
**用途**: 本地模拟 CI/CD 流程测试

**使用方式**:
```bash
./test-ci-locally.sh
```

**功能**:
- 检查 Git 状态
- 验证 Java 和 Maven 环境
- 执行 Maven 构建
- 检查 JAR 文件生成
- 验证环境配置
- 可选：测试 JAR 启动和健康检查
- 检查 Git 提交和远程同步状态

**前置条件**:
- Java 17+
- Maven 3.6+
- Git

---

## 🚀 快速开始

### 运行所有测试

```bash
# 1. 确保后端服务运行
./start-local.sh

# 2. 运行 CI 测试
./scripts/testing/test-ci-locally.sh

# 3. 运行审批流程测试
./scripts/testing/test-approval-workflow.sh
```

---

## 📝 注意事项

1. **测试数据**: 这些脚本使用测试数据，不会影响生产环境
2. **服务依赖**: 大部分测试需要后端服务运行
3. **权限**: 确保脚本有执行权限 (`chmod +x *.sh`)
4. **环境变量**: 某些测试可能需要配置 `.env` 文件

---

## 🔧 故障排查

### 问题: 连接被拒绝

**解决方案**:
```bash
# 检查后端服务是否运行
curl http://localhost:8080/api/actuator/health

# 如果未运行，启动服务
./start-local.sh
```

### 问题: 认证失败

**解决方案**:
- 检查测试用户是否存在
- 验证用户密码是否正确
- 查看后端日志获取详细错误信息

### 问题: 测试数据不存在

**解决方案**:
```bash
# 重新初始化数据库
./scripts/deployment/init-database.sh
```

---

## 📚 相关文档

- **部署脚本**: `../deployment/README.md`
- **主 README**: `../../README.md`
- **API 文档**: `../../docs/api/`

---

**最后更新**: 2026-03-06
