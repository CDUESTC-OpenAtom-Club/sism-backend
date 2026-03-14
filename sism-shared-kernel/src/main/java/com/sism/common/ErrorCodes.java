package com.sism.common;

/**
 * 错误码常量类
 * 统一管理系统错误码
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    // ==================== 通用错误码 (1xxx) ====================
    
    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 请求参数错误
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 禁止访问
     */
    public static final int FORBIDDEN = 403;

    /**
     * 资源未找到
     */
    public static final int NOT_FOUND = 404;

    /**
     * 服务器内部错误
     */
    public static final int INTERNAL_ERROR = 500;

    // ==================== 业务错误码 (10xx) ====================

    /**
     * 业务规则验证失败
     */
    public static final int BUSINESS_VALIDATION_FAILED = 1001;

    /**
     * 数据已存在
     */
    public static final int DATA_ALREADY_EXISTS = 1002;

    /**
     * 数据不存在
     */
    public static final int DATA_NOT_FOUND = 1003;

    /**
     * 状态不允许操作
     */
    public static final int INVALID_STATUS = 1004;

    // ==================== 用户管理错误码 (20xx) ====================

    /**
     * 用户名或密码错误
     */
    public static final int INVALID_CREDENTIALS = 2001;

    /**
     * 用户已禁用
     */
    public static final int USER_DISABLED = 2002;

    /**
     * 用户已锁定
     */
    public static final int USER_LOCKED = 2003;

    /**
     *  token 无效或过期
     */
    public static final int INVALID_TOKEN = 2004;

    /**
     *  token 已过期
     */
    public static final int TOKEN_EXPIRED = 2005;

    /**
     * 权限不足
     */
    public static final int INSUFFICIENT_PERMISSION = 2006;

    /**
     * 角色不存在
     */
    public static final int ROLE_NOT_FOUND = 2007;

    /**
     * 用户不存在
     */
    public static final int USER_NOT_FOUND = 2008;

    // ==================== 任务管理错误码 (30xx) ====================

    /**
     * 任务不存在
     */
    public static final int TASK_NOT_FOUND = 3001;

    /**
     * 任务状态不允许分配
     */
    public static final int TASK_CANNOT_ASSIGN = 3002;

    /**
     * 任务状态不允许完成
     */
    public static final int TASK_CANNOT_COMPLETE = 3003;

    /**
     * 任务已逾期
     */
    public static final int TASK_OVERDUE = 3004;

    // ==================== 指标管理错误码 (40xx) ====================

    /**
     * 指标不存在
     */
    public static final int INDICATOR_NOT_FOUND = 4001;

    /**
     * 指标数据无效
     */
    public static final int INDICATOR_DATA_INVALID = 4002;

    /**
     * 指标计算失败
     */
    public static final int INDICATOR_CALCULATION_FAILED = 4003;

    // ==================== 文件操作错误码 (50xx) ====================

    /**
     * 文件上传失败
     */
    public static final int FILE_UPLOAD_FAILED = 5001;

    /**
     * 文件下载失败
     */
    public static final int FILE_DOWNLOAD_FAILED = 5002;

    /**
     * 文件不存在
     */
    public static final int FILE_NOT_FOUND = 5003;

    /**
     * 文件类型不支持
     */
    public static final int FILE_TYPE_NOT_SUPPORTED = 5004;

    /**
     * 文件大小超限
     */
    public static final int FILE_SIZE_EXCEEDED = 5005;

    // ==================== 技术错误码 (60xx) ====================

    /**
     * 数据库连接失败
     */
    public static final int DATABASE_CONNECTION_FAILED = 6001;

    /**
     * 缓存服务不可用
     */
    public static final int CACHE_SERVICE_UNAVAILABLE = 6002;

    /**
     * 外部服务调用失败
     */
    public static final int EXTERNAL_SERVICE_FAILED = 6003;

    /**
     * 系统限流
     */
    public static final int RATE_LIMIT_EXCEEDED = 6004;

    /**
     * 获取错误码对应的默认消息
     */
    public static String getMessage(int code) {
        switch (code) {
            case SUCCESS:
                return "Success";
            case BAD_REQUEST:
                return "Bad Request";
            case UNAUTHORIZED:
                return "Unauthorized";
            case FORBIDDEN:
                return "Forbidden";
            case NOT_FOUND:
                return "Not Found";
            case INTERNAL_ERROR:
                return "Internal Server Error";
            case BUSINESS_VALIDATION_FAILED:
                return "Business validation failed";
            case DATA_ALREADY_EXISTS:
                return "Data already exists";
            case DATA_NOT_FOUND:
                return "Data not found";
            case INVALID_STATUS:
                return "Invalid status for this operation";
            case INVALID_CREDENTIALS:
                return "Invalid username or password";
            case USER_DISABLED:
                return "User account is disabled";
            case USER_LOCKED:
                return "User account is locked";
            case INVALID_TOKEN:
                return "Invalid token";
            case TOKEN_EXPIRED:
                return "Token has expired";
            case INSUFFICIENT_PERMISSION:
                return "Insufficient permission";
            case ROLE_NOT_FOUND:
                return "Role not found";
            case USER_NOT_FOUND:
                return "User not found";
            case TASK_NOT_FOUND:
                return "Task not found";
            case TASK_CANNOT_ASSIGN:
                return "Task cannot be assigned in current status";
            case TASK_CANNOT_COMPLETE:
                return "Task cannot be completed in current status";
            case TASK_OVERDUE:
                return "Task is overdue";
            case INDICATOR_NOT_FOUND:
                return "Indicator not found";
            case INDICATOR_DATA_INVALID:
                return "Indicator data is invalid";
            case INDICATOR_CALCULATION_FAILED:
                return "Indicator calculation failed";
            case FILE_UPLOAD_FAILED:
                return "File upload failed";
            case FILE_DOWNLOAD_FAILED:
                return "File download failed";
            case FILE_NOT_FOUND:
                return "File not found";
            case FILE_TYPE_NOT_SUPPORTED:
                return "File type not supported";
            case FILE_SIZE_EXCEEDED:
                return "File size exceeded limit";
            case DATABASE_CONNECTION_FAILED:
                return "Database connection failed";
            case CACHE_SERVICE_UNAVAILABLE:
                return "Cache service unavailable";
            case EXTERNAL_SERVICE_FAILED:
                return "External service call failed";
            case RATE_LIMIT_EXCEEDED:
                return "Rate limit exceeded";
            default:
                return "Unknown error";
        }
    }
}
