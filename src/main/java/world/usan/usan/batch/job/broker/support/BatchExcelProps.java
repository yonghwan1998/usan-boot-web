package world.usan.usan.batch.job.broker.support;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("usan.batch.excel")
public class BatchExcelProps {
    private String inbox;
    private String archive;
    private String error;
    private String sheetName;
    @Value("${usan.batch.excel.header-rows}") int headerRows;//헤더 스킵 개수
    @Value("${usan.batch.excel.listing-type-col-index}") int listingTypeColIndex;//G 컬럼, 0~6번째 index
    @Value("${usan.batch.excel.office-name-col-index}") int officeNameColIndex;//BB 컬럼, 0~53번째 index
    @Value("${usan.batch.excel.broker-name-col-index}") int brokerNameColIndex;//BC 컬럼, 0~54번째 index
    @Value("${usan.batch.excel.address-col-index}") int addressColIndex;//BD 컬럼, 0~55번째 index
    @Value("${usan.batch.excel.registration-number-col-index}") int registrationNumberColIndex;//BE 컬럼, 0~56번째 index
    @Value("${usan.batch.excel.tel-col-index}") int telColIndex;//BF 컬럼, 0~57번째 index
    @Value("${usan.batch.excel.phone-col-index}") int phoneColIndex;//BG 컬럼, 0~58번째 index
}
