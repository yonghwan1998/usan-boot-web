package com.usanmap.usan.batch.job.broker.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedExcelHandler {
    public static final String LAST_COMMITTED_ROW = "broker.lastCommittedExcelRow";
    private final BatchExcelProps props;

    public void handle(JobExecution execution) {
        Path source = Path.of(execution.getJobParameters().getString("inputFile"));
        int row = execution.getStepExecutions().stream()
                .mapToInt(step -> step.getExecutionContext().getInt(LAST_COMMITTED_ROW, 0)).max().orElse(0);
        StringBuilder details = new StringBuilder();
        for (Throwable failure : execution.getAllFailureExceptions()) {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            details.append(trace).append(System.lineSeparator());
        }
        String report = "마지막 커밋 완료 엑셀 행: " + row + "\n"
                + "0은 처리 완료된 데이터 행이 없음을 의미합니다.\n"
                + "행 번호는 원본 엑셀 기준이며, 행 오류로 제외된 데이터도 진행 범위에 포함됩니다.\n"
                + "실패한 청크는 롤백되며, 파일 전체 재실행 시 이전 커밋 데이터가 중복될 수 있습니다.\n"
                + details;
        try {
            Path directory = Path.of(props.getError());
            Files.createDirectories(directory);
            Path target = directory.resolve(source.getFileName());
            if (Files.exists(target)) target = directory.resolve(UUID.randomUUID() + "_" + source.getFileName());
            Files.move(source, target);
            try {
                appendSheet(target, row, report);
            } catch (Exception e) {
                // 손상된 엑셀도 inbox에서 격리하고 진단 내용을 남긴다.
                Files.writeString(target.resolveSibling(target.getFileName() + ".failure.txt"),
                        report + "\n기록 시트 추가 실패: " + e);
                log.warn("Failed to append failure sheet: {}", target, e);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to move failed excel: " + source, e);
        }
    }

    // 워크시트 데이터는 ZIP 스트림으로 복사하여 대용량 파일 전체를 메모리에 올리지 않는다.
    private void appendSheet(Path file, int row, String report) throws Exception {
        Path temp = Files.createTempFile(file.getParent(), "failure-report-", ".xlsx");
        try {
            try (ZipFile zip = new ZipFile(file.toFile());
                 ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(temp))) {
                Document workbook = readXml(zip, "xl/workbook.xml");
                Document relationships = readXml(zip, "xl/_rels/workbook.xml.rels");
                Document types = readXml(zip, "[Content_Types].xml");
                String id = "failure" + UUID.randomUUID().toString().replace("-", "");
                String part = "worksheets/" + id + ".xml";
                Element sheets = (Element) workbook.getElementsByTagNameNS("*", "sheets").item(0);
                int sheetId = 1;
                Set<String> names = new HashSet<>();
                var existing = workbook.getElementsByTagNameNS("*", "sheet");
                for (int i = 0; i < existing.getLength(); i++) {
                    Element sheet = (Element) existing.item(i);
                    sheetId = Math.max(sheetId, Integer.parseInt(sheet.getAttribute("sheetId")) + 1);
                    names.add(sheet.getAttribute("name"));
                }
                String name = "batch_failure";
                for (int i = 2; names.contains(name); i++) name = "batch_failure_" + i;
                Element sheet = workbook.createElementNS(sheets.getNamespaceURI(), "sheet");
                sheet.setAttribute("name", name);
                sheet.setAttribute("sheetId", String.valueOf(sheetId));
                sheet.setAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "r:id", id);
                sheets.appendChild(sheet);
                Element rel = relationships.createElementNS(relationships.getDocumentElement().getNamespaceURI(), "Relationship");
                rel.setAttribute("Id", id);
                rel.setAttribute("Type", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet");
                rel.setAttribute("Target", part);
                relationships.getDocumentElement().appendChild(rel);
                Element type = types.createElementNS(types.getDocumentElement().getNamespaceURI(), "Override");
                type.setAttribute("PartName", "/xl/" + part);
                type.setAttribute("ContentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml");
                types.getDocumentElement().appendChild(type);
                Map<String, Document> changed = Map.of("xl/workbook.xml", workbook,
                        "xl/_rels/workbook.xml.rels", relationships, "[Content_Types].xml", types);
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    output.putNextEntry(new ZipEntry(entry.getName()));
                    if (changed.containsKey(entry.getName())) writeXml(changed.get(entry.getName()), output);
                    else try (InputStream input = zip.getInputStream(entry)) { input.transferTo(output); }
                    output.closeEntry();
                }
                Document data = newDocument();
                String ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
                Element worksheet = data.createElementNS(ns, "worksheet");
                data.appendChild(worksheet);
                Element sheetData = data.createElementNS(ns, "sheetData");
                worksheet.appendChild(sheetData);
                List<String> lines = new ArrayList<>();
                lines.add("마지막 커밋 완료 엑셀 행");
                lines.add(String.valueOf(row));
                lines.addAll(Arrays.asList(report.split("\\R")));
                for (int i = 0; i < Math.min(lines.size(), 1048576); i++) {
                    Element r = data.createElementNS(ns, "row");
                    r.setAttribute("r", String.valueOf(i + 1));
                    Element cell = data.createElementNS(ns, "c");
                    cell.setAttribute("r", "A" + (i + 1));
                    cell.setAttribute("t", "inlineStr");
                    Element inline = data.createElementNS(ns, "is");
                    Element text = data.createElementNS(ns, "t");
                    String value = lines.get(i).replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
                    text.setTextContent(value.substring(0, Math.min(value.length(), 32767)));
                    inline.appendChild(text); cell.appendChild(inline); r.appendChild(cell); sheetData.appendChild(r);
                }
                output.putNextEntry(new ZipEntry("xl/" + part));
                writeXml(data, output);
                output.closeEntry();
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }

    private Document readXml(ZipFile zip, String name) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (InputStream input = zip.getInputStream(zip.getEntry(name))) {
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private Document newDocument() throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    }

    private void writeXml(Document document, OutputStream output) throws Exception {
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(document), new StreamResult(output));
    }
}
