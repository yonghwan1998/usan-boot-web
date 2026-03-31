package com.usanmap.usan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BrokerPropertyTagDto {

    private String label;
    private int count;
    private String cssClass;
}
