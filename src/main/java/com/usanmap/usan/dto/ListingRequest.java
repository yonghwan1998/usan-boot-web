package com.usanmap.usan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.usanmap.usan.entity.enums.ListingRole;
import com.usanmap.usan.entity.enums.ListingTradeType;
import com.usanmap.usan.entity.enums.ListingType;

import java.math.BigDecimal;

public record ListingRequest(

        // Step1
        @NotNull ListingRole role,
        @NotNull ListingType type,

        // Step2 - 주소
        @NotBlank String addressName,
        String roadAddress,
        String jibunAddress,
        String addressDetail,
        String sido,
        String sigungu,
        String emd,

        @NotNull Double lat,
        @NotNull Double lng,

        // Step3 - 기본 정보
        String ownerName,
        String carrier,
        String ownerPhone,

        ListingTradeType tradeType,

        Integer depositManwon,
        Integer rentManwon,
        Integer priceManwon,

        String dongho,
        String floorInfo,

        BigDecimal areaM2,

        String description

) {
    public static ListingRequest empty() {
        return new ListingRequest(null, null, "", "", "", "", "", "", "", null, null, "", "", "", null, null, null, null, "", "", null, "");
    }
}