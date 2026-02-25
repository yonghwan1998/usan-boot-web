package world.usan.usan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoAddressSearchResponse(List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(Address address, RoadAddress road_address, String x, String y) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(String address_name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoadAddress(String address_name, String building_name) {}
}
