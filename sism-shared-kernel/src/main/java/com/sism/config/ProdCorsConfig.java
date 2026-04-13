package com.sism.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Production CORS configuration.
 *
 * Production must explicitly declare allowed origins so localhost defaults never
 * leak into deployed environments.
 */
@Configuration
@Profile("prod")
public class ProdCorsConfig {

    private static final Logger log = LoggerFactory.getLogger(ProdCorsConfig.class);

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${app.cors.allowed-headers:Authorization,Content-Type,Accept,X-Requested-With,X-Timestamp,X-Signature}")
    private String allowedHeaders;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsFilter corsFilter() {
        if (!StringUtils.hasText(allowedOrigins)) {
            throw new IllegalStateException("Production CORS origins must be configured via app.cors.allowed-origins");
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(allowCredentials);

        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        for (String origin : origins) {
            String trimmedOrigin = origin.trim();
            if (trimmedOrigin.isEmpty()) {
                continue;
            }
            if ("*".equals(trimmedOrigin) && allowCredentials) {
                log.error("Wildcard CORS origin '*' cannot be used when credentials are enabled");
                throw new IllegalStateException("Cannot use wildcard CORS origin with credentials enabled");
            }
            if (trimmedOrigin.contains("*")) {
                config.addAllowedOriginPattern(trimmedOrigin);
            } else {
                config.addAllowedOrigin(trimmedOrigin);
            }
        }

        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        config.setExposedHeaders(Arrays.asList("Authorization", "X-Request-ID", "Content-Disposition"));
        config.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
