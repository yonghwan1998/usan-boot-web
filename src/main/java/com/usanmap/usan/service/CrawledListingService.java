package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.batch.job.broker.processor.EnrichedBrokerItem;
import com.usanmap.usan.dto.BoundaryCodeResponse;
import com.usanmap.usan.entity.CrawledBroker;
import com.usanmap.usan.entity.CrawledListing;
import com.usanmap.usan.repository.CrawledBrokerRepository;
import com.usanmap.usan.repository.CrawledListingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrawledListingService {

    private final CrawledListingRepository crawledListingRepository;
    private final CrawledBrokerRepository crawledBrokerRepository;
    private final AdministrativeBoundaryService administrativeBoundaryService;

    @Transactional
    public void createListing(UUID brokerCode, EnrichedBrokerItem e) {

        CrawledBroker crawledBroker = crawledBrokerRepository.findById(brokerCode)
                .orElseThrow(() -> new IllegalArgumentException("Broker not found: " + brokerCode));

        BoundaryCodeResponse regionCodes = administrativeBoundaryService.getRegionCodes(
                e.getLat().doubleValue(), e.getLng().doubleValue()
        );

        CrawledListing crawledListing = new CrawledListing();
        crawledListing.setCode(UUID.randomUUID());
        crawledListing.setCrawledBroker(crawledBroker);
        crawledListing.setListingType(e.getListingType());
        crawledListing.setLat(e.getLat());
        crawledListing.setLng(e.getLng());
        crawledListing.setSidoCode(regionCodes.getSidoCode());
        crawledListing.setSigunguCode(regionCodes.getSigunguCode());
        crawledListing.setEmdCode(regionCodes.getEmdCode());
        crawledListingRepository.save(crawledListing);
    }
}
