@echo off
REM Health Check 性能测试脚本
REM 对比优化前后的响应时间

setlocal enabledelayedexpansion

echo ========================================
echo Health Check 性能测试
echo ========================================
echo.

REM 检查应用是否运行
curl -s http://127.0.0.1:8080/api/actuator/health >nul 2>&1
if errorlevel 1 (
    echo [错误] 应用未运行或无法访问
    echo 请先启动应用: mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    pause
    exit /b 1
)

echo [测试 1] 使用 127.0.0.1 (推荐)
echo ----------------------------------------
set total=0
for /L %%i in (1,1,5) do (
    echo 第 %%i 次请求...
    for /f "tokens=*" %%a in ('curl -w "%%{time_total}" -s -o nul http://127.0.0.1:8080/api/actuator/health') do (
        echo   响应时间: %%a 秒
    )
)
echo.

echo [测试 2] 使用 localhost (对比)
echo ----------------------------------------
for /L %%i in (1,1,3) do (
    echo 第 %%i 次请求...
    for /f "tokens=*" %%a in ('curl -w "%%{time_total}" -s -o nul http://localhost:8080/api/actuator/health') do (
        echo   响应时间: %%a 秒
    )
)
echo.

echo [测试 3] Liveness 探针 (最快)
echo ----------------------------------------
for /L %%i in (1,1,3) do (
    echo 第 %%i 次请求...
    for /f "tokens=*" %%a in ('curl -w "%%{time_total}" -s -o nul http://127.0.0.1:8080/api/actuator/health/liveness') do (
        echo   响应时间: %%a 秒
    )
)
echo.

echo ========================================
echo 测试完成
echo ========================================
echo.
echo 性能标准:
echo   优秀: ^< 0.5 秒
echo   良好: 0.5 - 1.0 秒
echo   一般: 1.0 - 2.0 秒
echo   较慢: ^> 2.0 秒
echo.
echo 如果响应时间仍然较慢，请运行: diagnose-health.bat
echo.
pause
