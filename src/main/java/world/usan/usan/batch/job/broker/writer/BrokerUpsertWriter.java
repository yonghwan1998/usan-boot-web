package world.usan.usan.batch.job.broker.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.batch.job.broker.processor.EnrichedBrokerItem;
import world.usan.usan.service.CrawledBrokerService;
import world.usan.usan.service.CrawledListingService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BrokerUpsertWriter implements ItemWriter<EnrichedBrokerItem> {

    private final CrawledBrokerService crawledBrokerService;
    private final CrawledListingService crawledListingService;

    @Override
    @Transactional
    public void write(Chunk<? extends EnrichedBrokerItem> chunk) {

        for (EnrichedBrokerItem e : chunk) {
            UUID brokerCode = crawledBrokerService.saveOrUpdate(e);
            crawledListingService.createListing(brokerCode, e.getListingType());
        }
    }
}
