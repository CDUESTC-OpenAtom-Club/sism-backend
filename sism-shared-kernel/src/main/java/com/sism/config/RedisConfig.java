package com.sism.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis配置类
 * 
 * 配置Redis连接工厂、RedisTemplate和序列化器
 * 支持通过环境变量配置Redis连接参数
 * 
 * 注意: 此配置类仅在 spring.data.redis.enabled=true 时生效
 * 
 * **Validates: Requirements 1.1, 1.6**
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {
    
    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);
    
    @Value("${spring.data.redis.host:localhost}")
    private String host;
    
    @Value("${spring.data.redis.port:6379}")
    private int port;
    
    @Value("${spring.data.redis.password:}")
    private String password;
    
    @Value("${spring.data.redis.database:0}")
    private int database;
    
    @Value("${spring.data.redis.timeout:5000}")
    private long timeout;
    
    @Value("${spring.data.redis.lettuce.pool.max-active:8}")
    private int maxActive;
    
    @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;
    
    @Value("${spring.data.redis.lettuce.pool.min-idle:0}")
    private int minIdle;
    
    @Value("${spring.data.redis.lettuce.pool.max-wait:-1}")
    private long maxWait;
    
    /**
     * 配置Redis连接工厂
     * 
     * 使用Lettuce客户端,配置连接池参数和超时时间
     * 
     * @return RedisConnectionFactory
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        log.info("Initializing Redis connection factory: host={}, port={}, database={}", 
                 host, port, database);
        
        // Redis standalone configuration
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(host);
        redisConfig.setPort(port);
        redisConfig.setDatabase(database);
        
        if (password != null && !password.isEmpty()) {
            redisConfig.setPassword(password);
        }
        
        // Socket options for connection timeout
        SocketOptions socketOptions = SocketOptions.builder()
                .connectTimeout(Duration.ofMillis(timeout))
                .build();
        
        // Client options
        ClientOptions clientOptions = ClientOptions.builder()
                .socketOptions(socketOptions)
                .build();
        
        // Lettuce client configuration with connection pooling
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .clientOptions(clientOptions)
                .build();
        
        // Create connection factory
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        factory.setValidateConnection(true);
        factory.afterPropertiesSet();
        
        // Validate connection on startup
        try {
            factory.getConnection().ping();
            log.info("Redis connection validated successfully");
        } catch (Exception e) {
            log.error("Failed to validate Redis connection", e);
            throw new IllegalStateException("Redis connection validation failed", e);
        }
        
        return factory;
    }
    
    /**
     * 配置RedisTemplate
     * 
     * 使用String序列化器作为key序列化器
     * 使用Jackson2Json序列化器作为value序列化器
     * 
     * @param connectionFactory Redis连接工厂
     * @return RedisTemplate<String, Object>
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("Configuring RedisTemplate with Jackson2Json serializer");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // String serializer for keys
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // Jackson2Json serializer for values
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonSerializer = 
                new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // Set key serializer
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Set value serializer
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        
        log.info("RedisTemplate configured successfully");
        return template;
    }
}
