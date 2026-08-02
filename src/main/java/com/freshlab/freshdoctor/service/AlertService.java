package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Alert;
import com.freshlab.freshdoctor.dto.AlertResponse;
import com.freshlab.freshdoctor.dto.UnreadAlertCountResponse;
import com.freshlab.freshdoctor.exception.AlertNotFoundException;
import com.freshlab.freshdoctor.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public List<AlertResponse> getAlerts(Long userId) {
        return alertRepository.findByUserUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(AlertResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadAlertCountResponse getUnreadCount(Long userId) {
        return new UnreadAlertCountResponse(alertRepository.countByUserUserIdAndIsReadFalse(userId));
    }

    @Transactional
    public AlertResponse markAsRead(Long userId, Long alertId) {
        Alert alert = alertRepository.findByIdAndUserUserId(alertId, userId)
                .orElseThrow(AlertNotFoundException::new);
        alert.setIsRead(true);
        return AlertResponse.from(alert);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        alertRepository.markAllAsReadByUserId(userId);
    }
}
