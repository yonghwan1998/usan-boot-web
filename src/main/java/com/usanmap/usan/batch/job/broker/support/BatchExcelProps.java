package com.usanmap.usan.batch.job.broker.support;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("usan.batch.excel")
public class BatchExcelProps {
    private long maxFileBytes = 10_000_000L;
    private String inbox;
    private String archive;
    private String error;
    private String sheetName;
    @Value("${usan.batch.excel.header-rows}") int headerRows;//헤더 스킵 개수
    @Value("${usan.batch.excel.listing-type-col-index}") int listingTypeColIndex;//H 컬럼, 0-based index 7
    @Value("${usan.batch.excel.listing-coordinates-col-index}") int listingCoordinatesColIndex;//AV 컬럼, 0-based index 47
    @Value("${usan.batch.excel.office-name-col-index}") int officeNameColIndex;//BC 컬럼, 0-based index 54
    @Value("${usan.batch.excel.broker-name-col-index}") int brokerNameColIndex;//BD 컬럼, 0-based index 55
    @Value("${usan.batch.excel.address-col-index}") int addressColIndex;//BE 컬럼, 0-based index 56
    @Value("${usan.batch.excel.registration-number-col-index}") int registrationNumberColIndex;//BF 컬럼, 0-based index 57
    @Value("${usan.batch.excel.tel-col-index}") int telColIndex;//BG 컬럼, 0-based index 58
    @Value("${usan.batch.excel.phone-col-index}") int phoneColIndex;//BH 컬럼, 0-based index 59
}
