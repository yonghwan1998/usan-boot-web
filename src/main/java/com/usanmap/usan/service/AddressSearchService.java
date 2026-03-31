package com.usanmap.usan.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.usanmap.usan.dto.KakaoAddressSearchResponse;

import java.util.List;
import java.util.Map;

@Service
public class AddressSearchService {

    private final WebClient kakaoWebClient;

    public AddressSearchService(WebClient kakaoWebClient) {
        this.kakaoWebClient = kakaoWebClient;
    }

    public List<Map<String, Object>> search(String q) {

        KakaoAddressSearchResponse resp = kakaoWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/address.json")
                        .queryParam("query", q)
                        .queryParam("size", 10)
                        .build())
                .retrieve()
                .bodyToMono(KakaoAddressSearchResponse.class)
                .block();

        if (resp == null || resp.documents() == null) return List.of();

        return resp.documents().stream().map(d -> {
            String roadAddr = d.road_address() != null ? d.road_address().address_name() : null;
            String jibunAddr = d.address() != null ? d.address().address_name() : null;
            String buildingName = (d.road_address() != null) ? d.road_address().building_name() : null;

            String name = (buildingName != null && !buildingName.isBlank())
                    ? buildingName
                    : (roadAddr != null ? roadAddr : (jibunAddr != null ? jibunAddr : ""));

            double lng = d.x() != null ? Double.parseDouble(d.x()) : 0.0;
            double lat = d.y() != null ? Double.parseDouble(d.y()) : 0.0;

            Map<String, Object> m = new java.util.HashMap<>();
            m.put("name", name);
            m.put("buildingName", buildingName == null ? "" : buildingName);
            m.put("roadAddress", roadAddr == null ? "" : roadAddr);
            m.put("jibunAddress", jibunAddr == null ? "" : jibunAddr);
            m.put("lat", lat);
            m.put("lng", lng);

            return m;
        }).toList();
    }
}
