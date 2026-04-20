package com.usanmap.usan.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class BrokerClusterRequest {

    private List<UUID> brokerCodes;
    private Double lat;
    private Double lng;
    private Double distanceM;
}
