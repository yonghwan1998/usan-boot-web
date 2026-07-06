package com.usanmap.usan.entity.enums;

public enum ListingType {
    APT("아파트"),
    OFFICETEL("오피스텔"),
    VILLA("빌라/연립"),
    ONEROOM("원룸"),
    TWOROOM("투룸"),
    DETACHED("단독/다가구"),
    RURAL("전원주택"),
    MIXEDHOUSE("상가주택"),
    HANOK("한옥주택"),
    STORE("상가"),
    OFFICE("사무실"),
    BUILDING("건물"),
    FACTORY("공장/창고"),
    KNOWLEDGE("지식산업센터"),
    LAND("토지"),
    APT_SALE("아파트분양권"),
    OFFICETEL_SALE("오피스텔분양권"),
    REDEVELOPMENT("재개발"),
    RECONSTRUCTION("재건축"),
    PRESALE("분양중/예정");

    private final String label;

    ListingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
