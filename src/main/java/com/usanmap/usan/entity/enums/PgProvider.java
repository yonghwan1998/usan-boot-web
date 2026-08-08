package com.usanmap.usan.entity.enums;

public enum PgProvider {
    /**
     * @deprecated 기존 토스페이먼츠 연동은 삭제됨. 과거 결제 이력(DB) 호환을 위해서만 유지.
     */
    @Deprecated
    TOSS,
    HECTO,
    BANK_TRANSFER,
    /**
     * @deprecated 기존 NHN KCP 연동은 삭제됨. 과거 결제 이력(DB) 호환을 위해서만 유지.
     */
    @Deprecated
    KCP
}
