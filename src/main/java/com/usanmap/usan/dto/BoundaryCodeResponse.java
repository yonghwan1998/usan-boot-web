package com.usanmap.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BoundaryCodeResponse {

    private String sidoCode;
    private String sigunguCode;
    private String emdCode;

    private String sidoName;
    private String sigunguName;
    private String emdName;
}
