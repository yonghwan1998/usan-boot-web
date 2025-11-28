package world.usan.usan.batch.job.broker.processor;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public final class EnrichedBrokerItem {

    private String listingType;
    private String brokerName;
    private String officeName;
    private String registrationNumber;
    private String tel;
    private String phone;
    private String rawAddress;
    private String sido;
    private String sigungu;
    private String emd;
    private String addrRoad;
    private String addrJibun;
    private BigDecimal lat;
    private BigDecimal lng;
}
