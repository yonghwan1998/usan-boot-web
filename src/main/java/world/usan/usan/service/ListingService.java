package world.usan.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import world.usan.usan.dto.ListingRequest;
import world.usan.usan.entity.Listing;
import world.usan.usan.repository.ListingRepository;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;

    @Transactional
    public Listing createDraft(ListingRequest req, Long userId) {

        Listing listing = Listing.createDraft(req, userId);
        return  listingRepository.save(listing);
    }
}
