package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.NewsRiskResponse;
import com.freshlab.freshdoctor.service.NewsRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news-risk")
@RequiredArgsConstructor
public class NewsRiskController {

    private final NewsRiskService newsRiskService;

    @GetMapping("/{itemCode}")
    public NewsRiskResponse getNewsRisk(@PathVariable String itemCode) {
        return newsRiskService.calculateRisk(itemCode);
    }
}
