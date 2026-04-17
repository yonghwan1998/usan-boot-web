package com.usanmap.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PropertyStatDto {
    private String label;
    private String cssClass;
    private int count;
    private int percentage;
}
