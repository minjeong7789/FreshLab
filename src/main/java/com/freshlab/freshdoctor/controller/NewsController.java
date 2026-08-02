package com.freshlab.freshdoctor.controller;

import com.freshlab.freshdoctor.dto.NewsCollectResult;
import com.freshlab.freshdoctor.dto.NewsResponse;
import com.freshlab.freshdoctor.service.NaverNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NaverNewsService naverNewsService;

    @PostMapping("/collect/{itemCode}")
    public NewsCollectResult collectNews(
            @PathVariable String itemCode,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "20") int display
    ) {
        return naverNewsService.collectNews(itemCode, query, display);
    }

    @GetMapping("/{itemCode}")
    public List<NewsResponse> getNews(@PathVariable String itemCode) {
        return naverNewsService.getNews(itemCode);
    }

    @GetMapping("/{itemCode}/representative-risk")
    public NewsResponse getRepresentativeRiskNews(@PathVariable String itemCode) {
        return naverNewsService.getRepresentativeRiskNews(itemCode);
    }
}
