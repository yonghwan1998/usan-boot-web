package world.usan.usan.dto;

import jakarta.validation.constraints.NotBlank;
import world.usan.usan.entity.enums.ListingRole;
import world.usan.usan.entity.enums.ListingType;

public record ListingRequest(
        @NotBlank ListingRole role,
        @NotBlank ListingType type,
        @NotBlank String address
) {
}