package com.kidslearn.api.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSecurityConfigValidatorTest {

    @Test
    void acceptsDevProfileWithoutProductionSecrets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertDoesNotThrow(() -> new ProductionSecurityConfigValidator(environment).afterPropertiesSet());
    }

    @Test
    void rejectsProdProfileWithoutJwtSecret() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("JWT_SECRET", "");

        assertThrows(IllegalStateException.class,
            () -> new ProductionSecurityConfigValidator(environment).afterPropertiesSet());
    }

    @Test
    void rejectsProdProfileWithShortJwtSecret() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("JWT_SECRET", "short");

        assertThrows(IllegalStateException.class,
            () -> new ProductionSecurityConfigValidator(environment).afterPropertiesSet());
    }

    @Test
    void rejectsProdProfileWithWildcardCors() {
        MockEnvironment environment = prodEnvironment();
        environment.setProperty("CORS_ALLOWED_ORIGINS", "*");

        assertThrows(IllegalStateException.class,
            () -> new ProductionSecurityConfigValidator(environment).afterPropertiesSet());
    }

    @Test
    void acceptsProdProfileWithExplicitSecurityConfiguration() {
        MockEnvironment environment = prodEnvironment();

        assertDoesNotThrow(() -> new ProductionSecurityConfigValidator(environment).afterPropertiesSet());
    }

    private MockEnvironment prodEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        environment.setProperty("JWT_SECRET", "01234567890123456789012345678901");
        environment.setProperty("payment.callback.secret", "callback-secret");
        environment.setProperty("CORS_ALLOWED_ORIGINS", "https://admin.example.com,https://app.example.com");
        return environment;
    }
}
