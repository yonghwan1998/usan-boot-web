package com.usanmap.usan.controller;

import lombok.RequiredArgsConstructor;
import com.usanmap.usan.service.BrokerExcelIngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @date    2025-11-19
 * @author  yongss
 * @desc    brokerExcelIngestJob 수동 실행 전용 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/broker")
public class BrokerExcelIngestController {

    private final BrokerExcelIngestService brokerExcelIngestService;

    /**
     * @date    2025-11-19
     * @author  yongss
     *
     * 처리 과정:
     *  - /broker/batch/run-broker-excel-ingest POST 요청
     *  - brokerExcelIngestJob 수동 실행
     */
    @PostMapping("/batch/run-broker-excel-ingest")
    public ResponseEntity<String> run() throws Exception {

        brokerExcelIngestService.run();

        return ResponseEntity.ok("brokerExcelIngestJob batch started");
    }
}
