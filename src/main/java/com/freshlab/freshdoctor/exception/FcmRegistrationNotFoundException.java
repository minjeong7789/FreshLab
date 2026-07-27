package com.freshlab.freshdoctor.exception;

public class FcmRegistrationNotFoundException extends RuntimeException {
    public FcmRegistrationNotFoundException() {
        super("FCM registration not found.");
    }
}
