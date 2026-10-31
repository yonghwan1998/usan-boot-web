package com.usanmap.usan.service;

import java.time.LocalDate;
import java.time.LocalTime;

public record HectoReconciliationTransaction(
        String merchantId,
        String method,
        String transactionNo,
        String orderNo,
        Type type,
        LocalDate transactionDate,
        LocalTime transactionTime,
        int amount,
        String originalTransactionNo
) {
    public enum Type {
        APPROVAL,
        CANCELLATION
    }
}
