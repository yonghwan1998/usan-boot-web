package com.usanmap.usan.dto;

import java.time.LocalDateTime;

public record SmsDeliveryResult(String requestId, String messageId, String to,
                                String status, String statusName, String statusCode,
                                LocalDateTime completeTime) {}
