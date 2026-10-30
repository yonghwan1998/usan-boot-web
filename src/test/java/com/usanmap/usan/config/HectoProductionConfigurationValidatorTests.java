package com.usanmap.usan.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HectoProductionConfigurationValidatorTests {

    @Test
    void acceptsSafeProductionPaymentConfiguration() {
        HectoProductionConfigurationValidator validator = validator(validEnvironment());

        assertThatCode(validator::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void rejectsTestModeInsecureCallbackAndInvalidEncryptionKey() {
        MockEnvironment environment = validEnvironment()
                .withProperty("hecto.test-mode", "true")
                .withProperty("hecto.enc-key", "short-key")
                .withProperty("usan.base-url", "http://localhost:8080/");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hecto.test-mode")
                .hasMessageContaining("hecto.enc-key")
                .hasMessageContaining("https")
                .hasMessageContaining("로컬 호스트")
                .hasMessageContaining("슬래시");
    }

    @Test
    void rejectsInvalidRetryAndExpirationRanges() {
        MockEnvironment environment = validEnvironment()
                .withProperty("hecto.notification.batch-size", "0")
                .withProperty("hecto.notification.retry-base-seconds", "60")
                .withProperty("hecto.notification.retry-max-seconds", "30")
                .withProperty("hecto.payment.ready-expiration-minutes", "invalid");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("batch-size")
                .hasMessageContaining("retry-max-seconds")
                .hasMessageContaining("ready-expiration-minutes");
    }

    @Test
    void rejectsMixedProductionAndDevelopmentProfiles() {
        MockEnvironment environment = validEnvironment();
        environment.setActiveProfiles("prod", "dev");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("함께 활성화할 수 없습니다");
    }

    @Test
    void rejectsUnsafePersistenceAndMonitoringConfiguration() {
        MockEnvironment environment = validEnvironment()
                .withProperty("spring.jpa.hibernate.ddl-auto", "update")
                .withProperty("spring.jpa.show-sql", "true")
                .withProperty("management.endpoint.health.show-details", "always")
                .withProperty("management.endpoints.web.exposure.include", "health,info");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ddl-auto")
                .hasMessageContaining("show-sql")
                .hasMessageContaining("health")
                .hasMessageContaining("prometheus");
    }

    @Test
    void validationErrorsDoNotExposeSecretValues() {
        MockEnvironment environment = validEnvironment()
                .withProperty("hecto.hash-key", "")
                .withProperty("hecto.enc-key", "do-not-print-this-secret");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hecto.hash-key")
                .hasMessageContaining("hecto.enc-key")
                .hasMessageNotContaining("do-not-print-this-secret");
    }

    private HectoProductionConfigurationValidator validator(MockEnvironment environment) {
        return new HectoProductionConfigurationValidator(environment);
    }

    private MockEnvironment validEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("hecto.mcht-id", "merchant01")
                .withProperty("hecto.hash-key", "hash-key")
                .withProperty("hecto.enc-key", "12345678901234567890123456789012")
                .withProperty("hecto.mcht-name", "우리동네부동산")
                .withProperty("hecto.mcht-ename", "Usanmap")
                .withProperty("hecto.test-mode", "false")
                .withProperty("usan.base-url", "https://usan.example.com")
                .withProperty("hecto.notification.processing-delay-ms", "1000")
                .withProperty("hecto.notification.initial-delay-ms", "1000")
                .withProperty("hecto.notification.batch-size", "20")
                .withProperty("hecto.notification.max-attempts", "8")
                .withProperty("hecto.notification.retry-base-seconds", "30")
                .withProperty("hecto.notification.retry-max-seconds", "3600")
                .withProperty("hecto.payment.ready-expiration-minutes", "30")
                .withProperty("hecto.payment.expiration-delay-ms", "60000")
                .withProperty("hecto.payment.expiration-initial-delay-ms", "60000")
                .withProperty("hecto.payment.expiration-batch-size", "100")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.jpa.show-sql", "false")
                .withProperty("management.endpoint.health.show-details", "never")
                .withProperty("management.endpoints.web.exposure.include", "health,info,prometheus,metrics");
    }
}
