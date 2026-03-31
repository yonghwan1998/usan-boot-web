package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.dto.ListingRequest;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.repository.ListingRepository;

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
