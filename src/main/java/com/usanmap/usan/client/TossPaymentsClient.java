package com.usanmap.usan.client;

import com.usanmap.usan.config.TossProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TossPaymentsClient {

    private final TossProperties tossProperties;

    private static final String TOSS_API = "https://api.tosspayments.com";

    public Map<String, Object> confirmPayment(String paymentKey, String orderId, int amount) {
        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((tossProperties.getSecretKey() + ":").getBytes());

        return WebClient.builder()
                .baseUrl(TOSS_API)
                .build()
                .post()
                .uri("/v1/payments/confirm")
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("토스 결제 승인 실패: " + body)))
                )
                .bodyToMono(Map.class)
                .block();
    }
}
