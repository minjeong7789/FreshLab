package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.TotalRiskCalculationResult;
import com.freshlab.freshdoctor.service.TotalRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/risks")
@RequiredArgsConstructor
public class TotalRiskController {

    private final TotalRiskService totalRiskService;

    @PostMapping("/calculate/{itemCode}")
    public TotalRiskCalculationResult calculateRisk(
            @PathVariable String itemCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate scoreDate
    ) {
        return totalRiskService.calculateAndSave(itemCode, scoreDate);
    }
}
