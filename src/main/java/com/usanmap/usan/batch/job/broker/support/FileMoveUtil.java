package com.usanmap.usan.batch.job.broker.support;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
public class FileMoveUtil {

    private final BatchExcelProps props;

    public void moveToArchive(String inputFile) {
        File file = new File(inputFile);
        try {
            FileUtils.moveFileToDirectory(file, new File(props.getArchive()), true);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to archive excel: " + inputFile, e);
        }
    }
}
