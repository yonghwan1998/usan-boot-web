package com.usanmap.usan.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.ZoneId;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class HectoProductionConfigurationValidator implements InitializingBean {

    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        List<String> errors = new ArrayList<>();

        validateActiveProfiles(errors);
        validatePaymentProfile(errors, "hecto.test", true);
        validatePaymentProfile(errors, "hecto.production", false);
        validateDistinctMerchantIds(errors);
        validateRequired(errors, "hecto.mcht-name");
        validateRequired(errors, "hecto.mcht-ename");
        validateBaseUrl(errors);
        validateNotificationSettings(errors);
        validateExpirationSettings(errors);
        validateReconciliationSettings(errors);
        validateOperationalSafety(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("운영 결제 설정이 올바르지 않습니다: " + String.join("; ", errors));
        }

        log.info("payment_production_configuration_validated testMode=true productionMode=false callbackScheme=https");
    }

    private void validateActiveProfiles(List<String> errors) {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        if (activeProfiles.contains("dev") || activeProfiles.contains("test")) {
            errors.add("prod 프로필과 dev/test 프로필을 함께 활성화할 수 없습니다");
        }
    }

    private void validateRequired(List<String> errors, String key) {
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            errors.add(key + " 값이 필요합니다");
        }
    }

    private void validatePaymentProfile(List<String> errors, String prefix, boolean expectedTestMode) {
        validateRequired(errors, prefix + ".mcht-id");
        validateRequired(errors, prefix + ".hash-key");
        validateRequired(errors, prefix + ".enc-key");
        validateEncryptionKey(errors, prefix + ".enc-key");

        String testMode = getProperty(prefix + ".test-mode");
        if (!String.valueOf(expectedTestMode).equalsIgnoreCase(testMode)) {
            errors.add(prefix + ".test-mode는 " + expectedTestMode + "여야 합니다");
        }
    }

    private void validateEncryptionKey(List<String> errors, String key) {
        String encKey = getProperty(key);
        if (encKey != null && !encKey.isBlank()
                && encKey.getBytes(StandardCharsets.UTF_8).length != 32) {
            errors.add(key + "는 UTF-8 기준 32바이트여야 합니다");
        }
    }

    private void validateDistinctMerchantIds(List<String> errors) {
        String testMerchantId = getProperty("hecto.test.mcht-id");
        String productionMerchantId = getProperty("hecto.production.mcht-id");
        if (testMerchantId != null && !testMerchantId.isBlank()
                && testMerchantId.equals(productionMerchantId)) {
            errors.add("테스트와 운영 헥토 상점 ID는 서로 달라야 합니다");
        }
    }

    private void validateBaseUrl(List<String> errors) {
        String baseUrl = getProperty("usan.base-url");
        if (baseUrl == null || baseUrl.isBlank()) {
            errors.add("usan.base-url 값이 필요합니다");
            return;
        }

        try {
            URI uri = URI.create(baseUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                errors.add("운영 usan.base-url은 https URL이어야 합니다");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                errors.add("usan.base-url에 유효한 호스트가 필요합니다");
            } else if (isLocalHost(uri.getHost())) {
                errors.add("운영 usan.base-url에는 로컬 호스트를 사용할 수 없습니다");
            }
            if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                errors.add("usan.base-url에는 사용자 정보, 쿼리 또는 fragment를 포함할 수 없습니다");
            }
            if (uri.getPath() != null && !uri.getPath().isBlank()) {
                errors.add("usan.base-url에는 경로를 포함할 수 없습니다");
            }
            if (baseUrl.endsWith("/")) {
                errors.add("usan.base-url 끝에는 슬래시를 입력하지 않습니다");
            }
        } catch (IllegalArgumentException e) {
            errors.add("usan.base-url 형식이 올바르지 않습니다");
        }
    }

    private boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host)
                || "127.0.0.1".equals(host)
                || "0.0.0.0".equals(host)
                || "::1".equals(host);
    }

    private void validateNotificationSettings(List<String> errors) {
        validatePositive(errors, "hecto.notification.processing-delay-ms");
        validateNonNegative(errors, "hecto.notification.initial-delay-ms");
        validatePositive(errors, "hecto.notification.batch-size");
        validatePositive(errors, "hecto.notification.max-attempts");
        Long retryBaseSeconds = validatePositive(errors, "hecto.notification.retry-base-seconds");
        Long retryMaxSeconds = validatePositive(errors, "hecto.notification.retry-max-seconds");
        if (retryBaseSeconds != null && retryMaxSeconds != null && retryMaxSeconds < retryBaseSeconds) {
            errors.add("hecto.notification.retry-max-seconds는 retry-base-seconds 이상이어야 합니다");
        }
    }

    private void validateExpirationSettings(List<String> errors) {
        validatePositive(errors, "hecto.payment.ready-expiration-minutes");
        validatePositive(errors, "hecto.payment.expiration-delay-ms");
        validateNonNegative(errors, "hecto.payment.expiration-initial-delay-ms");
        validatePositive(errors, "hecto.payment.expiration-batch-size");
    }

    private void validateReconciliationSettings(List<String> errors) {
        validatePositive(errors, "hecto.reconciliation.lookback-days");
        validateRequired(errors, "hecto.reconciliation.cron");
        validateRequired(errors, "hecto.reconciliation.zone");
        String zone = getProperty("hecto.reconciliation.zone");
        if (zone != null && !zone.isBlank()) {
            try {
                ZoneId.of(zone);
            } catch (Exception e) {
                errors.add("hecto.reconciliation.zone 값이 올바른 시간대가 아닙니다");
            }
        }
    }

    private void validateOperationalSafety(List<String> errors) {
        if (!"validate".equalsIgnoreCase(getProperty("spring.jpa.hibernate.ddl-auto"))) {
            errors.add("운영 spring.jpa.hibernate.ddl-auto는 validate여야 합니다");
        }
        if ("true".equalsIgnoreCase(getProperty("spring.jpa.show-sql"))) {
            errors.add("운영 spring.jpa.show-sql은 false여야 합니다");
        }
        if ("always".equalsIgnoreCase(getProperty("management.endpoint.health.show-details"))) {
            errors.add("운영 health 상세 정보는 always로 공개할 수 없습니다");
        }

        String exposure = getProperty("management.endpoints.web.exposure.include");
        if (exposure == null || Arrays.stream(exposure.split(","))
                .map(String::trim)
                .noneMatch(value -> "*".equals(value) || "prometheus".equalsIgnoreCase(value))) {
            errors.add("운영 actuator 노출 목록에 prometheus가 필요합니다");
        }
    }

    private Long validatePositive(List<String> errors, String key) {
        Long value = parseLong(errors, key);
        if (value != null && value <= 0) {
            errors.add(key + " 값은 0보다 커야 합니다");
        }
        return value;
    }

    private void validateNonNegative(List<String> errors, String key) {
        Long value = parseLong(errors, key);
        if (value != null && value < 0) {
            errors.add(key + " 값은 0 이상이어야 합니다");
        }
    }

    private Long parseLong(List<String> errors, String key) {
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            errors.add(key + " 값이 필요합니다");
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            errors.add(key + " 값은 정수여야 합니다");
            return null;
        }
    }

    private String getProperty(String key) {
        try {
            return environment.getProperty(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
