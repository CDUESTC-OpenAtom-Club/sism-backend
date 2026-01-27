@echo off
REM Health Check 诊断脚本
REM 用于快速定位 Actuator Health 端点响应慢的原因

echo ========================================
echo Spring Boot Actuator Health 诊断工具
echo ========================================
echo.

echo [1/5] 测试基本连通性...
curl -w "\n响应时间: %%{time_total}s\n" -s -o nul http://127.0.0.1:8080/api/actuator/health
echo.

echo [2/5] 获取详细健康信息...
curl -s http://127.0.0.1:8080/api/actuator/health | jq . 2>nul || curl -s http://127.0.0.1:8080/api/actuator/health
echo.

echo [3/5] 测试数据库连接...
curl -w "\n数据库检查响应时间: %%{time_total}s\n" -s http://127.0.0.1:8080/api/actuator/health/db
echo.

echo [4/5] 测试 Liveness 探针...
curl -w "\n响应时间: %%{time_total}s\n" -s http://127.0.0.1:8080/api/actuator/health/liveness
echo.

echo [5/5] 测试 Readiness 探针...
curl -w "\n响应时间: %%{time_total}s\n" -s http://127.0.0.1:8080/api/actuator/health/readiness
echo.

echo ========================================
echo 诊断完成
echo ========================================
echo.
echo 提示：
echo - 如果某个检查特别慢，说明该组件存在问题
echo - 数据库检查慢：检查 PostgreSQL 是否正常运行
echo - 整体都慢：可能是网络或 DNS 解析问题
echo.
pause
