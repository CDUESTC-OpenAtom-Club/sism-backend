# SISM Backend Deployment Scripts

本目录只保留**当前仍建议长期使用**的后端部署与运维脚本。

## 当前保留

### `deploy.sh`
服务器上的通用部署脚本，用于：

- 更新部署 JAR 软链接
- 重启 `sism-backend` 服务
- 进行启动后的健康检查

### `setup-server.sh`
一次性服务器初始化脚本，用于：

- 配置部署账号的最小 sudo 权限
- 设置 `/opt/sism/backend` 目录权限
- 验证部署账号是否具备重启服务和写目录能力

### `health-check.sh`
后端部署后的健康检查脚本，用于快速确认：

- Spring Boot 服务可达
- Actuator 健康端点返回正常
- 数据库连接可用
- 磁盘空间未达到危险阈值

### `backup-database.sh`
PostgreSQL 备份脚本，适用于生产或受控测试环境的例行备份。

### `restore-database.sh`
PostgreSQL 恢复脚本，适用于从备份文件恢复数据库；内部校验已对齐当前 `sys_* / cycle / indicator / sys_task` 表结构。

## 不再保留的脚本

以下脚本已从本目录移除，因为它们已经不符合长期维护标准：

- `init-database.sh`：已被 Flyway 初始化流程与 `database/scripts/` 替代
- `quick-setup.sh`：包含环境绑定信息，不适合作为公共脚本长期保留

## 推荐用法

### 本地开发数据库

```bash
./mvnw flyway:migrate
./database/scripts/reset-clean-seeds.sh
```

### 服务器部署

```bash
# 一次性初始化部署账号
sudo ./scripts/deployment/setup-server.sh <deploy-user>

# 服务器上完成 JAR 替换后执行部署
./scripts/deployment/deploy.sh sism-backend-1.0.0.jar
```

### 部署后检查

```bash
./scripts/deployment/health-check.sh
```

## 维护原则

1. 本目录只放可重复执行的部署/运维脚本。
2. 新脚本不得写死服务器、数据库或账号信息。
3. 任何一次性排障脚本都不应长期留在这里。
4. 如果部署流程变化，优先同步更新本目录 README 与 CI 工作流。
