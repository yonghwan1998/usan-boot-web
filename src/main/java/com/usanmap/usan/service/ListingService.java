package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.dto.ListingRequest;
import com.usanmap.usan.dto.MyListingDto;
import com.usanmap.usan.entity.Listing;
import com.usanmap.usan.entity.enums.ListingRole;
import com.usanmap.usan.entity.enums.ListingTradeType;
import com.usanmap.usan.entity.enums.ListingType;
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
    public void delete(String publicId, Long userId) {
        Listing listing = listingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매물입니다."));
        if (!listing.getUserId().equals(userId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        listing.markDeleted();
    }

    @Transactional(readOnly = true)
    public ListingRequest getByPublicId(String publicId, Long userId) {
        Listing listing = listingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매물입니다."));
        if (!listing.getUserId().equals(userId)) {
            throw new IllegalStateException("조회 권한이 없습니다.");
        }
        return toListingRequest(listing);
    }

    @Transactional
    public void update(String publicId, ListingRequest req, Long userId) {
        Listing listing = listingRepository.findByPublicId(publicId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 매물입니다."));
        if (!listing.getUserId().equals(userId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }
        listing.update(req);
    }

    private ListingRequest toListingRequest(Listing l) {
        return new ListingRequest(
                l.getRole() == null ? null : ListingRole.valueOf(l.getRole()),
                l.getType() == null ? null : ListingType.valueOf(l.getType()),
                l.getAddressName(),
                l.getRoadAddress(),
                l.getJibunAddress(),
                l.getAddressDetail(),
                l.getSido(),
                l.getSigungu(),
                l.getEmd(),
                l.getLat() == null ? null : l.getLat().doubleValue(),
                l.getLng() == null ? null : l.getLng().doubleValue(),
                l.getOwnerName(),
                l.getCarrier(),
                l.getOwnerPhone(),
                l.getTradeType() == null ? null : ListingTradeType.valueOf(l.getTradeType()),
                l.getDepositManwon(),
                l.getRentManwon(),
                l.getPriceManwon(),
                l.getDongho(),
                l.getFloorInfo(),
                l.getAreaM2(),
                l.getDescription()
        );
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
