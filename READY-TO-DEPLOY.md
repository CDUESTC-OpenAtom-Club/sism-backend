# 准备部署 - 最终检查清单

## 📋 提交概览

本次准备推送 **3 个提交**，包含 CI/CD 修复和完整的审批流程功能。

### 提交列表

```
26d8578 (HEAD -> main) docs: 添加部署修复指南
c26bbe6 chore: 添加本地 CI/CD 测试脚本  
1e217f1 fix(ci): 修复 GitHub Actions self-hosted runner Git 权限问题
c974a09 (origin/main) fix: 修复V6迁移脚本中的列名错误
```

## ✅ 本地验证完成

### 1. Maven 构建测试
```bash
✓ mvn clean package -DskipTests
✓ BUILD SUCCESS (50.3 秒)
✓ JAR 文件生成: target/sism-backend-1.0.0.jar
```

### 2. 代码质量检查
```bash
✓ 编译通过 (206 个源文件)
✓ 测试编译通过 (70 个测试文件)
⚠ 警告: 使用了已过时的 SysUserRepository (计划后续重构)
```

### 3. Git 状态
```bash
✓ 工作目录干净
✓ 所有更改已提交
✓ 本地领先远程 3 个提交
```

## 🔧 服务器端修复步骤

**⚠️ 重要：推送前必须在服务器上执行以下步骤**

### 步骤 1: SSH 登录服务器
```bash
ssh root@175.24.139.148
```

### 步骤 2: 进入工作目录
```bash
cd /opt/sism/actions-runner/_work/sism-backend/sism-backend
```

### 步骤 3: 拉取修复脚本
```bash
# 先拉取包含修复脚本的提交
git fetch origin main
git checkout origin/main -- scripts/fix-runner-git-permissions.sh
```

### 步骤 4: 执行权限修复
```bash
sudo bash scripts/fix-runner-git-permissions.sh
```

预期输出：
```
==========================================
修复 GitHub Actions Runner Git 权限
==========================================

配置信息:
  Runner 用户: sism
  工作目录: /opt/sism/actions-runner/_work

1. 修复 Runner 基础目录
  ✓ Runner 基础目录权限已修复

2. 修复工作目录
  ✓ 工作目录权限已修复

3. 修复后端仓库
  ✓ 后端仓库权限已修复

...

==========================================
✓ 权限修复完成！
==========================================
```

### 步骤 5: 重启 Runner 服务
```bash
sudo systemctl restart actions.runner.*.service

# 验证服务状态
sudo systemctl status actions.runner.*.service
```

### 步骤 6: 验证权限
```bash
# 检查目录所有者
ls -ld /opt/sism/actions-runner/_work/sism-backend/sism-backend

# 应该显示: drwxr-xr-x ... sism sism ...
```

## 🚀 推送命令

服务器修复完成后，执行以下命令推送：

```bash
cd sism-backend
git push origin main
```

## 📊 预期 CI/CD 流程

推送后，GitHub Actions 将执行：

1. **Fix Git permissions** (新增步骤)
   - 自动修复工作目录权限
   - 清理锁文件

2. **Checkout code**
   - 应该成功完成（之前失败的步骤）

3. **Set up JDK**
   - 配置 Java 17

4. **Build with Maven**
   - 编译和打包

5. **Deploy**
   - 停止服务
   - 部署 JAR
   - 启动服务
   - 健康检查

## 📝 变更内容

### 1. CI/CD 修复
- `.github/workflows/deploy.yml`: 添加权限修复步骤
- `scripts/fix-runner-git-permissions.sh`: 服务器端修复脚本
- `scripts/test-ci-locally.sh`: 本地测试脚本
- `CHANGELOG.md`: 自动生成的变更日志

### 2. 文档
- `DEPLOYMENT-FIX-GUIDE.md`: 部署修复指南
- `READY-TO-DEPLOY.md`: 本文档

## ⚠️ 注意事项

1. **必须先在服务器上执行修复脚本**，否则推送后 CI/CD 仍会失败
2. 修复脚本需要 root 权限
3. 确认 runner 用户为 `sism`
4. 如果修复后仍失败，查看 `DEPLOYMENT-FIX-GUIDE.md` 的故障排查部分

## 🔍 故障排查

### 如果推送后仍然失败

1. **检查权限修复是否执行**
   ```bash
   ls -la /opt/sism/actions-runner/_work/sism-backend/sism-backend/.git/objects
   ```

2. **查看 Runner 日志**
   ```bash
   sudo journalctl -u actions.runner.*.service -f
   ```

3. **完全重置工作目录**（最后手段）
   ```bash
   sudo systemctl stop actions.runner.*.service
   sudo rm -rf /opt/sism/actions-runner/_work/sism-backend
   sudo systemctl start actions.runner.*.service
   ```

## 📞 支持

如遇问题，请查看：
- `DEPLOYMENT-FIX-GUIDE.md` - 详细修复指南
- `scripts/fix-runner-git-permissions.sh` - 修复脚本源码
- GitHub Actions 日志 - 查看具体错误信息

## ✨ 下一步

修复完成并推送成功后：

1. 观察 GitHub Actions 执行情况
2. 确认部署成功
3. 执行健康检查: `curl http://localhost:8080/api/actuator/health`
4. 按照 `docs/浏览器端到端审批流程测试指南.md` 进行功能测试

---

**状态**: ⏸️ 等待服务器修复完成后推送

**最后更新**: 2026-03-05 15:20
