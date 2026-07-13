package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.DashboardResponse;
import com.freshlab.freshdoctor.dto.RiskDashboardResponse;
import com.freshlab.freshdoctor.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }

    @GetMapping("/items/{itemCode}")
    public RiskDashboardResponse getItemDashboard(@PathVariable String itemCode) {
        return dashboardService.getItemDashboard(itemCode);
    }
}
