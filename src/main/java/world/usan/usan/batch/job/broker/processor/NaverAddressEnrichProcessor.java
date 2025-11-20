package world.usan.usan.batch.job.broker.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import world.usan.usan.batch.job.broker.reader.BrokerRow;
import world.usan.usan.client.NaverGeocodeClient;
import world.usan.usan.client.NaverGeocodeDto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NaverAddressEnrichProcessor implements ItemProcessor<BrokerRow, EnrichedBrokerItem> {

    private final NaverGeocodeClient client;

    @Override
    public EnrichedBrokerItem process(BrokerRow row) {

        try {
            if (row.getAddress() == null || row.getAddress().isBlank()) {
                log.warn("[geocode skip] empty address, regno={} name={}", row.getRegistrationNumber(), row.getBrokerName());
                return null;
            }

            NaverGeocodeDto.Response resp = client.geocode(row.getAddress())
                    .blockOptional()
                    .orElse(null);
            if (resp == null || resp.getAddresses() == null || resp.getAddresses().isEmpty()) {
                log.warn("[geocode miss] {}", row.getAddress());
                return null;
            }

            NaverGeocodeDto.Address a = resp.getAddresses().get(0);
            Map<String, String> m = extract(a.getAddressElements());

            return EnrichedBrokerItem.builder()
                    .listingType(row.getListingType())
                    .brokerName(row.getBrokerName())
                    .officeName(row.getOfficeName())
                    .registrationNumber(row.getRegistrationNumber())
                    .tel(row.getTel())
                    .phone(row.getPhone())
                    .sido(m.getOrDefault("SIDO", ""))
                    .sigungu(m.getOrDefault("SIGUGUN", ""))
                    .dongmyun(m.getOrDefault("DONGMYUN", ""))
                    .addrRoad(a.getRoadAddress() == null ? "" : a.getRoadAddress())
                    .addrJibun(a.getJibunAddress() == null ? "" : a.getJibunAddress())
                    .lat(new BigDecimal(a.getY()))
                    .lng(new BigDecimal(a.getX()))
                    .build();
        } catch (Exception e) {
            log.error("[geocode error] {} => {}", row.getAddress(), e.toString());
            return null;
        }
    }

    private Map<String, String> extract(List<NaverGeocodeDto.Element> els) {

        Map<String, String> m = new HashMap<>();

        if (els == null) {
            return m;
        }

        for (var e : els) {
            if (e.getTypes() == null) continue;
            if (e.getTypes().contains("SIDO")) m.put("SIDO", e.getLongName());
            if (e.getTypes().contains("SIGUGUN")) m.put("SIGUGUN", e.getLongName());
            if (e.getTypes().contains("DONGMYUN")) m.put("DONGMYUN", e.getLongName());
        }

        return m;
    }
}
