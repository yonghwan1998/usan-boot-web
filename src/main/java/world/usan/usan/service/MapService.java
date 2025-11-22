package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.dto.BrokerMarkerDto;
import world.usan.usan.entity.BrokerPropertyCount;
import world.usan.usan.repository.BrokerPropertyCountRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MapService {

    private final BrokerPropertyCountRepository brokerPropertyCountRepository;

    @Transactional(readOnly = true)
    public List<BrokerMarkerDto> getMarkersInBounds(double south, double north, double west, double east) {

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
}
