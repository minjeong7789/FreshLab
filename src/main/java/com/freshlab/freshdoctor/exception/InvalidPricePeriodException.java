package com.freshlab.freshdoctor.exception;

public class InvalidPricePeriodException extends RuntimeException {

    public InvalidPricePeriodException() {
        super("days는 7, 14, 30 중 하나여야 합니다.");
    }
}
