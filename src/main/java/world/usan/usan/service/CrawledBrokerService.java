package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.entity.CrawledBroker;
import world.usan.usan.repository.CrawledBrokerRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrawledBrokerService {

    private final CrawledBrokerRepository crawledBrokerRepository;

    @Transactional
    public UUID saveOrUpdate(String brokerName, String officeName, String registrationNumber,
                             String brokerAddress, String tel, String phone,
                             String sido, String sigungu, String emd,
                             String roadName, String addrRoad, String addrJibun,
                             BigDecimal lat, BigDecimal lng) {

        Optional<CrawledBroker> found = crawledBrokerRepository.findByRegistrationNumberAndBrokerName(registrationNumber, brokerName);

        CrawledBroker crawledBroker = new CrawledBroker();

        crawledBroker.setBrokerCode(UUID.randomUUID());
        if (found.isPresent()) {
            crawledBroker.setBrokerCode(found.get().getBrokerCode());
        }
        crawledBroker.setBrokerName(brokerName);
        crawledBroker.setOfficeName(officeName);
        crawledBroker.setRegistrationNumber(registrationNumber);
        crawledBroker.setTel(tel);
        crawledBroker.setPhone(phone);
        crawledBroker.setSido(sido);
        crawledBroker.setSigungu(sigungu);
        crawledBroker.setEmd(emd);
        crawledBroker.setRoadName(roadName);
        crawledBroker.setAddrRoad(addrRoad);
        crawledBroker.setAddrJibun(addrJibun);
        crawledBroker.setLat(lat);
        crawledBroker.setLng(lng);

        crawledBrokerRepository.save(crawledBroker);

        return crawledBroker.getBrokerCode();
    }
}
