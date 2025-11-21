package world.usan.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class BrokerMarkerDto {

    private final UUID brokerCode;
    private final String brokerName;
    private final String officeName;
    private final BigDecimal lat;
    private final BigDecimal lng;
}
