package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.entity.CreditOrder;
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
        HectoPaymentProfiles profiles = new HectoPaymentProfiles(
                "test-merchant", "test-hash", "12345678901234567890123456789012", true,
                "prod-merchant", "prod-hash", "abcdefghijklmnopqrstuvwxyz123456", false
        );
        service = new HectoPaymentService(profiles);
        ReflectionTestUtils.setField(service, "mchtName", "우리동네부동산");
        ReflectionTestUtils.setField(service, "mchtEname", "Usanmap");
        ReflectionTestUtils.setField(service, "baseUrl", "https://usan.example.com");
    }

    @Test
    void verifiesOfficialNotificationHashComposition() {
        Map<String, String> result = validNotification();

        assertThat(service.verifyNotificationHash(result)).isTrue();
    }

    @Test
    void rejectsNotificationForDifferentMerchant() {
        Map<String, String> result = validNotification();
        result.put("mchtId", "unknown-merchant");

        assertThat(service.verifyNotificationHash(result)).isFalse();
    }

    @Test
    void rejectsNotificationWithoutPgTransactionNumber() {
        Map<String, String> result = validNotification();
        result.remove("trdNo");

        assertThat(service.verifyNotificationHash(result)).isFalse();
    }

    @Test
    void buildsTestPaymentWithTestProfileForTestAccount() {
        CreditOrder order = paymentOrder();

        HectoPaymentService.PaymentRequest request = service.buildPaymentRequest(order, "test@naver.com");

        assertThat(request.environment()).isEqualTo(HectoPaymentProfiles.Environment.TEST);
        assertThat(request.actionUrl()).isEqualTo("https://tbnpg.settlebank.co.kr/card/main.do");
        assertThat(request.params().get("mchtId")).isEqualTo("test-merchant");
    }

    @Test
    void buildsProductionPaymentWithProductionProfileForRegularAccount() {
        CreditOrder order = paymentOrder();

        HectoPaymentService.PaymentRequest request = service.buildPaymentRequest(order, "member@example.com");

        assertThat(request.environment()).isEqualTo(HectoPaymentProfiles.Environment.PRODUCTION);
        assertThat(request.actionUrl()).isEqualTo("https://npg.settlebank.co.kr/card/main.do");
        assertThat(request.params().get("mchtId")).isEqualTo("prod-merchant");
    }

    @Test
    void verifiesProductionNotificationWithProductionHashKey() {
        Map<String, String> result = validNotification();
        result.put("mchtId", "prod-merchant");
        result.put("pktHash", HectoCryptoUtil.sha256Hex(
                "0021" + "20261031" + "123045" + "prod-merchant" + "ORD-123" + "1000" + "prod-hash"
        ));

        assertThat(service.verifyNotificationHash(result)).isTrue();
    }

    @Test
    void rejectsNotificationSignedWithOtherEnvironmentKey() {
        Map<String, String> result = validNotification();
        result.put("pktHash", HectoCryptoUtil.sha256Hex(
                "0021" + "20261031" + "123045" + "test-merchant" + "ORD-123" + "1000" + "prod-hash"
        ));

        assertThat(service.verifyNotificationHash(result)).isFalse();
    }

    private Map<String, String> validNotification() {
        String outStatCd = "0021";
        String trdDtm = "20261031123045";
        String orderNo = "ORD-123";
        String amount = "1000";
        String hash = HectoCryptoUtil.sha256Hex(
                outStatCd + "20261031" + "123045" + "test-merchant" + orderNo + amount + "test-hash"
        );

        Map<String, String> result = new HashMap<>();
        result.put("outStatCd", outStatCd);
        result.put("trdDtm", trdDtm);
        result.put("mchtId", "test-merchant");
        result.put("mchtTrdNo", orderNo);
        result.put("trdAmt", amount);
        result.put("trdNo", "TRD-123");
        result.put("pktHash", hash);
        return result;
    }

    private CreditOrder paymentOrder() {
        return CreditOrder.builder()
                .orderNo("ORD-123")
                .productNameSnapshot("100 크레딧")
                .priceAmountSnapshot(1000)
                .build();
    }
}
