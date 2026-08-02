package com.freshlab.freshdoctor.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DashboardResponse(
        Integer todayScore,
        String todayGrade,
        String summary,
        List<DashboardTopRiskItemResponse> topRiskItems,
        DashboardGradeCountsResponse gradeCounts,
        List<DashboardItemResponse> items,
        String aiRecommendation,
        LocalDate dataDate,
        LocalDateTime lastUpdatedAt
) {
}
