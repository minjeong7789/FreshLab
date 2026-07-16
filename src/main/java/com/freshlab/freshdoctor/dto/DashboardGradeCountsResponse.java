package com.freshlab.freshdoctor.dto;

public record DashboardGradeCountsResponse(
        int safe,
        int interest,
        int caution,
        int alert,
        int critical
) {
}
