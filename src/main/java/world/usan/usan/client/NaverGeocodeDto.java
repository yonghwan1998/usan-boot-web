package world.usan.usan.client;

import lombok.Data;

import java.util.List;

public class NaverGeocodeDto {

    @Data
    public static class Response {
        private String status;
        private List<Address> addresses;
    }

    @Data
    public static class Address {
        private String roadAddress;
        private String jibunAddress;
        private List<Element> addressElements;
        private String x;
        private String y;
    }

    @Data
    public static class Element {
        private List<String> types;
        private String longName;
    }
}
