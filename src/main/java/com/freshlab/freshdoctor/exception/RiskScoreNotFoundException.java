package com.freshlab.freshdoctor.exception;

public class RiskScoreNotFoundException extends RuntimeException {

    public RiskScoreNotFoundException(String itemCode) {
        super("No risk score found for itemCode=" + itemCode + ". Calculate risk first.");
    }
}
