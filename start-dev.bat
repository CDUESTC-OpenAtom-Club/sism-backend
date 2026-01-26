@echo off
chcp 65001 >nul

REM 从 .env 文件加载环境变量
if exist ".env" (
    echo 正在从 .env 文件加载配置...
    for /f "usebackq tokens=1,* delims==" %%a in (".env") do (
        REM 跳过注释行和空行
        set "line=%%a"
        if not "%%a"=="" if not "!line:~0,1!"=="#" (
            set "%%a=%%b"
        )
    )
) else (
    echo [警告] 未找到 .env 文件，请复制 .env.example 并填写配置
    echo 示例: copy .env.example .env
    pause
    exit /b 1
)

REM 验证必需的环境变量
setlocal enabledelayedexpansion
set "missing="
if "%DB_HOST%"=="" set "missing=!missing! DB_HOST"
if "%DB_USERNAME%"=="" set "missing=!missing! DB_USERNAME"
if "%DB_PASSWORD%"=="" set "missing=!missing! DB_PASSWORD"
if "%JWT_SECRET%"=="" set "missing=!missing! JWT_SECRET"

if not "%missing%"=="" (
    echo [错误] 缺少必需的环境变量:%missing%
    echo 请检查 .env 文件配置
    pause
    exit /b 1
)
endlocal

echo 正在启动 SISM 后端服务 (dev 模式)...
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
