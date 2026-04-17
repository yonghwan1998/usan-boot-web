package com.usanmap.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegionEmdItemDto {
    private String admCd;
    private String name;
    private double lat;
    private double lng;
}
