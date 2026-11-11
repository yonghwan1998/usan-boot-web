package com.usanmap.usan.batch.job.broker.support;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;
import com.usanmap.usan.batch.job.broker.reader.BrokerErrorRow;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorExcelWriter {

    private final BatchExcelProps props;

    public void write(String errorDir, List<BrokerErrorRow> errors) {

        if (errors == null || errors.isEmpty()) {
            return;
        }

        try {
            File dir = new File(errorDir);
            if (!dir.exists() && !dir.mkdirs()) {
                log.warn("[ErrorExcelWriter] failed to create error dir: {}", errorDir);
            }

            String ts = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File outFile = new File(dir, "broker_errors_" + ts + "_" + java.util.UUID.randomUUID() + ".xlsx");

            try(SXSSFWorkbook wb = new SXSSFWorkbook(100);
                FileOutputStream fos = new FileOutputStream(outFile)) {

                // 재처리 대상 시트를 첫 번째로 생성하고 원본 열 순서를 그대로 유지한다.
                String sheetName = props.getSheetName();
                Sheet sheet = wb.createSheet(sheetName == null || sheetName.isBlank() ? "data" : sheetName);
                List<List<String>> headers = errors.get(0).getSourceHeaders();
                int errorColumn = 0;
                for (List<String> header : headers) {
                    errorColumn = Math.max(errorColumn, header.size());
                }
                for (BrokerErrorRow error : errors) {
                    errorColumn = Math.max(errorColumn, error.getSourceValues().size());
                }
                // 재처리 파일에 이미 오류 열이 있으면 해당 열을 갱신한다.
                if (!headers.isEmpty()) {
                    List<String> lastHeader = headers.get(headers.size() - 1);
                    if (!lastHeader.isEmpty() && "errorMessage".equals(lastHeader.get(lastHeader.size() - 1))) {
                        errorColumn = lastHeader.size() - 1;
                    }
                }
                for (int i = 0; i < props.getHeaderRows(); i++) {
                    Row header = sheet.createRow(i);
                    writeValues(header, i < headers.size() ? headers.get(i) : List.of());
                    if (i == props.getHeaderRows() - 1) {
                        header.createCell(errorColumn).setCellValue("errorMessage");
                    }
                }

                int rowNum = props.getHeaderRows();
                for (BrokerErrorRow e : errors) {
                    Row row = sheet.createRow(rowNum++);
                    writeValues(row, e.getSourceValues());
                    row.createCell(errorColumn).setCellValue(nvl(e.getErrorMessage()));
                }

                wb.write(fos);
                wb.dispose();
            }

            log.info("[ErrorExcelWriter] {} errors saved to {}", errors.size(), outFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("[ErrorExcelWriter] failed to write error excel", e);
        }
    }

    private void writeValues(Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            row.createCell(i).setCellValue(nvl(values.get(i)));
        }
    }

    private String nvl(String s){
        return s == null ? "" : s;
    }
}
