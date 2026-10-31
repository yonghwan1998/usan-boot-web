package com.usanmap.usan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.List;

@Component
public class HectoPaymentProfiles {

    private static final String TEST_PAYMENT_ACCOUNT = "test@naver.com";
    private static final String TEST_ACTION_URL = "https://tbnpg.settlebank.co.kr/card/main.do";
    private static final String PRODUCTION_ACTION_URL = "https://npg.settlebank.co.kr/card/main.do";

    private final PaymentProfile test;
    private final PaymentProfile production;

    public HectoPaymentProfiles(
            @Value("${hecto.test.mcht-id:}") String testMchtId,
            @Value("${hecto.test.hash-key:}") String testHashKey,
            @Value("${hecto.test.enc-key:}") String testEncKey,
            @Value("${hecto.test.test-mode:true}") boolean testMode,
            @Value("${hecto.production.mcht-id:}") String productionMchtId,
            @Value("${hecto.production.hash-key:}") String productionHashKey,
            @Value("${hecto.production.enc-key:}") String productionEncKey,
            @Value("${hecto.production.test-mode:false}") boolean productionTestMode
    ) {
        this.test = new PaymentProfile(
                Environment.TEST, testMchtId, testHashKey, testEncKey, testMode,
                testMode ? TEST_ACTION_URL : PRODUCTION_ACTION_URL
        );
        this.production = new PaymentProfile(
                Environment.PRODUCTION, productionMchtId, productionHashKey, productionEncKey, productionTestMode,
                productionTestMode ? TEST_ACTION_URL : PRODUCTION_ACTION_URL
        );
    }

    public boolean isTestAccount(String email) {
        return email != null && TEST_PAYMENT_ACCOUNT.equalsIgnoreCase(email);
    }

    public PaymentProfile forAccount(String email) {
        return isTestAccount(email) ? test : production;
    }

    public PaymentProfile forEnvironment(Environment environment) {
        return environment == Environment.TEST ? test : production;
    }

    public List<PaymentProfile> all() {
        return List.of(test, production);
    }

    public Optional<PaymentProfile> findByMerchantId(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return Optional.empty();
        }
        boolean matchesTest = merchantId.equals(test.mchtId());
        boolean matchesProduction = merchantId.equals(production.mchtId());
        if (matchesTest == matchesProduction) {
            return Optional.empty();
        }
        PaymentProfile matched = matchesTest ? test : production;
        return matched.hasSigningKey() ? Optional.of(matched) : Optional.empty();
    }

    public boolean matchesAccountMerchant(String email, String merchantId) {
        PaymentProfile expected = forAccount(email);
        return merchantId != null && merchantId.equals(expected.mchtId());
    }

    public enum Environment {
        TEST,
        PRODUCTION
    }

    public record PaymentProfile(
            Environment environment,
            String mchtId,
            String hashKey,
            String encKey,
            boolean testMode,
            String actionUrl
    ) {
        public boolean isConfigured() {
            return hasText(mchtId) && hasText(hashKey) && hasText(encKey);
        }

        public boolean hasSigningKey() {
            return hasText(hashKey);
        }

        public String reconciliationUrl() {
            String host = testMode ? "https://tb-nspay.settlebank.co.kr" : "https://nspay.settlebank.co.kr";
            return host + "/api/pg/" + mchtId + "/transInfo.do";
        }

        private boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
