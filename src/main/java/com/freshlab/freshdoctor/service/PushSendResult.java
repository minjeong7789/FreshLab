package com.freshlab.freshdoctor.service;

public record PushSendResult(
        boolean success,
        String messageId,
        String errorCode,
        boolean invalidRegistration
) {
    public static PushSendResult success(String messageId) {
        return new PushSendResult(true, messageId, null, false);
    }

    public static PushSendResult failure(String errorCode, boolean invalidRegistration) {
        return new PushSendResult(false, null, errorCode, invalidRegistration);
    }
}
