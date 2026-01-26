@echo off
chcp 65001 >nul

REM ========================================
REM   SISM 后端服务 - 生产模式启动脚本
REM ========================================

REM 检查 .env 文件是否存在
if not exist ".env" (
    echo [错误] 未找到 .env 文件
    echo 请复制 .env.example 为 .env 并填写配置
    echo 示例: copy .env.example .env
    pause
    exit /b 1
)

REM 从 .env 文件加载环境变量
echo 正在从 .env 文件加载配置...
for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
    REM 跳过空行和注释行
    if not "%%a"=="" (
        echo %%a | findstr /b "#" >nul
        if errorlevel 1 (
            set "%%a=%%b"
        )
    )
)

REM 显示配置信息
echo.
echo ========================================
echo   SISM 后端服务 - 生产模式
echo ========================================
echo 数据库: %DB_URL%
echo 日志路径: %LOG_PATH%
echo ========================================
echo.

REM 启动应用
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=prod"
