package world.usan.usan.batch.job.broker.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import world.usan.usan.batch.job.broker.reader.BrokerErrorRow;
import world.usan.usan.batch.job.broker.reader.BrokerRow;
import world.usan.usan.batch.job.broker.support.BrokerErrorCollector;
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
    private final BrokerErrorCollector errorCollector;

    @Override
    public EnrichedBrokerItem process(BrokerRow row) {

        String errorMessage;

        if (row.getAddress() == null || row.getAddress().isBlank()) {
            log.warn("[geocode skip] empty address, regno={}, name={}", row.getRegistrationNumber(), row.getBrokerName());

            errorMessage = "[geocode skip] empty address, regno=" + row.getRegistrationNumber() + ", name=" + row.getBrokerName();
            collectingErrorWithMessage(row, errorMessage);

            return null;
        }

        NaverGeocodeDto.Response resp;
        try {
            resp = client.geocode(row.getAddress())
                    .blockOptional()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[geocode error] error={} => {}", row.getAddress(), e.toString());

            errorMessage = "[geocode error] error=" + e.getMessage();
            collectingErrorWithMessage(row, errorMessage);

            return null;
        }

        if (resp == null || resp.getAddresses() == null || resp.getAddresses().isEmpty()) {
            log.warn("[geocode miss] address={}", row.getAddress());

            errorMessage = "[geocode miss] address=" + row.getAddress();
            collectingErrorWithMessage(row, errorMessage);

            return null;
        }

        NaverGeocodeDto.Address a = resp.getAddresses().get(0);
        Map<String, String> m = extract(a.getAddressElements());

        log.info("[process] file={} rowIndex={}",
                row.getSourceFileName(),
                row.getRowIndex());

        return EnrichedBrokerItem.builder()
                .listingType(row.getListingType())
                .brokerName(row.getBrokerName())
                .officeName(row.getOfficeName())
                .registrationNumber(row.getRegistrationNumber())
                .tel(row.getTel())
                .phone(row.getPhone())
                .roadAddress(a.getRoadAddress() == null ? "" : a.getRoadAddress())
                .jibunAddress(a.getJibunAddress() == null ? "" : a.getJibunAddress())
                .sido(m.getOrDefault("SIDO", ""))
                .sigungu(m.getOrDefault("SIGUGUN", ""))
                .emd(m.getOrDefault("DONGMYUN", ""))
                .ri(m.getOrDefault("RI", ""))
                .roadName(m.getOrDefault("ROAD_NAME", ""))
                .buildingNumber(m.getOrDefault("BUILDING_NUMBER", ""))
                .buildingName(m.getOrDefault("BUILDING_NAME", ""))
                .landNumber(m.getOrDefault("LAND_NUMBER", ""))
                .postalCode(m.getOrDefault("POSTAL_CODE", ""))
                .lat(new BigDecimal(a.getY()))
                .lng(new BigDecimal(a.getX()))
                .build();
    }

    private void collectingErrorWithMessage(BrokerRow row, String errorMessage) {
        errorCollector.add(
            BrokerErrorRow.builder()
                    .sourceFileName(row.getSourceFileName())
                    .rowIndex(row.getRowIndex())
                    .listingType(row.getListingType())
                    .officeName(row.getOfficeName())
                    .brokerName(row.getBrokerName())
                    .address(row.getAddress())
                    .registrationNumber(row.getRegistrationNumber())
                    .tel(row.getTel())
                    .phone(row.getPhone())
                    .errorMessage(errorMessage)
                    .build()
        );
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
            if (e.getTypes().contains("RI")) m.put("RI", e.getLongName());
            if (e.getTypes().contains("ROAD_NAME")) m.put("ROAD_NAME", e.getLongName());
            if (e.getTypes().contains("BUILDING_NUMBER")) m.put("BUILDING_NUMBER", e.getLongName());
            if (e.getTypes().contains("BUILDING_NAME")) m.put("BUILDING_NAME", e.getLongName());
            if (e.getTypes().contains("LAND_NUMBER")) m.put("LAND_NUMBER", e.getLongName());
            if (e.getTypes().contains("POSTAL_CODE")) m.put("POSTAL_CODE", e.getLongName());
        }

        return m;
    }
}
