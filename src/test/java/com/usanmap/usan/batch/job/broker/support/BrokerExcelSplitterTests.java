package com.usanmap.usan.batch.job.broker.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class BrokerExcelSplitterTests {
    @TempDir Path temp;
    @Test
    void recursivelySplitsAndPreservesAllRows() throws Exception {
        Path input = temp.resolve("input.xlsx");
        List<String> expected = new ArrayList<>();
        try (var wb = new XSSFWorkbook(); var out = Files.newOutputStream(input)) {
            var sheet = wb.createSheet("data");
            sheet.createRow(0).createCell(0).setCellValue("header");
            for (int i = 0; i < 300; i++) {
                String value = UUID.randomUUID().toString().repeat(3);
                expected.add(value); sheet.createRow(i + 1).createCell(0).setCellValue(value);
            }
            wb.write(out);
        }
        BatchExcelProps props = new BatchExcelProps();
        props.setHeaderRows(1); props.setMaxFileBytes(6000);
        props.setArchive(temp.resolve("archive").toString());
        var files = new BrokerExcelSplitter(props).prepare(input);
        assertThat(files.size()).isGreaterThan(2);
        List<String> actual = new ArrayList<>();
        for (Path file : files) {
            assertThat(Files.size(file)).isLessThan(6000);
            try (var wb = new XSSFWorkbook(file.toFile())) {
                var sheet = wb.getSheetAt(0);
                assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("header");
                for (int i = 1; i <= sheet.getLastRowNum(); i++) actual.add(sheet.getRow(i).getCell(0).getStringCellValue());
            }
        }
        assertThat(actual).containsExactlyElementsOf(expected);
        assertThat(input).doesNotExist();
    }

    @Test
    void impossibleLimitLeavesOriginalAndPublishesNothing() throws Exception {
        Path input = temp.resolve("input.xlsx");
        try (var wb = new XSSFWorkbook(); var out = Files.newOutputStream(input)) {
            wb.createSheet("data").createRow(0).createCell(0).setCellValue("only row"); wb.write(out);
        }
        BatchExcelProps props = new BatchExcelProps(); props.setMaxFileBytes(1);
        assertThatThrownBy(() -> new BrokerExcelSplitter(props).prepare(input)).isInstanceOf(IllegalStateException.class);
        assertThat(input).exists();
        try (var files = Files.list(temp)) { assertThat(files.toList()).containsExactly(input); }
    }

    @Test
    void suppliedWorkbookCanBeSplitUnderDefaultLimit() throws Exception {
        String path = System.getenv("USAN_SPLIT_TEST_FILE");
        org.junit.jupiter.api.Assumptions.assumeTrue(path != null);
        Path input = temp.resolve("input.xlsx"); Files.copy(Path.of(path), input);
        BatchExcelProps props = new BatchExcelProps(); props.setHeaderRows(1);
        props.setArchive(temp.resolve("archive").toString());
        props.setMaxFileBytes(Math.min(props.getMaxFileBytes(), Files.size(input) / 2));
        var files = new BrokerExcelSplitter(props).prepare(input);
        assertThat(files.size()).isGreaterThan(1);
        for (Path file : files) assertThat(Files.size(file)).isLessThan(props.getMaxFileBytes());
        System.out.println("ACTUAL_SPLIT_SIZES=" + files.stream().map(f -> { try { return Files.size(f); } catch (Exception e) { throw new RuntimeException(e); } }).toList());
    }
}
