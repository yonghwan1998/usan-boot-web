package com.usanmap.usan.service;

import com.usanmap.usan.config.HectoPaymentProfiles;
import com.usanmap.usan.util.HectoCryptoUtil;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class HectoReconciliationClient {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final WebClient webClient;

    public HectoReconciliationClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public HectoReconciliationResponse fetch(
            HectoPaymentProfiles.PaymentProfile profile,
            LocalDate targetDate
    ) {
        String transactionDate = targetDate.format(DATE_FORMAT);
        String packetHash = HectoCryptoUtil.sha256Hex(
                profile.mchtId() + transactionDate + profile.hashKey()
        );

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mchtId", profile.mchtId());
        form.add("mchtTy", "M");
        form.add("method", "ALL");
        form.add("pdTy", "D");
        form.add("trdDtSt", transactionDate);
        form.add("ezpIncYN", "Y");
        form.add("pktHash", packetHash);

        String body = webClient.post()
                .uri(profile.reconciliationUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(String.class)
                .block(REQUEST_TIMEOUT);

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("헥토 거래대사 응답이 비어 있습니다.");
        }
        return parse(body);
    }

    HectoReconciliationResponse parse(String body) {
        String[] lines = body.replace("\r", "").split("\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new IllegalArgumentException("헥토 거래대사 헤더가 없습니다.");
        }

        String[] header = lines[0].split("\\|", -1);
        if (header.length < 3) {
            throw new IllegalArgumentException("헥토 거래대사 헤더 형식이 올바르지 않습니다.");
        }
        String resultCode = header[0].trim();
        String resultMessage = header[1].trim();
        int expectedCount = parseNumber(header[2], "조회 건수");
        if ("1001".equals(resultCode)) {
            return new HectoReconciliationResponse(resultCode, resultMessage, List.of());
        }
        if (!"0000".equals(resultCode)) {
            throw new IllegalStateException("헥토 거래대사 조회 실패: " + resultCode + " " + resultMessage);
        }

        List<HectoReconciliationTransaction> transactions = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            transactions.add(parseTransaction(lines[index]));
        }
        if (transactions.size() != expectedCount) {
            throw new IllegalStateException("헥토 거래대사 응답 건수가 일치하지 않습니다.");
        }
        return new HectoReconciliationResponse(resultCode, resultMessage, List.copyOf(transactions));
    }

    private HectoReconciliationTransaction parseTransaction(String line) {
        String[] fields = line.split("\\|", -1);
        if (fields.length < 8) {
            throw new IllegalArgumentException("헥토 거래대사 거래내역 형식이 올바르지 않습니다.");
        }
        HectoReconciliationTransaction.Type type = switch (fields[4]) {
            case "0" -> HectoReconciliationTransaction.Type.APPROVAL;
            case "1" -> HectoReconciliationTransaction.Type.CANCELLATION;
            default -> throw new IllegalArgumentException("알 수 없는 헥토 거래 구분입니다.");
        };
        return new HectoReconciliationTransaction(
                fields[0],
                fields[1],
                required(fields[2], "거래번호"),
                required(fields[3], "주문번호"),
                type,
                LocalDate.parse(fields[5], DATE_FORMAT),
                LocalTime.parse(fields[6], TIME_FORMAT),
                parseNumber(fields[7], "거래금액"),
                fields.length > 8 ? fields[8] : ""
        );
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("헥토 거래대사 " + name + " 값이 없습니다.");
        }
        return value;
    }

    private int parseNumber(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("헥토 거래대사 " + name + " 형식이 올바르지 않습니다.", e);
        }
    }
}
