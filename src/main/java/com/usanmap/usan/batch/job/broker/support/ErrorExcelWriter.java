package com.usanmap.usan.batch.job.broker.support;

import lombok.extern.slf4j.Slf4j;
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
public class ErrorExcelWriter {

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
            File outFile = new File(dir, "broker_errors_" + ts + ".xlsx");

            try(SXSSFWorkbook wb = new SXSSFWorkbook(100);
                FileOutputStream fos = new FileOutputStream(outFile)) {

                Sheet sheet = wb.createSheet("errors");

                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("원본 파일명");
                header.createCell(1).setCellValue("원본 파일 행번호");
                header.createCell(2).setCellValue("종류");
                header.createCell(3).setCellValue("중개사무소명");
                header.createCell(4).setCellValue("중개사명");
                header.createCell(5).setCellValue("중개사무소 주소");
                header.createCell(6).setCellValue("중개사등록번호");
                header.createCell(7).setCellValue("중개사무소 전화");
                header.createCell(8).setCellValue("중개사 휴대폰");
                header.createCell(9).setCellValue("errorMessage");

                int rowNum = 1;
                for (BrokerErrorRow e : errors) {
                    Row row = sheet.createRow(rowNum++);

                    int c = 0;
                    row.createCell(c++).setCellValue(nvl(e.getSourceFileName()));
                    row.createCell(c++).setCellValue(e.getRowIndex());
                    row.createCell(c++).setCellValue(nvl(e.getListingType()));
                    row.createCell(c++).setCellValue(nvl(e.getOfficeName()));
                    row.createCell(c++).setCellValue(nvl(e.getBrokerName()));
                    row.createCell(c++).setCellValue(nvl(e.getAddress()));
                    row.createCell(c++).setCellValue(nvl(e.getRegistrationNumber()));
                    row.createCell(c++).setCellValue(nvl(e.getTel()));
                    row.createCell(c++).setCellValue(nvl(e.getPhone()));
                    row.createCell(c++).setCellValue(nvl(e.getErrorMessage()));
                }

                wb.write(fos);
                wb.dispose();
            }

            log.info("[ErrorExcelWriter] {} errors saved to {}", errors.size(), outFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("[ErrorExcelWriter] failed to write error excel", e);
        }
    }

    private String nvl(String s){
        return s == null ? "" : s;
    }
}
