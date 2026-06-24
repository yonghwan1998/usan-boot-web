package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.common.broker.BrokerPropertyTagFactory;
import com.usanmap.usan.util.GeoDistance;
import com.usanmap.usan.dto.BrokerMarkerDetailDto;
import com.usanmap.usan.dto.BrokerMarkerDto;
import com.usanmap.usan.dto.BrokerPropertyTagDto;
import com.usanmap.usan.entity.BrokerPropertyCount;
import com.usanmap.usan.repository.BrokerPropertyCountRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MapService {

    private final BrokerPropertyCountRepository brokerPropertyCountRepository;

    @Transactional(readOnly = true)
    public List<BrokerMarkerDto> getBrokersInBounds(double south, double north, double west, double east) {

        BigDecimal southBd = new BigDecimal(south);
        BigDecimal northBd = new BigDecimal(north);
        BigDecimal westBd = new BigDecimal(west);
        BigDecimal eastBd = new BigDecimal(east);

        List<BrokerPropertyCount> entities = brokerPropertyCountRepository.findByLatBetweenAndLngBetween(southBd, northBd, westBd, eastBd);

        return entities.stream()
                .map(b -> BrokerMarkerDto.builder()
                        .brokerCode(b.getBrokerCode())
                        .brokerName(b.getBrokerName())
                        .officeName(b.getOfficeName())
                        .lat(b.getLat())
                        .lng(b.getLng())
                        .build()
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> getCountsByRadius(double lat, double lng) {
        double margin    = 3000.0 / 111320.0;
        double marginLng = 3000.0 / (111320.0 * Math.cos(Math.toRadians(lat)));

        List<Object[]> rows = brokerPropertyCountRepository.countByRadii(
                lat, lng,
                lat - margin, lat + margin,
                lng - marginLng, lng + marginLng
        );

        if (rows.isEmpty()) return Map.of(500, 0L, 1000, 0L, 2000, 0L, 3000, 0L);
        Object[] row = rows.get(0);
        return Map.of(
                500,  toLong(row[0]),
                1000, toLong(row[1]),
                2000, toLong(row[2]),
                3000, toLong(row[3])
        );
    }

    private long toLong(Object val) {
        return val == null ? 0L : ((Number) val).longValue();
    }

    @Transactional(readOnly = true)
    public BrokerMarkerDetailDto getBrokerDetail(UUID brokerCode) {

        BrokerPropertyCount broker = brokerPropertyCountRepository.findById(brokerCode)
                .orElseThrow(() -> new IllegalArgumentException("Broker not found: " + brokerCode));

        List<BrokerPropertyTagDto> items = BrokerPropertyTagFactory.topN(broker, 5);

        return BrokerMarkerDetailDto.builder()
                .brokerCode(broker.getBrokerCode())
                .brokerName(broker.getBrokerName())
                .officeName(broker.getOfficeName())
                .registrationNumber(broker.getRegistrationNumber())
                .tel(broker.getTel())
                .phone(broker.getPhone())
                .addrRoad(broker.getAddrRoad())
                .addrJibun(broker.getAddrJibun())
                .lat(broker.getLat())
                .lng(broker.getLng())
                .top5(items)
                .build();
    }

    @Transactional(readOnly = true)
    public List<BrokerMarkerDetailDto> getBrokerDetails(List<UUID> brokerCodes, Double filterLat, Double filterLng, Double filterDistanceM, List<String> filterListingTypes) {

        if (brokerCodes == null || brokerCodes.isEmpty()) {
            return List.of();
        }

        Set<String> typeSet = (filterListingTypes != null && !filterListingTypes.isEmpty())
                ? Set.copyOf(filterListingTypes) : null;

        List<BrokerPropertyCount> list = brokerPropertyCountRepository.findByBrokerCodeIn(brokerCodes);

        return list.stream()
                .filter(b -> {
                    if (typeSet == null) return true;
                    return BrokerPropertyTagFactory.allSorted(b).stream()
                            .anyMatch(tag -> typeSet.contains(tag.getLabel()));
                })
                .map(b -> {
                    List<BrokerPropertyTagDto> top5 = BrokerPropertyTagFactory.topN(b, 5);
                    return BrokerMarkerDetailDto.builder()
                            .brokerCode(b.getBrokerCode())
                            .brokerName(b.getBrokerName())
                            .officeName(b.getOfficeName())
                            .registrationNumber(b.getRegistrationNumber())
                            .tel(b.getTel())
                            .phone(b.getPhone())
                            .sido(b.getSido())
                            .sigungu(b.getSigungu())
                            .emd(b.getEmd())
                            .addrRoad(b.getAddrRoad())
                            .addrJibun(b.getAddrJibun())
                            .lat(b.getLat())
                            .lng(b.getLng())
                            .top5(top5)
                            .build();
                })
                .filter(dto -> {
                    if (filterLat == null || filterLng == null || filterDistanceM == null) return true;
                    if (dto.getLat() == null || dto.getLng() == null) return false;
                    double d = GeoDistance.meters(filterLat, filterLng, dto.getLat().doubleValue(), dto.getLng().doubleValue());
                    return d <= filterDistanceM;
                })
                .toList();
    }
}
