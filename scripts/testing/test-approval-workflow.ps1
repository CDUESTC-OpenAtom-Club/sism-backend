# 测试两级主管审批流程
# 使用方式: .\scripts\test-approval-workflow.ps1

$ErrorActionPreference = "Stop"

$BASE_URL = "http://localhost:8080/api"
$PLAN_ID = 9001

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "两级主管审批流程测试" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 步骤 1: 获取 JWT Token
Write-Host "步骤 1: 获取测试用户 Token" -ForegroundColor Yellow
Write-Host "----------------------------------------"

# 填报人 (使用 admin 用户)
Write-Host "获取填报人 Token (admin)..."
$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $REPORTER_TOKEN = $response.data.accessToken
    Write-Host "✓ 填报人 Token: $($REPORTER_TOKEN.Substring(0, 20))..." -ForegroundColor Green
} catch {
    Write-Host "❌ 获取填报人 Token 失败: $_" -ForegroundColor Red
    exit 1
}

# 一级主管 (使用 zhangsan)
Write-Host "获取一级主管 Token (zhangsan)..."
$loginBody = @{
    username = "zhangsan"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $LEVEL1_TOKEN = $response.data.accessToken
    Write-Host "✓ 一级主管 Token: $($LEVEL1_TOKEN.Substring(0, 20))..." -ForegroundColor Green
} catch {
    Write-Host "❌ 获取一级主管 Token 失败: $_" -ForegroundColor Red
    exit 1
}

# 二级主管 (使用 lisi)
Write-Host "获取二级主管 Token (lisi)..."
$loginBody = @{
    username = "lisi"
    password = "password123"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    $LEVEL2_TOKEN = $response.data.accessToken
    Write-Host "✓ 二级主管 Token: $($LEVEL2_TOKEN.Substring(0, 20))..." -ForegroundColor Green
} catch {
    Write-Host "❌ 获取二级主管 Token 失败: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 步骤 2: 提交计划
Write-Host "步骤 2: 填报人提交计划" -ForegroundColor Yellow
Write-Host "----------------------------------------"
Write-Host "提交计划 ID: $PLAN_ID"

$headers = @{
    "Authorization" = "Bearer $REPORTER_TOKEN"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/plans/approval/$PLAN_ID/submit?userId=1" -Method Post -Headers $headers
    $INSTANCE_ID = $response.data.id
    Write-Host "✓ 计划提交成功，审批实例 ID: $INSTANCE_ID" -ForegroundColor Green
    Write-Host "响应: $($response | ConvertTo-Json -Depth 3)"
} catch {
    Write-Host "❌ 提交计划失败: $_" -ForegroundColor Red
    Write-Host $_.Exception.Response
    exit 1
}

Write-Host ""
Start-Sleep -Seconds 1

# 步骤 3: 一级主管审批
Write-Host "步骤 3: 一级主管审批" -ForegroundColor Yellow
Write-Host "----------------------------------------"
Write-Host "审批实例 ID: $INSTANCE_ID"

$headers = @{
    "Authorization" = "Bearer $LEVEL1_TOKEN"
    "Content-Type" = "application/json"
}

$approveBody = @{
    approverId = 2
    comment = "一级主管同意，请二级主管审核"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/plans/approval/instances/$INSTANCE_ID/approve" -Method Post -Headers $headers -Body $approveBody
    Write-Host "✓ 一级主管审批成功" -ForegroundColor Green
    Write-Host "响应: $($response | ConvertTo-Json -Depth 3)"
} catch {
    Write-Host "❌ 一级主管审批失败: $_" -ForegroundColor Red
    Write-Host $_.Exception.Response
    exit 1
}

Write-Host ""
Start-Sleep -Seconds 1

# 步骤 4: 二级主管审批
Write-Host "步骤 4: 二级主管审批" -ForegroundColor Yellow
Write-Host "----------------------------------------"
Write-Host "审批实例 ID: $INSTANCE_ID"

$headers = @{
    "Authorization" = "Bearer $LEVEL2_TOKEN"
    "Content-Type" = "application/json"
}

$approveBody = @{
    approverId = 3
    comment = "二级主管最终批准"
} | ConvertTo-Json

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/plans/approval/instances/$INSTANCE_ID/approve" -Method Post -Headers $headers -Body $approveBody
    Write-Host "✓ 二级主管审批成功" -ForegroundColor Green
    Write-Host "响应: $($response | ConvertTo-Json -Depth 3)"
} catch {
    Write-Host "❌ 二级主管审批失败: $_" -ForegroundColor Red
    Write-Host $_.Exception.Response
    exit 1
}

Write-Host ""

# 步骤 5: 验证最终状态
Write-Host "步骤 5: 验证审批流程完成" -ForegroundColor Yellow
Write-Host "----------------------------------------"

$headers = @{
    "Authorization" = "Bearer $REPORTER_TOKEN"
}

try {
    $response = Invoke-RestMethod -Uri "$BASE_URL/plans/approval/plans/$PLAN_ID/status" -Method Get -Headers $headers
    $status = $response.data.status
    
    if ($status -eq "APPROVED") {
        Write-Host "✓ 审批流程已完成，状态: APPROVED" -ForegroundColor Green
    } else {
        Write-Host "⚠ 当前状态: $status" -ForegroundColor Yellow
    }
    
    Write-Host "完整响应: $($response | ConvertTo-Json -Depth 3)"
} catch {
    Write-Host "❌ 查询状态失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "测试完成！" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
