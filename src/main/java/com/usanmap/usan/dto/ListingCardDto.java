package com.usanmap.usan.dto;

public record ListingCardDto(
        String addressName,
        String roadAddress,
        String jibunAddress,
        int nearbyCount
) {}
