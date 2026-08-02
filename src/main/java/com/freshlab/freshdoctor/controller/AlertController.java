package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.AlertResponse;
import com.freshlab.freshdoctor.dto.UnreadAlertCountResponse;
import com.freshlab.freshdoctor.security.CurrentUserId;
import com.freshlab.freshdoctor.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<AlertResponse> getAlerts(@CurrentUserId Long userId) {
        return alertService.getAlerts(userId);
    }

    @GetMapping("/unread-count")
    public UnreadAlertCountResponse getUnreadCount(@CurrentUserId Long userId) {
        return alertService.getUnreadCount(userId);
    }

    @PatchMapping("/{alertId}/read")
    public AlertResponse markAsRead(
            @CurrentUserId Long userId,
            @PathVariable Long alertId
    ) {
        return alertService.markAsRead(userId, alertId);
    }

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@CurrentUserId Long userId) {
        alertService.markAllAsRead(userId);
    }
}
