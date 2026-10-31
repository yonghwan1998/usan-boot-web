package com.usanmap.usan.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HectoPaymentProfilesTests {

    @Test
    void selectsTestProfileOnlyForConfiguredTestAccount() {
        HectoPaymentProfiles profiles = profiles("test-merchant", "prod-merchant");

        assertThat(profiles.forAccount("TEST@NAVER.COM").environment())
                .isEqualTo(HectoPaymentProfiles.Environment.TEST);
        assertThat(profiles.forAccount("member@example.com").environment())
                .isEqualTo(HectoPaymentProfiles.Environment.PRODUCTION);
    }

    @Test
    void resolvesNotificationProfileByMerchantId() {
        HectoPaymentProfiles profiles = profiles("test-merchant", "prod-merchant");

        assertThat(profiles.findByMerchantId("test-merchant"))
                .get().extracting(HectoPaymentProfiles.PaymentProfile::environment)
                .isEqualTo(HectoPaymentProfiles.Environment.TEST);
        assertThat(profiles.findByMerchantId("unknown")).isEmpty();
    }

    @Test
    void failsClosedWhenMerchantIdsAreDuplicated() {
        HectoPaymentProfiles profiles = profiles("same-merchant", "same-merchant");

        assertThat(profiles.findByMerchantId("same-merchant")).isEmpty();
    }

    @Test
    void matchesNotificationMerchantToAccountEnvironment() {
        HectoPaymentProfiles profiles = profiles("test-merchant", "prod-merchant");

        assertThat(profiles.matchesAccountMerchant("test@naver.com", "test-merchant")).isTrue();
        assertThat(profiles.matchesAccountMerchant("member@example.com", "test-merchant")).isFalse();
        assertThat(profiles.matchesAccountMerchant("member@example.com", "prod-merchant")).isTrue();
    }

    private HectoPaymentProfiles profiles(String testMerchantId, String productionMerchantId) {
        return new HectoPaymentProfiles(
                testMerchantId, "test-hash", "12345678901234567890123456789012", true,
                productionMerchantId, "prod-hash", "abcdefghijklmnopqrstuvwxyz123456", false
        );
    }
}
