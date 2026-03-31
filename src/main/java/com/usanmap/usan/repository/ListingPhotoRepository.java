package com.usanmap.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.usanmap.usan.entity.ListingPhoto;

import java.util.List;

public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, Long> {

    List<ListingPhoto> findByListingIdOrderBySortOrderAsc(Long listingId);
}
