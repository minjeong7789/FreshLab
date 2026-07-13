package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.RecommendationResponse;
import com.freshlab.freshdoctor.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{itemCode}")
    public RecommendationResponse getLatestRecommendation(@PathVariable String itemCode) {
        return recommendationService.getLatest(itemCode);
    }

    @PostMapping("/generate/{itemCode}")
    public RecommendationResponse generateRecommendation(@PathVariable String itemCode) {
        return recommendationService.generate(itemCode);
    }
}
