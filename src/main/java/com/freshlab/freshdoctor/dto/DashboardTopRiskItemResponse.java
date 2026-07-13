package com.freshlab.freshdoctor.dto;

public record DashboardTopRiskItemResponse(
        String itemCode,
        String itemName,
        Integer finalScore,
        String riskGrade
) {
}
