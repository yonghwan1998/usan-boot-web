package com.usanmap.usan.batch.job.broker.reader;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class BrokerRow {

    private String listingType;
    private String listingCoordinates;
    private String officeName;
    private String brokerName;
    private String address;
    private String registrationNumber;
    private String tel;
    private String phone;

    private List<String> sourceValues;
    private List<List<String>> sourceHeaders;

    private String sourceFileName;
    private int rowIndex;
}
