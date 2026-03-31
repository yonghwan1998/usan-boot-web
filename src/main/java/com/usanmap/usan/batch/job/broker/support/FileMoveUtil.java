package com.usanmap.usan.batch.job.broker.support;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@RequiredArgsConstructor
public class FileMoveUtil {

    private final BatchExcelProps props;

    public void moveAllToArchive() {

        File inbox = new File(props.getInbox());
        File[] files = inbox.listFiles((dir, name) -> name.toLowerCase().endsWith(".xlsx"));

        if (files == null) {
            return;
        }

        for (File f : files) {
            try {
                FileUtils.moveFileToDirectory(f, new File(props.getArchive()), true);
            } catch (Exception e) {
                try {
                    FileUtils.moveFileToDirectory(f, new File(props.getError()), true);
                } catch (Exception ignored) {}
            }
        }
    }
}
