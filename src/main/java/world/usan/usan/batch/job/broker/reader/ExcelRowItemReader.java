package world.usan.usan.batch.job.broker.reader;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;
import world.usan.usan.batch.job.broker.support.BatchExcelProps;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Slf4j
public class ExcelRowItemReader implements ResourceAwareItemReaderItemStream<BrokerRow> {

    private final BatchExcelProps props;

    private Resource resource;
    private Workbook workbook;
    private Iterator<Row> rowIterator;

    private static final String EC_PREFIX = "ExcelRowItemReader.row.";
    private String ecKey;// 파일별로 유니크 키
    private int currentRowIdx = 0;// 헤더 스킵 후부터 증가

    public ExcelRowItemReader(BatchExcelProps props) {
        this.props = props;
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        this.ecKey = EC_PREFIX + resource.getFilename();
    }

    @Override
    public void open(ExecutionContext executionContext) {

        if (resource == null) {
            return;
        }

        try {
            InputStream is = resource.getInputStream();
            this.workbook = WorkbookFactory.create(is);

            Sheet sheet = (props.getSheetName() != null && !props.getSheetName().isBlank())
                    ? workbook.getSheet(props.getSheetName())
                    : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalStateException("Sheet not found: " + resource.getFilename());
            }

            this.rowIterator = sheet.rowIterator();

            int saved = executionContext.containsKey(ecKey) ? executionContext.getInt(ecKey) : 0;
            //저장된 위치 복구
            int skip = props.getHeaderRows() + saved;

            for (int i = 0; i < skip && rowIterator.hasNext(); i++) {
                rowIterator.next();
            }

            this.currentRowIdx = saved;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open workbook: " + resource, e);
        } catch (Exception e) {
            log.error("[excel-open-error] {} -> {}", resource.getFilename(), e);
            throw new IllegalStateException("Excel open failed: " + resource.getFilename(), e);
        }
    }

    @Override
    public BrokerRow read() {
        if (rowIterator == null || !rowIterator.hasNext()) {
            return null;
        }

        Row r = rowIterator.next();
        currentRowIdx++;

        String listingType          = getCell(r, props.getListingTypeColIndex());
        String officeName           = getCell(r, props.getOfficeNameColIndex());
        String brokerName           = getCell(r, props.getBrokerNameColIndex());
        String address              = getCell(r, props.getAddressColIndex());
        String registrationNumber   = getCell(r, props.getRegistrationNumberColIndex());
        String tel                  = getCell(r, props.getTelColIndex());
        String phone                = getCell(r, props.getPhoneColIndex());

        return new BrokerRow(listingType, officeName, brokerName, address, registrationNumber, tel, phone);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        if (ecKey != null) {
            executionContext.putInt(ecKey, currentRowIdx);
        }
    }

    @Override
    public void close() {

        if (workbook != null) {
            try {
                workbook.close();
            } catch (IOException e) {}
        }
    }

    private String getCell(Row row, int idx) {

        if (idx < 0) {
            return "";
        }

        Cell c = row.getCell(idx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        if(c == null) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = row.getSheet()
                                        .getWorkbook()
                                        .getCreationHelper()
                                        .createFormulaEvaluator();

        return formatter.formatCellValue(c, evaluator).trim();
    }

}
