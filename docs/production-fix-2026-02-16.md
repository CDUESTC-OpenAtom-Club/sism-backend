# 生产环境修复记录 - 2026-02-16

## 问题描述

生产环境 (https://blackevil.cn) 所有 API 端点返回 404 错误：
```
org.springframework.web.servlet.resource.NoResourceFoundException: No static resource {path}
```

## 根本原因

SpringDoc (Swagger) 在生产环境被禁用 (`SWAGGER_ENABLED=false`)，导致 Spring Boot 3.2.0 无法正确注册 REST Controllers。

## 解决方案

修改服务器配置文件 `/opt/sism/backend/.env`：
```bash
# 修改前
SWAGGER_ENABLED=false

# 修改后
SWAGGER_ENABLED=true
```

重启服务：
```bash
systemctl restart sism-backend
```

## 验证结果

所有 API 端点恢复正常：
- ✅ Health Check: 正常
- ✅ Authentication: 正常
- ✅ Organization API: 正常 (712 个组织)
- ✅ Indicator API: 正常 (712 个指标)
- ✅ Dashboard API: 正常
- ✅ Swagger UI: 可访问

## 重要说明

⚠️ **生产环境必须保持 `SWAGGER_ENABLED=true`**

这是 Spring Boot 3.2.0 + SpringDoc 的已知兼容性问题。禁用 SpringDoc 会导致 Controllers 无法注册。

## 相关文档

- 完整验证报告: 见根目录 `production-validation-report.md`
- 部署指南: 见根目录 `docs/deployment/springdoc-requirement.md`
- 项目状态: 见根目录 `PROJECT-STATUS.md`

## 修复时间

- 问题发现: 2026-02-16 00:00 CST
- 问题解决: 2026-02-16 00:30 CST
- 停机时间: 约 30 分钟
