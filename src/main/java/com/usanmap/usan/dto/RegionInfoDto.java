package com.usanmap.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RegionInfoDto {
    private String level;
    private String sidoName;
    private String sigunguName;
    private String emdName;
    private int brokerCount;
    private List<PropertyStatDto> propertyStats;
}
