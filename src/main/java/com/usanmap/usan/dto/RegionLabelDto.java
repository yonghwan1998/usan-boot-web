package com.usanmap.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegionLabelDto {
    private String name;
    private double lat;
    private double lng;
}
