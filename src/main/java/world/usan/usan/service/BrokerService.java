package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.entity.Broker;
import world.usan.usan.repository.BrokerRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BrokerService {

    private final BrokerRepository brokerRepository;

    @Transactional
    public UUID saveOrUpdate(String brokerName, String officeName, String registrationNumber,
                             String brokerAddress, String tel, String phone,
                             String sido, String sigungu, String dongmyun,
                             String roadName, String addrRoad, String addrJibun,
                             BigDecimal lat, BigDecimal lng) {

        Optional<Broker> found = brokerRepository.findByRegistrationNumberAndBrokerName(registrationNumber, brokerName);

        Broker broker = new Broker();

        broker.setBrokerCode(UUID.randomUUID());
        if (found.isPresent()) {
            broker.setBrokerCode(found.get().getBrokerCode());
        }
        broker.setBrokerName(brokerName);
        broker.setOfficeName(officeName);
        broker.setRegistrationNumber(registrationNumber);
        broker.setTel(tel);
        broker.setPhone(phone);
        broker.setSido(sido);
        broker.setSigungu(sigungu);
        broker.setDongmyun(dongmyun);
        broker.setRoadName(roadName);
        broker.setAddrRoad(addrRoad);
        broker.setAddrJibun(addrJibun);
        broker.setLat(lat);
        broker.setLng(lng);

        brokerRepository.save(broker);

        return broker.getBrokerCode();
    }
}
