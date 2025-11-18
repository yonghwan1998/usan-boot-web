package world.usan.usan.batch.job.broker.reader;

public record BrokerRow (
    String listingType,
    String officeName,
    String brokerName,
    String address,
    String registrationNumber,
    String tel,
    String phone
){}
