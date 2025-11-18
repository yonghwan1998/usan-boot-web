package world.usan.usan.batch.job.broker.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.batch.job.broker.processor.EnrichedBrokerItem;
import world.usan.usan.service.BrokerService;
import world.usan.usan.service.ListingService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BrokerUpsertWriter implements ItemWriter<EnrichedBrokerItem> {

    private final BrokerService brokerService;
    private final ListingService listingService;

    @Override
    @Transactional
    public void write(Chunk<? extends EnrichedBrokerItem> chunk) {

        for (EnrichedBrokerItem e : chunk) {
            UUID brokerCode = brokerService.saveOrUpdate(
                    e.brokerName(),
                    e.officeName(),
                    e.registrationNumber(),
                    "",
                    e.tel(),
                    e.phone(),
                    e.sido(),
                    e.sigungu(),
                    e.dongmyun(),
                    "",
                    e.addrRoad(),
                    e.addrJibun(),
                    e.lat(),
                    e.lng()
            );
            listingService.createListing(brokerCode, e.listingType());
        }
    }
}
