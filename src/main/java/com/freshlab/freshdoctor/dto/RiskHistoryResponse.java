package com.freshlab.freshdoctor.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RiskHistoryResponse(
        String itemCode,
        LocalDate scoreDate,
        Integer finalScore,
        String riskGrade,
        Integer rawScore,
        LocalDateTime lastUpdatedAt
) {

    public static RiskHistoryResponse from(RiskScoreResponse riskScore) {
        return new RiskHistoryResponse(
                riskScore.itemCode(),
                riskScore.scoreDate(),
                riskScore.finalScore(),
                riskScore.riskGrade(),
                riskScore.rawScore(),
                riskScore.updatedAt()
        );
    }
}
