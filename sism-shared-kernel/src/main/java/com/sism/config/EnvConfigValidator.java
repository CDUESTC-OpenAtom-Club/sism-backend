package com.sism.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 环境变量配置验证器
 * 
 * 在应用启动时验证所有必需的环境变量是否已配置。
 * 如果缺少任何必需的环境变量，将抛出异常并阻止应用启动。
 * 
 * **Validates: Requirements 1.1.1, 1.1.2, 1.1.4**
 * 
 * @see <a href="design.md#P1">Property P1: 环境变量配置完整性</a>
 */
@Component
@Profile("!test") // 不在测试环境中运行
@Order(1) // 确保在其他 ApplicationRunner 之前执行
public class EnvConfigValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(EnvConfigValidator.class);

    /**
     * 必需的环境变量列表
     * 
     * - JWT_SECRET: JWT 签名密钥，用于生成和验证 Token
     * - DB_URL: 数据库连接 URL
     * - DB_USERNAME: 数据库用户名
     * - DB_PASSWORD: 数据库密码
     */
    public static final List<String> REQUIRED_VARS = List.of(
            "JWT_SECRET",
            "DB_URL",
            "DB_USERNAME",
            "DB_PASSWORD"
    );

    private final Environment environment;

    public EnvConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        logger.info("Validating required environment variables...");
        
        List<String> missing = findMissingVariables();
        
        if (!missing.isEmpty()) {
            String errorMessage = buildErrorMessage(missing);
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        logger.info("All required environment variables are configured.");
    }

    /**
     * 查找所有缺失的环境变量
     * 
     * @return 缺失的环境变量列表
     */
    public List<String> findMissingVariables() {
        return REQUIRED_VARS.stream()
                .filter(var -> !isVariableConfigured(var))
                .collect(Collectors.toList());
    }

    /**
     * 检查指定的环境变量是否已配置
     * 
     * 支持以下配置方式：
     * 1. 系统环境变量 (System.getenv)
     * 2. Spring Environment 属性 (application.yml 中的占位符解析)
     * 
     * @param varName 环境变量名称
     * @return 如果变量已配置且非空，返回 true
     */
    public boolean isVariableConfigured(String varName) {
        // 首先检查系统环境变量
        String envValue = System.getenv(varName);
        if (envValue != null && !envValue.isBlank()) {
            return true;
        }
        
        // 然后检查 Spring Environment 中对应的属性值
        // 根据变量名映射到实际的配置属性
        String propertyValue = getPropertyValueForEnvVar(varName);
        
        return propertyValue != null && !propertyValue.isBlank();
    }
    
    /**
     * 根据环境变量名获取对应的Spring属性值
     * 
     * @param envVarName 环境变量名称
     * @return 属性值，如果不存在返回null
     */
    private String getPropertyValueForEnvVar(String envVarName) {
        return switch (envVarName) {
            case "JWT_SECRET" -> {
                String appJwtSecret = environment.getProperty("app.jwt.secret");
                yield appJwtSecret != null ? appJwtSecret : environment.getProperty("jwt.secret");
            }
            case "DB_URL" -> environment.getProperty("spring.datasource.url");
            case "DB_USERNAME" -> environment.getProperty("spring.datasource.username");
            case "DB_PASSWORD" -> environment.getProperty("spring.datasource.password");
            default -> null;
        };
    }

    /**
     * 将环境变量名转换为 Spring 属性名格式
     * 
     * 例如：JWT_SECRET -> jwt.secret
     *       DB_URL -> db.url
     * 
     * @param envVarName 环境变量名称
     * @return Spring 属性名称
     */
    private String convertToPropertyName(String envVarName) {
        return envVarName.toLowerCase().replace('_', '.');
    }

    /**
     * 构建错误消息，列出所有缺失的环境变量
     * 
     * @param missing 缺失的环境变量列表
     * @return 格式化的错误消息
     */
    public static String buildErrorMessage(List<String> missing) {
        StringBuilder sb = new StringBuilder();
        sb.append("Missing required environment variables: ");
        sb.append(missing);
        sb.append("\n\nPlease configure the following environment variables before starting the application:\n");
        
        for (String var : missing) {
            sb.append("  - ").append(var);
            sb.append(": ").append(getVariableDescription(var));
            sb.append("\n");
        }
        
        sb.append("\nYou can set these variables in your environment or in a .env file.");
        sb.append("\nSee .env.example for reference.");
        
        return sb.toString();
    }

    /**
     * 获取环境变量的描述信息
     * 
     * @param varName 环境变量名称
     * @return 变量描述
     */
    private static String getVariableDescription(String varName) {
        return switch (varName) {
            case "JWT_SECRET" -> "JWT signing secret key (at least 256 bits / 32 characters)";
            case "DB_URL" -> "Database connection URL (e.g., jdbc:postgresql://localhost:5432/sism)";
            case "DB_USERNAME" -> "Database username";
            case "DB_PASSWORD" -> "Database password";
            default -> "Required configuration value";
        };
    }

    /**
     * 获取必需环境变量列表（用于测试）
     * 
     * @return 必需环境变量列表的副本
     */
    public static List<String> getRequiredVars() {
        return List.copyOf(REQUIRED_VARS);
    }
}
