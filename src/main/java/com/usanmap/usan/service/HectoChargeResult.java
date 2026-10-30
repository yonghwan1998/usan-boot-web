package com.usanmap.usan.service;

import com.usanmap.usan.entity.enums.OrderStatus;

public record HectoChargeResult(
        Outcome outcome,
        OrderStatus previousStatus,
        OrderStatus currentStatus
) {
    public enum Outcome {
        COMPLETED,
        RECOVERED,
        FAILED,
        ALREADY_PROCESSED
    }
}
