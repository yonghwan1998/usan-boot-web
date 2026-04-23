package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                String publicId = Listing.generatePublicId();
                return listingRepository.save(Listing.createDraft(publicId, req, userId));
            } catch (DataIntegrityViolationException e) {
                if (attempt == 4) throw e;
            }
        }
        throw new IllegalStateException("public_id 생성 실패");
    }
}
