package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.entity.Broker;
import world.usan.usan.entity.CrawledListing;
import world.usan.usan.repository.BrokerRepository;
import world.usan.usan.repository.ListingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final BrokerRepository brokerRepository;

    @Transactional
    public void createListing(UUID brokerCode, String listingType) {

        Broker broker = brokerRepository.findById(brokerCode).orElseThrow(() -> new IllegalArgumentException("Broker not found: " + brokerCode));
        CrawledListing crawledListing = new CrawledListing();
        crawledListing.setCode(UUID.randomUUID());
        crawledListing.setBroker(broker);
        crawledListing.setListingType(listingType);
        listingRepository.save(crawledListing);
    }
}
