package com.freshlab.freshdoctor.exception;

public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException() {
        super("Alert not found.");
    }
}
