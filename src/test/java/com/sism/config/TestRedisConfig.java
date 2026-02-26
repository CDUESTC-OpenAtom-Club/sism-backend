package com.sism.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
 * Test Redis Configuration
 * 
 * This configuration is used for testing purposes only.
 * It does not have @ConditionalOnProperty, so it will always be loaded in tests.
 * 
 * @author SISM Team
 * @since 1.0.0
 */
@TestConfiguration
@EnableCaching
public class TestRedisConfig {
    
    private static final Logger log = LoggerFactory.getLogger(TestRedisConfig.class);
    
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
    
    /**
     * Configure Redis connection factory for tests
     * 
     * @return RedisConnectionFactory
     */
    @Bean
    @Primary
    public RedisConnectionFactory testRedisConnectionFactory() {
        log.info("Initializing Test Redis connection factory: host={}, port={}, database={}", 
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
        
        // Lettuce client configuration
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
            log.info("Test Redis connection validated successfully");
        } catch (Exception e) {
            log.error("Failed to validate Test Redis connection", e);
            throw new IllegalStateException("Test Redis connection validation failed", e);
        }
        
        return factory;
    }
    
    /**
     * Configure RedisTemplate for tests
     * 
     * @param connectionFactory Redis connection factory
     * @return RedisTemplate<String, Object>
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> testRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("Configuring Test RedisTemplate with Jackson2Json serializer");
        
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
        
        log.info("Test RedisTemplate configured successfully");
        return template;
    }
}
