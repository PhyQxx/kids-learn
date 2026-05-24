package com.kidslearn.api.config;

import java.util.Arrays;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionSecurityConfigValidator implements InitializingBean {

    private final Environment environment;

    public ProductionSecurityConfigValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        if (!isProdProfile()) {
            return;
        }
        requireJwtSecret();
        requireText("payment.callback.secret", "Production payment.callback.secret must be configured");
        requireExplicitCorsOrigins();
    }

    private boolean isProdProfile() {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase)
            || "prod".equalsIgnoreCase(environment.getProperty("spring.profiles.active"));
    }

    private void requireText(String key, String message) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }

    private void requireJwtSecret() {
        String value = environment.getProperty("JWT_SECRET");
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Production JWT_SECRET must be configured");
        }
        if (value.length() < 32) {
            throw new IllegalStateException("Production JWT_SECRET must be at least 32 characters");
        }
    }

    private void requireExplicitCorsOrigins() {
        String origins = environment.getProperty("CORS_ALLOWED_ORIGINS");
        if (origins == null || origins.isBlank()) {
            throw new IllegalStateException("Production CORS_ALLOWED_ORIGINS must be configured");
        }
        boolean containsWildcard = Arrays.stream(origins.split(","))
            .map(String::trim)
            .anyMatch(origin -> origin.equals("*") || origin.equalsIgnoreCase("all") || origin.contains("*"));
        if (containsWildcard) {
            throw new IllegalStateException("Production CORS_ALLOWED_ORIGINS cannot contain wildcards");
        }
    }
}
