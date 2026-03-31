package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.common.broker.BrokerPropertyTagFactory;
import com.usanmap.usan.dto.BrokerMarkerDetailDto;
import com.usanmap.usan.dto.BrokerMarkerDto;
import com.usanmap.usan.dto.BrokerPropertyTagDto;
import com.usanmap.usan.entity.BrokerPropertyCount;
import com.usanmap.usan.repository.BrokerPropertyCountRepository;

import java.math.BigDecimal;
import java.util.List;
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
    public List<BrokerMarkerDetailDto> getBrokerDetails(List<UUID> brokerCodes) {

        if (brokerCodes == null || brokerCodes.isEmpty()) {
            return List.of();
        }

        List<BrokerPropertyCount> list = brokerPropertyCountRepository.findByBrokerCodeIn(brokerCodes);

        return list.stream()
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
                .toList();
    }
}
