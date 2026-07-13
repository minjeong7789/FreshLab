package com.freshlab.freshdoctor.dto;

public record DashboardGradeCountsResponse(
        int stable,
        int watch,
        int caution,
        int alert,
        int severe
) {
}
