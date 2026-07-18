package com.freshlab.freshdoctor.dto;

import com.freshlab.freshdoctor.domain.Alert;
import com.freshlab.freshdoctor.domain.AlertType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AlertResponse(
        Long alertId,
        String itemCode,
        String itemName,
        AlertType alertType,
        Integer previousScore,
        String previousGrade,
        Integer currentScore,
        String currentGrade,
        String title,
        String description,
        String evidence,
        LocalDate riskScoreDate,
        LocalDateTime occurredAt,
        boolean read
) {
    public static AlertResponse from(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getItemCode(),
                alert.getItemName(),
                alert.getAlertType(),
                alert.getPreviousScore(),
                alert.getPreviousGrade(),
                alert.getCurrentScore() == null ? alert.getScore() : alert.getCurrentScore(),
                alert.getCurrentGrade(),
                alert.getTitle(),
                alert.getDescription() == null ? alert.getMessage() : alert.getDescription(),
                alert.getEvidence(),
                alert.getRiskScoreDate(),
                alert.getOccurredAt() == null ? alert.getCreatedAt() : alert.getOccurredAt(),
                Boolean.TRUE.equals(alert.getIsRead())
        );
    }
}
