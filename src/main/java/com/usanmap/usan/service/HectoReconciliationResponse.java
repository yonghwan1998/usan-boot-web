package com.usanmap.usan.service;

import java.util.List;

public record HectoReconciliationResponse(
        String resultCode,
        String resultMessage,
        List<HectoReconciliationTransaction> transactions
) {}
