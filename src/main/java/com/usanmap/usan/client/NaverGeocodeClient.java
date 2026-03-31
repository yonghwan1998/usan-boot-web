package com.usanmap.usan.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class NaverGeocodeClient {

    private final WebClient webClient;

    public NaverGeocodeClient(
            @Value("${naver.geocode.base-url}") String baseUrl,
            @Value("${naver.geocode.client-id}") String clientId,
            @Value("${naver.geocode.client-secret}") String clientSecret
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .defaultHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .build();
    }

    public Mono<NaverGeocodeDto.Response> geocode(String query) {
        return webClient.get()
                .uri(uri -> uri.path("/map-geocode/v2/geocode")
                        .queryParam("query", query)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, r -> r.createException().flatMap(Mono::error))
                .bodyToMono(NaverGeocodeDto.Response.class);
    }
}
