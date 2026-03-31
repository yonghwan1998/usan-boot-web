package com.usanmap.usan.batch.job.broker.reader;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import com.usanmap.usan.batch.job.broker.support.BatchExcelProps;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class SaxExcelRowItemReader implements ResourceAwareItemReaderItemStream<BrokerRow> {

    private final BatchExcelProps props;

    private Resource resource;
    private OPCPackage opcPackage;
    private ReadOnlySharedStringsTable sst;
    private Iterator<List<String>> rowIterator;

    private int headerRows;
    private int currentRowIndex = 0;

    private String ecKey;

    public SaxExcelRowItemReader(BatchExcelProps props) {
        this.props = props;
        this.headerRows = props.getHeaderRows();
    }

    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        this.ecKey = "sax.excel.pos." + resource.getFilename();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {

        try {
            if (resource == null) {
                return;
            }

            InputStream is = resource.getInputStream();
            this.opcPackage = OPCPackage.open(is);
            this.sst = new ReadOnlySharedStringsTable(opcPackage);

            XSSFReader reader = new XSSFReader(opcPackage);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

            InputStream sheetStream = null;

            //sheetName이 지정된 경우
            if (props.getSheetName() != null && !props.getSheetName().isBlank()) {
                while (sheets.hasNext()) {
                    InputStream tmp = sheets.next();
                    String name = sheets.getSheetName();

                    if (props.getSheetName().equals(name)) {
                        sheetStream = tmp;
                        break;
                    }
                    tmp.close();
                }
            } else if (sheets.hasNext()) {
                sheetStream = sheets.next();
            }

            if (sheetStream == null) {
                throw new IllegalStateException("Sheet not found: " + props.getSheetName());
            }

            XMLReader parser = XMLReaderFactory.createXMLReader();
            ExcelSheetHandler handler = new ExcelSheetHandler(sst, executionContext, ecKey);
            parser.setContentHandler(handler);

            parser.parse(new InputSource(sheetStream));

            List<List<String>> allRows = handler.getRowList();

            int savedPos = executionContext.containsKey(ecKey)
                    ? executionContext.getInt(ecKey)
                    : 0;

            int skip = headerRows + savedPos;
            if (skip > allRows.size()) {
                skip = allRows.size();
            }

            this.currentRowIndex = savedPos;
            this.rowIterator = allRows.subList(skip, allRows.size()).iterator();

            sheetStream.close();
        } catch (Exception e) {
            log.error("[SAX-open-error] {}", e.getMessage(), e);
            throw new ItemStreamException("Failed to open SAX Excel reader: " + resource.getFilename(), e);
        }
    }

    @Override
    public BrokerRow read() {

        if (rowIterator == null || !rowIterator.hasNext()) {
            return null;
        }

        List<String> row = rowIterator.next();
        currentRowIndex++;

        String listingType          = get(row, props.getListingTypeColIndex());
        String listingCoordinates   = get(row, props.getListingCoordinatesColIndex());
        String officeName           = get(row, props.getOfficeNameColIndex());
        String brokerName           = get(row, props.getBrokerNameColIndex());
        String address              = get(row, props.getAddressColIndex());
        String registrationNumber   = get(row, props.getRegistrationNumberColIndex());
        String tel                  = get(row, props.getTelColIndex());
        String phone                = get(row, props.getPhoneColIndex());

        return BrokerRow.builder()
                .listingType(listingType)
                .listingCoordinates(listingCoordinates)
                .officeName(officeName)
                .brokerName(brokerName)
                .address(address)
                .registrationNumber(registrationNumber)
                .tel(tel)
                .phone(phone)
                .sourceFileName(resource.getFilename())
                .rowIndex(currentRowIndex)
                .build();
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (ecKey != null) {
            executionContext.putInt(ecKey, currentRowIndex);
        }
    }

    @Override
    public void close() throws ItemStreamException {

        try {
            if (opcPackage != null) {
                opcPackage.close();
            }
        } catch (Exception e) {}
    }

    private String get(List<String> row, int idx) {

        if (idx < 0 || idx >= row.size()) {
            return "";
        }
        String v = row.get(idx);

        return v == null ? "" : v.trim();
    }

    private static class ExcelSheetHandler extends org.xml.sax.helpers.DefaultHandler {

        private final ReadOnlySharedStringsTable sst;
        private final ExecutionContext executionContext;
        private final String ecKey;

        private final List<List<String>> rowList = new ArrayList<>();
        private List<String> cellBuffer;

        private String cellType;
        private StringBuilder cellValue = new StringBuilder();

        private int currentColIndex = -1;

        public ExcelSheetHandler(ReadOnlySharedStringsTable sst,
                                 ExecutionContext executionContext,
                                 String ecKey) {
            this.sst = sst;
            this.executionContext = executionContext;
            this.ecKey = ecKey;
        }

        public List<List<String>> getRowList() {
            return rowList;
        }

        @Override
        public void startElement(String uri,
                                 String localName,
                                 String qName,
                                 Attributes attributes) {
            if ("row".equals(qName)) {
                cellBuffer = new ArrayList<>();
                currentColIndex = -1;
            }

            if ("c".equals(qName)) {
                cellType = attributes.getValue("t");
                String r = attributes.getValue("r");
                if (r != null) {
                    currentColIndex = columnToIndex(r);
                } else {
                    currentColIndex = -1;
                }
            }

            if ("v".equals(qName) || "t".equals(qName)) {
                cellValue.setLength(0);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            cellValue.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {

            if ("v".equals(qName) || "t".equals(qName)) {
                String raw = cellValue.toString();

                String value;
                if ("s".equals(cellType)) {
                    int idx = Integer.parseInt(raw);
                    value = sst.getItemAt(idx).toString();
                } else {
                    value = raw;
                }

                if (currentColIndex < 0) {
                    cellBuffer.add(value);
                } else {
                    while (cellBuffer.size() < currentColIndex) {
                        cellBuffer.add("");
                    }
                    cellBuffer.add(value);
                }
            }

            if ("row".equals(qName)) {
                rowList.add(cellBuffer);
            }
        }

        private static int columnToIndex(String cellRef) {

            StringBuffer colRef = new StringBuffer();
            for (int i = 0; i < cellRef.length(); i++) {
                char ch = cellRef.charAt(i);
                if (ch >= 'A' && ch <= 'Z') {
                    colRef.append(ch);
                } else {
                    break;
                }
            }

            String col = colRef.toString();
            int result = 0;
            for (int i = 0; i < col.length(); i++) {
                result = result * 26 + (col.charAt(i) - 'A' + 1);
            }

            return result - 1;
        }
    }
}
