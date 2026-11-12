package com.usanmap.usan.batch.job.broker.support;

import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class BrokerExcelSplitter {
    private final BatchExcelProps props;

    public List<Path> prepare(Path source) throws Exception {
        long limit = props.getMaxFileBytes();
        if (limit <= 0) throw new IllegalArgumentException("max-file-bytes must be positive");
        if (Files.size(source) < limit) return List.of(source);
        Path staging = Files.createTempDirectory(source.getParent(), ".split-");
        List<Path> published = new ArrayList<>();
        boolean committed = false;
        try {
            List<Path> leaves = new ArrayList<>();
            divide(source, staging, limit, leaves);
            for (Path leaf : leaves) {
                Path target = source.resolveSibling("split_" + UUID.randomUUID() + ".xlsx");
                Files.move(leaf, target);
                published.add(target);
            }
            Path originals = Path.of(props.getArchive()).resolve("split-originals");
            Files.createDirectories(originals);
            Files.move(source, originals.resolve(UUID.randomUUID() + "_" + source.getFileName()));
            committed = true;
            return published;
        } finally {
            if (!committed) for (Path file : published) Files.deleteIfExists(file);
            try (var paths = Files.walk(staging)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private void divide(Path source, Path directory, long limit, List<Path> leaves) throws Exception {
        if (Files.size(source) < limit) { leaves.add(source); return; }
        long[] count = {0};
        scan(source, row -> count[0]++);
        long dataRows = count[0] - props.getHeaderRows();
        if (dataRows <= 1) throw new IllegalStateException("Cannot split below size limit: " + source);
        Path left = directory.resolve(UUID.randomUUID() + ".xlsx");
        Path right = directory.resolve(UUID.randomUUID() + ".xlsx");
        SXSSFWorkbook a = new SXSSFWorkbook(50);
        SXSSFWorkbook b = new SXSSFWorkbook(50);
        try (a; b) {
            a.setCompressTempFiles(true); b.setCompressTempFiles(true);
            String name = props.getSheetName();
            if (name == null || name.isBlank()) name = "data";
            var sa = a.createSheet(name); var sb = b.createSheet(name);
            long[] index = {0}; int[] ra = {0}, rb = {0};
            scan(source, values -> {
                long i = index[0]++;
                if (i < props.getHeaderRows()) {
                    write(sa.createRow(ra[0]++), values); write(sb.createRow(rb[0]++), values);
                } else if (i - props.getHeaderRows() < dataRows / 2) {
                    write(sa.createRow(ra[0]++), values);
                } else write(sb.createRow(rb[0]++), values);
            });
            try (var out = Files.newOutputStream(left)) { a.write(out); }
            try (var out = Files.newOutputStream(right)) { b.write(out); }
        } finally { a.dispose(); b.dispose(); }
        divide(left, directory, limit, leaves);
        divide(right, directory, limit, leaves);
    }

    private void write(org.apache.poi.ss.usermodel.Row row, List<String> values) {
        for (int i = 0; i < values.size(); i++) row.createCell(i).setCellValue(values.get(i));
    }

    // 전체 행 목록 없이 SAX에서 한 행씩 전달한다.
    private void scan(Path source, Consumer<List<String>> consumer) throws Exception {
        try (var pkg = OPCPackage.open(source.toFile(), PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            var strings = new ReadOnlySharedStringsTable(pkg);
            var sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (sheets.hasNext()) {
                try (InputStream input = sheets.next()) {
                    String selected = props.getSheetName();
                    if (selected != null && !selected.isBlank() && !selected.equals(sheets.getSheetName())) continue;
                    var parser = XMLHelper.newXMLReader();
                    var handler = new XSSFSheetXMLHandler.SheetContentsHandler() {
                        private List<String> row;
                        public void startRow(int number) { row = new ArrayList<>(); }
                        public void cell(String ref, String value, XSSFComment comment) {
                            int col = new CellReference(ref).getCol();
                            while (row.size() <= col) row.add("");
                            row.set(col, value == null ? "" : value);
                        }
                        public void endRow(int number) { consumer.accept(row); }
                    };
                    parser.setContentHandler(new XSSFSheetXMLHandler(reader.getStylesTable(), null,
                            strings, handler, new DataFormatter(), false));
                    parser.parse(new org.xml.sax.InputSource(input));
                    return;
                }
            }
            throw new IllegalStateException("Sheet not found: " + props.getSheetName());
        }
    }
}
