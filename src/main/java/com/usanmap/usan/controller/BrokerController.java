package com.usanmap.usan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.usanmap.usan.batch.job.broker.support.BatchExcelProps;

import java.io.File;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/broker")
public class BrokerController {

    private final BatchExcelProps props;

    /**
     * @date    2025-11-18
     * @author  yongss
     * @param   file
     *
     * 처리 과정:
     *  - /broker/excel/upload POST 요청
     *  - inbox 경로로 파일 업로드
     */
    @PostMapping("/excel/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file")MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String name = StringUtils.cleanPath(file.getOriginalFilename());
        File dest = new File(props.getInbox(), name);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        return ResponseEntity.ok("uploaded: " + name);
    }
}
