package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.batch.job.broker.processor.EnrichedBrokerItem;
import com.usanmap.usan.entity.CrawledBroker;
import com.usanmap.usan.repository.CrawledBrokerRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrawledBrokerService {

    private final CrawledBrokerRepository crawledBrokerRepository;

    @Transactional
    public UUID saveOrUpdate(EnrichedBrokerItem e) {

        Optional<CrawledBroker> found = crawledBrokerRepository.findByRegistrationNumberAndBrokerName(e.getRegistrationNumber(), e.getBrokerName());

        CrawledBroker crawledBroker = new CrawledBroker();

        crawledBroker.setBrokerCode(UUID.randomUUID());
        if (found.isPresent()) {
            crawledBroker.setBrokerCode(found.get().getBrokerCode());
        }
        crawledBroker.setBrokerName(e.getBrokerName());
        crawledBroker.setOfficeName(e.getOfficeName());
        crawledBroker.setRegistrationNumber(e.getRegistrationNumber());
        crawledBroker.setTel(e.getTel());
        crawledBroker.setPhone(e.getPhone());
        crawledBroker.setRoadAddress(e.getRoadAddress());
        crawledBroker.setJibunAddress(e.getJibunAddress());
        crawledBroker.setSido(e.getSido());
        crawledBroker.setSigungu(e.getSigungu());
        crawledBroker.setEmd(e.getEmd());
        crawledBroker.setRi(e.getRi());
        crawledBroker.setRoadName(e.getRoadName());
        crawledBroker.setBuildingNumber(e.getBuildingNumber());
        crawledBroker.setBuildingName(e.getBuildingName());
        crawledBroker.setLandNumber(e.getLandNumber());
        crawledBroker.setPostalCode(e.getPostalCode());
        crawledBroker.setLat(e.getLat());
        crawledBroker.setLng(e.getLng());

        crawledBrokerRepository.save(crawledBroker);

        return crawledBroker.getBrokerCode();
    }

    @Transactional(readOnly = true)
    public CrawledBroker getBroker(UUID brokerCode) {
        return crawledBrokerRepository.findById(brokerCode)
                .orElseThrow(() -> new IllegalArgumentException("Broker not found: " + brokerCode));
    }
}
