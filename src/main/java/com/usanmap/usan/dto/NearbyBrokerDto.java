package com.usanmap.usan.dto;

import java.util.List;
import java.util.UUID;

public record NearbyBrokerDto(
        UUID brokerCode,
        String officeName,
        String address,
        List<BrokerPropertyTagDto> tags
) {}
