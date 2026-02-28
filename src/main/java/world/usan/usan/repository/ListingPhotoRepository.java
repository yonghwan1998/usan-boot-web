package world.usan.usan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import world.usan.usan.entity.ListingPhoto;

import java.util.List;

public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, Long> {

    List<ListingPhoto> findByListingIdOrderBySortOrderAsc(Long listingId);
}
