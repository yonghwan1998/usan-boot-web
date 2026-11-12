package com.usanmap.usan.batch.job.broker.support;

import com.usanmap.usan.batch.job.broker.reader.BrokerErrorRow;
import com.usanmap.usan.batch.job.broker.reader.SaxExcelRowItemReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorExcelWriterTests {
    @TempDir Path temp;

    @Test
    void errorWorkbookCanBeReadAgainWithOriginalColumnsAndHeaders() throws Exception {
        BatchExcelProps props = new BatchExcelProps();
        props.setSheetName("원본매물");
        props.setHeaderRows(2);
        props.setListingTypeColIndex(6);
        props.setListingCoordinatesColIndex(46);
        props.setOfficeNameColIndex(53);
        props.setBrokerNameColIndex(54);
        props.setAddressColIndex(55);
        props.setRegistrationNumberColIndex(56);
        props.setTelColIndex(57);
        props.setPhoneColIndex(58);
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 60; i++) values.add("");
        values.set(0, "원본 추가 정보");
        values.set(6, "아파트");
        values.set(46, "37.5,127.0");
        values.set(53, "사무소");
        values.set(54, "중개사");
        values.set(55, "서울 강남구");
        values.set(56, "00123");
        values.set(57, "02-001-0001");
        values.set(58, "010-0000-0000");
        List<List<String>> headers = List.of(List.of("제목"), List.of("원본 헤더"));
        BrokerErrorRow error = BrokerErrorRow.builder().sourceValues(values).sourceHeaders(headers)
                .sourceFileName("original.xlsx").rowIndex(1).errorMessage("주소 확인 필요").build();
        new ErrorExcelWriter(props).write(temp.toString(), List.of(error));
        Path output;
        try (var files = Files.list(temp)) { output = files.findFirst().orElseThrow(); }
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(output.toFile())) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
        }
        SaxExcelRowItemReader reader = new SaxExcelRowItemReader(props);
        byte[] originalBytes = Files.readAllBytes(output);
        reader.setResource(new FileSystemResource(output) {
            @Override
            public java.io.InputStream getInputStream() {
                throw new AssertionError("Excel must be opened directly as a file");
            }
        });
        reader.open(new ExecutionContext());
        try {
            var row = reader.read();
            assertThat(row.getSourceValues().subList(0, values.size())).isEqualTo(values);
            assertThat(row.getSourceValues().get(values.size())).isEqualTo("주소 확인 필요");
            assertThat(row.getSourceHeaders().get(0)).isEqualTo(headers.get(0));
            assertThat(row.getSourceHeaders().get(1).get(values.size())).isEqualTo("errorMessage");
            assertThat(row.getListingCoordinates()).isEqualTo("37.5,127.0");
            assertThat(row.getRegistrationNumber()).isEqualTo("00123");
            assertThat(reader.read()).isNull();
            Path retry = Files.createDirectory(temp.resolve("retry"));
            BrokerErrorRow retried = BrokerErrorRow.builder().sourceValues(row.getSourceValues())
                    .sourceHeaders(row.getSourceHeaders()).errorMessage("수정 필요").build();
            new ErrorExcelWriter(props).write(retry.toString(), List.of(retried));
            try (var files = Files.list(retry);
                 var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(files.findFirst().orElseThrow().toFile())) {
                var saved = workbook.getSheetAt(0).getRow(2);
                assertThat((int) saved.getLastCellNum()).isEqualTo(values.size() + 1);
                assertThat(saved.getCell(values.size()).getStringCellValue()).isEqualTo("수정 필요");
            }
        } finally { reader.close(); }
        assertThat(Files.readAllBytes(output)).isEqualTo(originalBytes);
    }
}
