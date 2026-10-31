package com.usanmap.usan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.util.HectoCryptoUtil;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HectoCancellationClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HectoCancellationClient client =
            new HectoCancellationClient(WebClient.builder(), objectMapper);
    private final HectoPaymentProfiles.PaymentProfile profile = new HectoPaymentProfiles.PaymentProfile(
            HectoPaymentProfiles.Environment.TEST,
            "nxca_jt_il",
            "ST1009281328226982205",
            "pgSettle30y739r82jtd709yOfZ2yK5K",
            true,
            "https://tbnpg.settlebank.co.kr/card/main.do"
    );
    private final HectoCancellationClient.CancellationCommand command =
            new HectoCancellationClient.CancellationCommand(
                    "CANCEL20261031120000ABCDEF123456",
                    "STFP_PGCA_ORIGINAL",
                    1000,
                    "고객 요청",
                    LocalDateTime.of(2026, 10, 31, 12, 0, 0)
            );

    @Test
    void buildsOfficialCardCancellationPayload() {
        HectoCancellationClient.RequestPayload payload = client.buildPayload(profile, command);

        @SuppressWarnings("unchecked")
        Map<String, String> params = (Map<String, String>) payload.body().get("params");
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) payload.body().get("data");

        assertThat(params).containsEntry("method", "CA")
                .containsEntry("bizType", "C0")
                .containsEntry("ver", "0A19")
                .containsEntry("mchtTrdNo", command.cancellationNo());
        assertThat(data).containsEntry("orgTrdNo", command.originalTransactionNo())
                .containsEntry("cnclOrd", "001")
                .containsEntry("crcCd", "KRW")
                .containsEntry("pktHash", payload.packetHash());
        assertThat(HectoCryptoUtil.decryptAes256Ecb(data.get("cnclAmt"), profile.encKey()))
                .isEqualTo("1000");
        assertThat(payload.packetHash()).isEqualTo(HectoCryptoUtil.sha256Hex(
                "20261031" + "120000" + profile.mchtId() + command.cancellationNo()
                        + "1000" + profile.hashKey()
        ));
    }

    @Test
    void acceptsOnlyMatchingSuccessfulResponse() throws Exception {
        HectoCancellationClient.RequestPayload payload = client.buildPayload(profile, command);
        String response = successResponse(payload.packetHash());

        HectoCancellationClient.CancellationResponse result =
                client.parseResponse(profile, command, payload.packetHash(), response);

        assertThat(result.success()).isTrue();
        assertThat(result.resultCode()).isEqualTo("0000");
        assertThat(result.cancellationTransactionNo()).isEqualTo("STFP_CANCEL_1");
        assertThat(result.auditJson()).doesNotContain("cnclAmt");
    }

    @Test
    void rejectsTamperedSuccessfulResponseHash() throws Exception {
        String response = successResponse("tampered");

        assertThatThrownBy(() -> client.parseResponse(profile, command, "expected", response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("해시");
    }

    @Test
    void returnsExplicitProviderFailureWithoutDecryptingAmount() throws Exception {
        String response = objectMapper.writeValueAsString(Map.of(
                "params", Map.of(
                        "mchtId", profile.mchtId(),
                        "mchtTrdNo", command.cancellationNo(),
                        "outStatCd", "0031",
                        "outRsltCd", "9999",
                        "outRsltMsg", "취소 거절"
                )
        ));

        HectoCancellationClient.CancellationResponse result =
                client.parseResponse(profile, command, "request-hash", response);

        assertThat(result.success()).isFalse();
        assertThat(result.resultCode()).isEqualTo("9999");
    }

    private String successResponse(String packetHash) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "params", Map.of(
                        "mchtId", profile.mchtId(),
                        "mchtTrdNo", command.cancellationNo(),
                        "trdNo", "STFP_CANCEL_1",
                        "outStatCd", "0021",
                        "outRsltCd", "0000",
                        "outRsltMsg", "정상처리"
                ),
                "data", Map.of(
                        "orgTrdNo", command.originalTransactionNo(),
                        "cnclAmt", HectoCryptoUtil.encryptAes256Ecb("1000", profile.encKey()),
                        "pktHash", packetHash
                )
        ));
    }
}
