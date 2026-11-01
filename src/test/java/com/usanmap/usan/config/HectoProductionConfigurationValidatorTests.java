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
                .withProperty("hecto.test.test-mode", "false")
                .withProperty("hecto.production.test-mode", "true")
                .withProperty("hecto.production.enc-key", "short-key")
                .withProperty("usan.base-url", "http://localhost:8080/");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hecto.test.test-mode")
                .hasMessageContaining("hecto.production.test-mode")
                .hasMessageContaining("hecto.production.enc-key")
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
                .withProperty("hecto.production.hash-key", "")
                .withProperty("hecto.production.enc-key", "do-not-print-this-secret");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hecto.production.hash-key")
                .hasMessageContaining("hecto.production.enc-key")
                .hasMessageNotContaining("do-not-print-this-secret");
    }

    @Test
    void rejectsSameMerchantIdForTestAndProduction() {
        MockEnvironment environment = validEnvironment()
                .withProperty("hecto.production.mcht-id", "test-merchant");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("상점 ID는 서로 달라야 합니다");
    }

    @Test
    void rejectsInvalidReconciliationZone() {
        MockEnvironment environment = validEnvironment()
                .withProperty("hecto.reconciliation.zone", "invalid-zone");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("올바른 시간대가 아닙니다");
    }

    @Test
    void rejectsInvalidCancellationZone() {
        MockEnvironment environment = validEnvironment()
                .withProperty("hecto.cancellation.zone", "invalid-zone");

        assertThatThrownBy(validator(environment)::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hecto.cancellation.zone");
    }

    private HectoProductionConfigurationValidator validator(MockEnvironment environment) {
        return new HectoProductionConfigurationValidator(environment);
    }

    private MockEnvironment validEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("hecto.test.mcht-id", "test-merchant")
                .withProperty("hecto.test.hash-key", "test-hash-key")
                .withProperty("hecto.test.enc-key", "12345678901234567890123456789012")
                .withProperty("hecto.test.test-mode", "true")
                .withProperty("hecto.production.mcht-id", "prod-merchant")
                .withProperty("hecto.production.hash-key", "prod-hash-key")
                .withProperty("hecto.production.enc-key", "abcdefghijklmnopqrstuvwxyz123456")
                .withProperty("hecto.production.test-mode", "false")
                .withProperty("hecto.mcht-name", "우리동네부동산")
                .withProperty("hecto.mcht-ename", "Usanmap")
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
                .withProperty("hecto.reconciliation.lookback-days", "1")
                .withProperty("hecto.reconciliation.cron", "0 0 3 * * *")
                .withProperty("hecto.reconciliation.zone", "Asia/Seoul")
                .withProperty("hecto.cancellation.zone", "Asia/Seoul")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.jpa.show-sql", "false")
                .withProperty("management.endpoint.health.show-details", "never")
                .withProperty("management.endpoints.web.exposure.include", "health,info,prometheus,metrics");
    }
}
