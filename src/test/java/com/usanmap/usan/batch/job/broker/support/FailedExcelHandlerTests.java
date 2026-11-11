package com.usanmap.usan.batch.job.broker.support;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class FailedExcelHandlerTests {
    @TempDir Path temp;

    private JobExecution execution(Path source, int committed) {
        JobExecution job = new JobExecution(1L, new JobParametersBuilder()
                .addString("inputFile", source.toString()).toJobParameters());
        job.setStatus(BatchStatus.FAILED);
        StepExecution step = job.createStepExecution("brokerExcelStep");
        step.getExecutionContext().putInt(FailedExcelHandler.LAST_COMMITTED_ROW, committed);
        step.addFailureException(new IllegalArgumentException("잘못된 좌표 <invalid> & value"));
        return job;
    }

    @Test
    void movesOriginalAndAppendsCommittedProgressAndCauseWithoutChangingData() throws Exception {
        Path source = temp.resolve("input.xlsx");
        try (var workbook = new XSSFWorkbook(); var out = Files.newOutputStream(source)) {
            workbook.createSheet("data").createRow(0).createCell(0).setCellValue("원본 데이터");
            workbook.write(out);
        }
        BatchExcelProps props = new BatchExcelProps();
        Path error = temp.resolve("error");
        props.setError(error.toString());
        new FailedExcelHandler(props).handle(execution(source, 102));
        assertThat(source).doesNotExist();
        try (var workbook = new XSSFWorkbook(error.resolve("input.xlsx").toFile())) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue()).isEqualTo("원본 데이터");
            var report = workbook.getSheet("batch_failure");
            assertThat(report.getRow(1).getCell(0).getStringCellValue()).isEqualTo("102");
            StringBuilder content = new StringBuilder();
            report.forEach(row -> content.append(row.getCell(0).getStringCellValue()));
            assertThat(content.toString()).contains("잘못된 좌표 <invalid> & value");
        }
    }

    @Test
    void corruptWorkbookIsIsolatedAndGetsTextReportWithoutOverwritingExistingFile() throws Exception {
        Path source = temp.resolve("input.xlsx");
        Files.writeString(source, "broken");
        Path error = Files.createDirectory(temp.resolve("error"));
        Files.writeString(error.resolve("input.xlsx"), "existing");
        BatchExcelProps props = new BatchExcelProps();
        props.setError(error.toString());
        new FailedExcelHandler(props).handle(execution(source, 0));
        assertThat(source).doesNotExist();
        assertThat(Files.readString(error.resolve("input.xlsx"))).isEqualTo("existing");
        try (var paths = Files.list(error)) {
            var report = paths.filter(path -> path.toString().endsWith(".failure.txt")).findFirst().orElseThrow();
            assertThat(Files.readString(report)).contains("마지막 커밋 완료 엑셀 행: 0", "잘못된 좌표");
        }
    }
}
