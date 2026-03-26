package world.usan.usan.dto;

import java.util.List;
import java.util.UUID;

public record ListingShareRequest(
        Long       listingId,
        List<UUID> brokerCodes
) {}
