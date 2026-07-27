package com.freshlab.freshdoctor.event;

import com.freshlab.freshdoctor.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
public class AlertCreatedEventListener {

    private final FcmPushService fcmPushService;

    @Async("fcmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAlertCreated(AlertCreatedEvent event) {
        fcmPushService.sendAlert(event.alertId());
    }
}
