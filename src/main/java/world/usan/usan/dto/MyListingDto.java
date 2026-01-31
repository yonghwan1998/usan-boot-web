package world.usan.usan.dto;

public record MyListingDto(
        Long listingId,
        double lat,
        double lng,
        String name
) {
}
