package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.*;
import com.freshlab.freshdoctor.repository.AlertRepository;
import com.freshlab.freshdoctor.repository.FcmDeliveryLogRepository;
import com.freshlab.freshdoctor.repository.FcmRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FcmPushService {

    private final AlertRepository alertRepository;
    private final FcmRegistrationRepository registrationRepository;
    private final FcmDeliveryLogRepository deliveryLogRepository;
    private final PushGateway pushGateway;

    @Transactional
    public void sendAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId).orElse(null);
        if (alert == null || alert.getUser() == null) {
            log.warn("FCM delivery skipped because alert was not found. alertId={}", alertId);
            return;
        }

        List<FcmRegistration> registrations =
                registrationRepository.findByUserUserIdAndActiveTrueOrderByIdAsc(alert.getUser().getUserId());
        PushMessage message = toPushMessage(alert);

        int successCount = 0;
        int failureCount = 0;
        for (FcmRegistration registration : registrations) {
            PushSendResult result = pushGateway.send(registration.getRegistrationKey(), message);
            saveDeliveryLog(alertId, registration.getId(), result);
            if (result.success()) {
                successCount++;
            } else {
                failureCount++;
                if (result.invalidRegistration()) {
                    registration.deactivate();
                }
            }
        }
        log.info("FCM delivery completed. alertId={}, targetCount={}, successCount={}, failureCount={}",
                alertId, registrations.size(), successCount, failureCount);
    }

    private PushMessage toPushMessage(Alert alert) {
        String targetUrl = AlertType.DAILY_SUMMARY == alert.getAlertType()
                ? "/alerts"
                : "/items/" + alert.getItemCode();
        Map<String, String> data = new LinkedHashMap<>();
        data.put("alertId", String.valueOf(alert.getId()));
        data.put("itemCode", valueOrEmpty(alert.getItemCode()));
        data.put("alertType", alert.getAlertType().name());
        data.put("riskGrade", valueOrEmpty(alert.getCurrentGrade()));
        data.put("title", valueOrEmpty(alert.getTitle()));
        data.put("body", valueOrEmpty(alert.getDescription()));
        data.put("targetUrl", targetUrl);
        return new PushMessage(
                valueOrEmpty(alert.getTitle()),
                valueOrEmpty(alert.getDescription()),
                targetUrl,
                data
        );
    }

    private void saveDeliveryLog(Long alertId, Long registrationId, PushSendResult result) {
        FcmDeliveryLog deliveryLog = new FcmDeliveryLog();
        deliveryLog.setAlertId(alertId);
        deliveryLog.setRegistrationId(registrationId);
        deliveryLog.setStatus(result.success() ? FcmDeliveryStatus.SUCCESS : FcmDeliveryStatus.FAILED);
        deliveryLog.setMessageId(result.messageId());
        deliveryLog.setErrorCode(result.errorCode());
        deliveryLog.setSentAt(LocalDateTime.now());
        deliveryLogRepository.save(deliveryLog);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
