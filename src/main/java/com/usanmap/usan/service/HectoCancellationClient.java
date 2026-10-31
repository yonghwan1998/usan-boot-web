package com.usanmap.usan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.util.HectoCryptoUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class HectoCancellationClient {

    private static final String VERSION = "0A19";
    private static final String METHOD = "CA";
    private static final String BUSINESS_TYPE = "C0";
    private static final String ENCRYPTION_CODE = "23";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public HectoCancellationClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public CancellationResponse cancel(HectoPaymentProfiles.PaymentProfile profile, CancellationCommand command) {
        RequestPayload payload = buildPayload(profile, command);
        String responseBody = webClient.post()
                .uri(profile.cancellationUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload.body())
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);

        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("헥토 결제 취소 응답이 비어 있습니다.");
        }
        return parseResponse(profile, command, payload.packetHash(), responseBody);
    }

    RequestPayload buildPayload(HectoPaymentProfiles.PaymentProfile profile, CancellationCommand command) {
        String date = command.requestedAt().format(DATE_FORMAT);
        String time = command.requestedAt().format(TIME_FORMAT);
        String amount = String.valueOf(command.amount());
        String packetHash = HectoCryptoUtil.sha256Hex(
                date + time + profile.mchtId() + command.cancellationNo() + amount + profile.hashKey()
        );

        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchtId", profile.mchtId());
        params.put("ver", VERSION);
        params.put("method", METHOD);
        params.put("bizType", BUSINESS_TYPE);
        params.put("encCd", ENCRYPTION_CODE);
        params.put("mchtTrdNo", command.cancellationNo());
        params.put("trdDt", date);
        params.put("trdTm", time);
        params.put("mobileYn", "N");
        params.put("osType", "W");

        Map<String, String> data = new LinkedHashMap<>();
        data.put("orgTrdNo", command.originalTransactionNo());
        data.put("cnclAmt", HectoCryptoUtil.encryptAes256Ecb(amount, profile.encKey()));
        data.put("crcCd", "KRW");
        data.put("cnclOrd", "001");
        data.put("cnclRsn", command.reason());
        data.put("pktHash", packetHash);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("params", params);
        body.put("data", data);
        return new RequestPayload(body, packetHash);
    }

    CancellationResponse parseResponse(HectoPaymentProfiles.PaymentProfile profile, CancellationCommand command,
                                       String requestHash, String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode params = root.path("params");
            JsonNode data = root.path("data");
            String statusCode = text(params, "outStatCd");
            String resultCode = text(params, "outRsltCd");
            String resultMessage = text(params, "outRsltMsg");

            validateEcho(text(params, "mchtId"), profile.mchtId(), "상점아이디");
            validateEcho(text(params, "mchtTrdNo"), command.cancellationNo(), "취소 주문번호");

            boolean success = "0021".equals(statusCode) && "0000".equals(resultCode);
            if (success) {
                validateEcho(text(data, "orgTrdNo"), command.originalTransactionNo(), "원거래번호");
                validateHash(text(data, "pktHash"), requestHash);
                String decryptedAmount = HectoCryptoUtil.decryptAes256Ecb(required(data, "cnclAmt"), profile.encKey());
                if (!String.valueOf(command.amount()).equals(decryptedAmount)) {
                    throw new IllegalStateException("헥토 결제 취소 응답 금액이 일치하지 않습니다.");
                }
            }

            Map<String, String> audit = new LinkedHashMap<>();
            audit.put("mchtId", text(params, "mchtId"));
            audit.put("mchtTrdNo", text(params, "mchtTrdNo"));
            audit.put("trdNo", text(params, "trdNo"));
            audit.put("outStatCd", statusCode);
            audit.put("outRsltCd", resultCode);
            audit.put("outRsltMsg", resultMessage);
            audit.put("orgTrdNo", text(data, "orgTrdNo"));
            return new CancellationResponse(
                    success, statusCode, resultCode, resultMessage, text(params, "trdNo"),
                    objectMapper.writeValueAsString(audit)
            );
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("헥토 결제 취소 응답을 확인할 수 없습니다.", e);
        }
    }

    private void validateEcho(String actual, String expected, String fieldName) {
        if (actual.isBlank() || !actual.equals(expected)) {
            throw new IllegalStateException("헥토 결제 취소 응답 " + fieldName + "가 일치하지 않습니다.");
        }
    }

    private void validateHash(String actual, String expected) {
        if (actual.isBlank() || !MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalStateException("헥토 결제 취소 응답 해시가 일치하지 않습니다.");
        }
    }

    private String required(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        if (value.isBlank()) {
            throw new IllegalStateException("헥토 결제 취소 응답에 " + fieldName + " 값이 없습니다.");
        }
        return value;
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asText("").trim();
    }

    public record CancellationCommand(
            String cancellationNo,
            String originalTransactionNo,
            int amount,
            String reason,
            LocalDateTime requestedAt
    ) {}

    public record CancellationResponse(
            boolean success,
            String statusCode,
            String resultCode,
            String resultMessage,
            String cancellationTransactionNo,
            String auditJson
    ) {}

    record RequestPayload(Map<String, Object> body, String packetHash) {}
}
