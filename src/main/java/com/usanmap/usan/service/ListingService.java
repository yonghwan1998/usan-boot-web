package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.dto.ListingRequest;
import com.usanmap.usan.dto.MyListingDto;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.repository.ListingRepository;
import com.usanmap.usan.util.PublicIdUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;

    @Transactional(readOnly = true)
    public List<MyListingDto> getMyListings(Long userId) {
        return listingRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(l -> new MyListingDto(
                        l.getId(),
                        l.getPublicId(),
                        l.getAddressName(),
                        l.getType(),
                        l.getTradeType(),
                        l.getLat(),
                        l.getLng()
                ))
                .toList();
    }

    @Transactional
    public Listing create(ListingRequest req, Long userId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                String publicId = PublicIdUtils.generate();
                return listingRepository.save(Listing.create(publicId, req, userId));
            } catch (DataIntegrityViolationException e) {
                if (attempt == 4) throw e;
            }
        }
        throw new IllegalStateException("public_id 생성 실패");
    }
}
