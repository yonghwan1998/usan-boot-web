package com.usanmap.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BoundarySeedResponse {

    private boolean success;
    private String message;

    private int deletedCount;
    private int sidoCount;

    private int sigunguFileCount;
    private int sigunguCount;

    private int emdFileCount;
    private int emdCount;
}
