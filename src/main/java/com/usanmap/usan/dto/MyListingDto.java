package com.usanmap.usan.dto;

public record MyListingDto(
        Long   listingId,
        String publicId,
        String addressName,
        String type,
        String tradeType,
        java.math.BigDecimal lat,
        java.math.BigDecimal lng
) {}
