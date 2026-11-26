package com.usanmap.usan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record ListingShareRequest(Long listingId, List<UUID> brokerCodes,
                                  @Valid SelectionCriteria selectionCriteria) {
    public ListingShareRequest(Long listingId, List<UUID> brokerCodes) {
        this(listingId, brokerCodes, null);
    }

    public record SelectionCriteria(@Min(1) Integer radiusM,
                                    @Size(max = 30) List<@NotBlank @Size(max = 15) String> listingTypes) {}
}
