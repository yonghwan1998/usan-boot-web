package com.usanmap.usan.service;

import com.usanmap.usan.common.broker.BrokerPropertyTagFactory;
import com.usanmap.usan.dto.NearbyBrokerDto;
import com.usanmap.usan.entity.UserRegion;
import com.usanmap.usan.repository.BrokerPropertyCountRepository;
import com.usanmap.usan.repository.UserRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MainService {

    private final UserRegionRepository userRegionRepository;
    private final BrokerPropertyCountRepository brokerPropertyCountRepository;

    public List<NearbyBrokerDto> getNearbyBrokers(Long userId) {
        List<UserRegion> regions = userRegionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (regions.isEmpty()) {
            return List.of();
        }

        UserRegion region = regions.get(0);
        var brokers = new ArrayList<>(
                brokerPropertyCountRepository.findBySidoAndSigunguAndEmd(
                        region.getSidoName(), region.getSigunguName(), region.getEmdName()
                )
        );

        Collections.shuffle(brokers);

        return brokers.stream()
                .map(b -> new NearbyBrokerDto(
                        b.getBrokerCode(),
                        b.getOfficeName(),
                        b.getSido() + " " + b.getSigungu() + " " + b.getEmd(),
                        BrokerPropertyTagFactory.topN(b, 3)
                ))
                .toList();
    }
}
