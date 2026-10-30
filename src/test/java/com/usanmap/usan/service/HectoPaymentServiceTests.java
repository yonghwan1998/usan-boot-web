package com.usanmap.usan.service;

import com.usanmap.usan.util.HectoCryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HectoPaymentServiceTests {

    private HectoPaymentService service;

    @BeforeEach
    void setUp() {
        service = new HectoPaymentService();
        ReflectionTestUtils.setField(service, "mchtId", "merchant01");
        ReflectionTestUtils.setField(service, "hashKey", "hash-secret");
    }

    @Test
    void verifiesOfficialNotificationHashComposition() {
        Map<String, String> result = validNotification();

        assertThat(service.verifyNotificationHash(result)).isTrue();
    }

    @Test
    void rejectsNotificationForDifferentMerchant() {
        Map<String, String> result = validNotification();
        result.put("mchtId", "merchant02");

        assertThat(service.verifyNotificationHash(result)).isFalse();
    }

    @Test
    void rejectsNotificationWithoutPgTransactionNumber() {
        Map<String, String> result = validNotification();
        result.remove("trdNo");

        assertThat(service.verifyNotificationHash(result)).isFalse();
    }

    private Map<String, String> validNotification() {
        String outStatCd = "0021";
        String trdDtm = "20261031123045";
        String orderNo = "ORD-123";
        String amount = "1000";
        String hash = HectoCryptoUtil.sha256Hex(
                outStatCd + "20261031" + "123045" + "merchant01" + orderNo + amount + "hash-secret"
        );

        Map<String, String> result = new HashMap<>();
        result.put("outStatCd", outStatCd);
        result.put("trdDtm", trdDtm);
        result.put("mchtId", "merchant01");
        result.put("mchtTrdNo", orderNo);
        result.put("trdAmt", amount);
        result.put("trdNo", "TRD-123");
        result.put("pktHash", hash);
        return result;
    }
}
