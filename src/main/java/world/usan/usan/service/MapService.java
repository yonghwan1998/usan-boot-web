package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.dto.BrokerMarkerDetailDto;
import world.usan.usan.dto.BrokerMarkerDto;
import world.usan.usan.entity.Broker;
import world.usan.usan.entity.BrokerPropertyCount;
import world.usan.usan.repository.BrokerPropertyCountRepository;
import world.usan.usan.repository.BrokerRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MapService {

    private final BrokerPropertyCountRepository brokerPropertyCountRepository;
    private final BrokerRepository brokerRepository;

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

        Broker broker = brokerRepository.findByBrokerCode(brokerCode);

        BrokerPropertyCount count = brokerPropertyCountRepository.findByBrokerCode(brokerCode);

        return BrokerMarkerDetailDto.builder()
                .brokerCode(broker.getBrokerCode())
                .brokerName(broker.getBrokerName())
                .officeName(broker.getOfficeName())
                .registrationNumber(broker.getRegistrationNumber())
                .tel(broker.getTel())
                .phone(broker.getPhone())
                .addrRoad(broker.getAddrRoad())
                .addrJibun(broker.getAddrJibun())
                .aptCnt(count.getAptCnt())
                .officetelCnt(count.getOfficetelCnt())
                .villaCnt(count.getVillaCnt())
                .build();
    }
}
