package com.freshlab.freshdoctor.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FirebasePushGateway implements PushGateway {

    private static final int MAX_ATTEMPTS = 2;
    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushSendResult send(String registrationKey, PushMessage pushMessage) {
        Message message = Message.builder()
                .setToken(registrationKey)
                .setNotification(Notification.builder()
                        .setTitle(pushMessage.title())
                        .setBody(pushMessage.body())
                        .build())
                .putAllData(pushMessage.data())
                .build();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return PushSendResult.success(firebaseMessaging.send(message));
            } catch (FirebaseMessagingException exception) {
                MessagingErrorCode code = exception.getMessagingErrorCode();
                if (isTransient(code) && attempt < MAX_ATTEMPTS) {
                    continue;
                }
                return PushSendResult.failure(errorCode(exception), code == MessagingErrorCode.UNREGISTERED);
            }
        }
        return PushSendResult.failure("UNKNOWN", false);
    }

    private boolean isTransient(MessagingErrorCode code) {
        return code == MessagingErrorCode.INTERNAL || code == MessagingErrorCode.UNAVAILABLE;
    }

    private String errorCode(FirebaseMessagingException exception) {
        return exception.getMessagingErrorCode() == null
                ? "UNKNOWN"
                : exception.getMessagingErrorCode().name();
    }
}
