package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.NewsArticle;
import com.freshlab.freshdoctor.dto.NewsRiskResponse;
import com.freshlab.freshdoctor.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsRiskService {

    private final ItemService itemService;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsRiskCalculator newsRiskCalculator;

    @Transactional
    public NewsRiskResponse calculateRisk(String itemCode) {
        itemService.getItem(itemCode);
        List<NewsArticle> articles = newsArticleRepository
                .findTop50ByItemCodeOrderByPublishedAtDescCreatedAtDesc(itemCode);

        NewsRiskResponse response = newsRiskCalculator.calculate(itemCode, articles);
        if (response.representativeArticleId() != null) {
            saveRiskResult(itemCode, response);
        }
        return response;
    }

    private void saveRiskResult(String itemCode, NewsRiskResponse response) {
        List<NewsArticle> representatives = newsArticleRepository.findByItemCodeAndRepresentativeRiskTrue(itemCode);
        representatives.forEach(article -> article.setRepresentativeRisk(false));

        newsArticleRepository.findById(response.representativeArticleId())
                .ifPresent(article -> {
                    article.setRepresentativeRisk(true);
                    article.setNewsRiskScore(response.score());
                    article.setNewsRiskType(response.riskType().name());
                    article.setRiskReason(response.reason());
                    article.setMatchedKeywords(String.join(",", response.matchedKeywords()));
                });
    }
}
