package com.usanmap.usan.controller;

import com.usanmap.usan.entity.BrokerPropertyCount;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.dto.NearbyBrokerDto;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddressTriggerTemplateTests {
    @Test
    void serverRenderedPagesProvideBothTypedAddressesWithSafeAttributeEscaping() throws Exception {
        var broker = mock(BrokerPropertyCount.class);
        when(broker.getSido()).thenReturn("서울특별시");
        when(broker.getSigungu()).thenReturn("강남구");
        when(broker.getEmd()).thenReturn("신사동");
        when(broker.getAddrRoad()).thenReturn("도로명 123 \"<건물>\"");
        when(broker.getAddrJibun()).thenReturn("지번 45");
        for (String template : List.of("pages/broker.html", "pages/share/broker.html")) {
            String html = renderTriggers(template, Map.of("broker", broker));
            assertThat(html).contains("data-address-road=", "data-address-jibun=", "&quot;&lt;건물&gt;&quot;");
        }
        var nearby = new NearbyBrokerDto(UUID.randomUUID(), "사무소", "지역", "도로명 123", "지번 45", List.of());
        assertThat(renderTriggers("index.html", Map.of("broker", nearby)))
                .contains("data-address-road=\"도로명 123\"", "data-address-jibun=\"지번 45\"");
        var listing = mock(Listing.class);
        when(listing.getSido()).thenReturn("서울특별시");
        when(listing.getSigungu()).thenReturn("강남구");
        when(listing.getRoadAddress()).thenReturn("도로명 123");
        when(listing.getJibunAddress()).thenReturn("지번 45");
        when(listing.getAddressDetail()).thenReturn("101호");
        when(listing.getAddressName()).thenReturn("전체 주소");
        assertThat(renderTriggers("pages/share/listing.html", Map.of("listing", listing)))
                .contains("data-address-road=\"도로명 123 101호\"", "data-address-jibun=\"지번 45 101호\"");
    }

    private String renderTriggers(String path, Map<String, Object> values) throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("templates/" + path)) {
            String source = new String(Objects.requireNonNull(input).readAllBytes(), StandardCharsets.UTF_8);
            var matcher = Pattern.compile("<button[^>]*data-address-trigger.*?</button>", Pattern.DOTALL).matcher(source);
            StringBuilder triggers = new StringBuilder();
            while (matcher.find()) triggers.append(matcher.group());
            assertThat(triggers).isNotEmpty();
            var engine = new SpringTemplateEngine();
            engine.setTemplateResolver(new StringTemplateResolver());
            return engine.process(triggers.toString(), new Context(Locale.KOREAN, values));
        }
    }
}
