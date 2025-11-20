package world.usan.usan.batch.job.broker.reader;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BrokerErrorRow {

    private String sourceFileName;
    private int rowIndex;

    private String listingType;
    private String officeName;
    private String brokerName;
    private String address;
    private String registrationNumber;
    private String tel;
    private String phone;

    private String errorMessage;
}
