package world.usan.usan.batch.job.broker.processor;

import java.math.BigDecimal;

public record EnrichedBrokerItem(
    String listingType,
    String brokerName,
    String officeName,
    String registrationNumber,
    String tel,
    String phone,
    String sido,
    String sigungu,
    String dongmyun,
//    String roadName,
    String addrRoad,
    String addrJibun,
    BigDecimal lat,
    BigDecimal lng
){}
