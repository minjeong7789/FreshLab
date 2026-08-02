package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.RiskScoreResponse;
import com.freshlab.freshdoctor.dto.RiskScoreUpsertRequest;
import com.freshlab.freshdoctor.service.RiskScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/risk-scores")
@RequiredArgsConstructor
public class RiskScoreController {

    private final RiskScoreService riskScoreService;

    @PostMapping
    public RiskScoreResponse upsertRiskScore(@RequestBody RiskScoreUpsertRequest request) {
        return riskScoreService.upsert(request);
    }

    @GetMapping("/{itemCode}/latest")
    public RiskScoreResponse getLatestRiskScore(@PathVariable String itemCode) {
        return riskScoreService.getLatest(itemCode);
    }

    @GetMapping("/{itemCode}/{scoreDate}")
    public RiskScoreResponse getRiskScoreByDate(
            @PathVariable String itemCode,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate scoreDate
    ) {
        return riskScoreService.getByDate(itemCode, scoreDate);
    }

    @GetMapping("/{itemCode}")
    public List<RiskScoreResponse> getRiskScores(
            @PathVariable String itemCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return riskScoreService.getScores(itemCode, startDate, endDate);
    }
}
