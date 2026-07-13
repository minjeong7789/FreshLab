package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.RiskDashboardResponse;
import com.freshlab.freshdoctor.dto.RiskHistoryResponse;
import com.freshlab.freshdoctor.dto.TotalRiskCalculationResult;
import com.freshlab.freshdoctor.service.RiskDashboardService;
import com.freshlab.freshdoctor.service.TotalRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/risks")
@RequiredArgsConstructor
public class TotalRiskController {

    private final TotalRiskService totalRiskService;
    private final RiskDashboardService riskDashboardService;

    @GetMapping("/{itemCode}")
    public RiskDashboardResponse getLatestRisk(@PathVariable String itemCode) {
        return riskDashboardService.getLatestRisk(itemCode);
    }

    @GetMapping("/{itemCode}/history")
    public List<RiskHistoryResponse> getRiskHistory(
            @PathVariable String itemCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return riskDashboardService.getHistory(itemCode, startDate, endDate);
    }

    @PostMapping("/calculate/{itemCode}")
    public TotalRiskCalculationResult calculateRisk(
            @PathVariable String itemCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate scoreDate
    ) {
        return totalRiskService.calculateAndSave(itemCode, scoreDate);
    }

    @PostMapping("/calculate/all")
    public List<TotalRiskCalculationResult> calculateAllRisks(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate scoreDate
    ) {
        return riskDashboardService.calculateAll(scoreDate);
    }
}
