package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.batch.job.broker.processor.EnrichedBrokerItem;
import world.usan.usan.entity.CrawledBroker;
import world.usan.usan.entity.CrawledListing;
import world.usan.usan.repository.CrawledBrokerRepository;
import world.usan.usan.repository.CrawledListingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrawledListingService {

    private final CrawledListingRepository crawledListingRepository;
    private final CrawledBrokerRepository crawledBrokerRepository;

    @Transactional
    public void createListing(UUID brokerCode, EnrichedBrokerItem e) {

        CrawledBroker crawledBroker = crawledBrokerRepository.findById(brokerCode).orElseThrow(() -> new IllegalArgumentException("Broker not found: " + brokerCode));
        CrawledListing crawledListing = new CrawledListing();
        crawledListing.setCode(UUID.randomUUID());
        crawledListing.setCrawledBroker(crawledBroker);
        crawledListing.setListingType(e.getListingType());
        crawledListing.setLat(e.getLat());
        crawledListing.setLng(e.getLng());
        crawledListingRepository.save(crawledListing);
    }
}
