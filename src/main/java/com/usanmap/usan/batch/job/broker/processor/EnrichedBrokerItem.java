package com.usanmap.usan.batch.job.broker.processor;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public final class EnrichedBrokerItem {

    private String listingType;
    private BigDecimal listingLat;
    private BigDecimal listingLng;
    private String brokerName;
    private String officeName;
    private String registrationNumber;
    private String tel;
    private String phone;

    private String roadAddress;
    private String jibunAddress;

    private String sido;
    private String sigungu;
    private String emd;
    private String ri;
    private String roadName;
    private String buildingNumber;
    private String buildingName;
    private String landNumber;
    private String postalCode;
    private BigDecimal lat;
    private BigDecimal lng;
}
