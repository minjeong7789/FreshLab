package com.freshlab.freshdoctor.exception;

public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException(String itemCode) {
        super("No recommendation found for itemCode=" + itemCode);
    }
}
