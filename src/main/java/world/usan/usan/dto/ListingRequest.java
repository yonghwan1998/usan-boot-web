package world.usan.usan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import world.usan.usan.entity.enums.ListingRole;
import world.usan.usan.entity.enums.ListingType;

public record ListingRequest(
        @NotNull ListingRole role,
        @NotNull ListingType type,
        @NotBlank String addressName,
        String roadAddress,
        String jibunAddress,
        @NotNull Double lat,
        @NotNull Double lng,
        String addressDetail
) {
}