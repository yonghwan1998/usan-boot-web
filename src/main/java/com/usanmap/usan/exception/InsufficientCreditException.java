package com.usanmap.usan.exception;

public class InsufficientCreditException extends RuntimeException {

    public InsufficientCreditException() {
        super("크레딧이 부족합니다.");
    }
}
