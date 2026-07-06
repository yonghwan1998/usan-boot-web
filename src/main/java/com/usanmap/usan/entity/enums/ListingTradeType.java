package com.usanmap.usan.entity.enums;

public enum ListingTradeType {
    SALE("매매"),
    JEONSE("전세"),
    MONTHLY("월세"),
    SEMI_JEONSE("반전세"),
    SHORT("단기임대"),
    COMMERCIAL("상가임대");

    private final String label;

    ListingTradeType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
