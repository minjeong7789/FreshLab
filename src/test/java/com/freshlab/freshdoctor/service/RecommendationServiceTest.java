package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.ActionRecommendation;
import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.RecommendationGenerationType;
import com.freshlab.freshdoctor.domain.RiskScore;
import com.freshlab.freshdoctor.dto.RecommendationInput;
import com.freshlab.freshdoctor.dto.RecommendationResponse;
import com.freshlab.freshdoctor.exception.RecommendationNotFoundException;
import com.freshlab.freshdoctor.exception.RiskScoreNotFoundException;
import com.freshlab.freshdoctor.repository.ActionRecommendationRepository;
import com.freshlab.freshdoctor.repository.RiskScoreRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private final ActionRecommendationRepository recommendationRepository =
            mock(ActionRecommendationRepository.class);
    private final RiskScoreRepository riskScoreRepository = mock(RiskScoreRepository.class);
    private final ItemService itemService = mock(ItemService.class);
    private final OpenAiRecommendationClient openAiRecommendationClient = mock(OpenAiRecommendationClient.class);
    private final RecommendationService recommendationService = new RecommendationService(
            recommendationRepository,
            riskScoreRepository,
            itemService,
            openAiRecommendationClient
    );

    @Test
    void generatesGptRecommendationAndStoresIt() throws Exception {
        Item item = item();
        RiskScore riskScore = riskScore();
        when(itemService.getItem("1001")).thenReturn(item);
        when(riskScoreRepository.findTopByItemCodeOrderByScoreDateDescIdDesc("1001"))
                .thenReturn(Optional.of(riskScore));
        when(recommendationRepository.findByInputHash(any())).thenReturn(Optional.empty());
        when(openAiRecommendationClient.generate(any(RecommendationInput.class)))
                .thenReturn("배추 발주는 평소보다 줄이고 대체 품목을 확인하세요.");
        when(recommendationRepository.save(any(ActionRecommendation.class)))
                .thenAnswer(invocation -> {
                    ActionRecommendation saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        RecommendationResponse response = recommendationService.generate("1001");

        assertThat(response.recommendation()).isEqualTo("배추 발주는 평소보다 줄이고 대체 품목을 확인하세요.");
        assertThat(response.generationType()).isEqualTo("GPT");
        assertThat(riskScore.getRecommendation()).isEqualTo(response.recommendation());
        verify(openAiRecommendationClient).generate(any(RecommendationInput.class));
    }

    @Test
    void reusesStoredRecommendationForSameInputHash() throws Exception {
        Item item = item();
        RiskScore riskScore = riskScore();
        ActionRecommendation cached = recommendation("이미 저장된 추천", RecommendationGenerationType.GPT);
        when(itemService.getItem("1001")).thenReturn(item);
        when(riskScoreRepository.findTopByItemCodeOrderByScoreDateDescIdDesc("1001"))
                .thenReturn(Optional.of(riskScore));
        when(recommendationRepository.findByInputHash(any())).thenReturn(Optional.of(cached));

        RecommendationResponse response = recommendationService.generate("1001");

        assertThat(response.recommendation()).isEqualTo("이미 저장된 추천");
        assertThat(response.generationType()).isEqualTo("GPT");
        verify(openAiRecommendationClient, never()).generate(any(RecommendationInput.class));
        verify(recommendationRepository, never()).save(any(ActionRecommendation.class));
    }

    @Test
    void retriesGptWhenCachedRecommendationIsFallback() throws Exception {
        Item item = item();
        RiskScore riskScore = riskScore();
        ActionRecommendation cached = recommendation("이전 fallback 추천", RecommendationGenerationType.FALLBACK);
        when(itemService.getItem("1001")).thenReturn(item);
        when(riskScoreRepository.findTopByItemCodeOrderByScoreDateDescIdDesc("1001"))
                .thenReturn(Optional.of(riskScore));
        when(recommendationRepository.findByInputHash(any())).thenReturn(Optional.of(cached));
        when(openAiRecommendationClient.generate(any(RecommendationInput.class)))
                .thenReturn("새 GPT 추천");
        when(recommendationRepository.save(cached)).thenReturn(cached);

        RecommendationResponse response = recommendationService.generate("1001");

        assertThat(response.recommendation()).isEqualTo("새 GPT 추천");
        assertThat(response.generationType()).isEqualTo("GPT");
        verify(openAiRecommendationClient).generate(any(RecommendationInput.class));
        verify(recommendationRepository).save(cached);
    }

    @Test
    void usesFallbackRecommendationWhenGptFails() throws Exception {
        Item item = item();
        RiskScore riskScore = riskScore();
        when(itemService.getItem("1001")).thenReturn(item);
        when(riskScoreRepository.findTopByItemCodeOrderByScoreDateDescIdDesc("1001"))
                .thenReturn(Optional.of(riskScore));
        when(recommendationRepository.findByInputHash(any())).thenReturn(Optional.empty());
        when(openAiRecommendationClient.generate(any(RecommendationInput.class)))
                .thenThrow(new IllegalStateException("OpenAI API key is empty."));
        when(recommendationRepository.save(any(ActionRecommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RecommendationResponse response = recommendationService.generate("1001");

        assertThat(response.generationType()).isEqualTo("FALLBACK");
        assertThat(response.recommendation()).contains("배추");
        assertThat(response.recommendation()).contains("발주");
    }

    @Test
    void returnsLatestStoredRecommendation() {
        ActionRecommendation cached = recommendation("최근 추천", RecommendationGenerationType.FALLBACK);
        when(itemService.getItem("1001")).thenReturn(item());
        when(recommendationRepository.findTopByItemCodeOrderByUpdatedAtDescIdDesc("1001"))
                .thenReturn(Optional.of(cached));

        RecommendationResponse response = recommendationService.getLatest("1001");

        assertThat(response.recommendation()).isEqualTo("최근 추천");
        assertThat(response.generationType()).isEqualTo("FALLBACK");
    }

    @Test
    void rejectsGenerateWhenRiskScoreDoesNotExist() {
        when(itemService.getItem("1001")).thenReturn(item());
        when(riskScoreRepository.findTopByItemCodeOrderByScoreDateDescIdDesc("1001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.generate("1001"))
                .isInstanceOf(RiskScoreNotFoundException.class)
                .hasMessageContaining("Calculate risk first");
    }

    @Test
    void rejectsGetLatestWhenRecommendationDoesNotExist() {
        when(itemService.getItem("1001")).thenReturn(item());
        when(recommendationRepository.findTopByItemCodeOrderByUpdatedAtDescIdDesc("1001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getLatest("1001"))
                .isInstanceOf(RecommendationNotFoundException.class);
    }

    private Item item() {
        Item item = new Item();
        item.setItemCode("1001");
        item.setItemName("배추");
        item.setItemType(Item.ItemType.DOMESTIC);
        return item;
    }

    private RiskScore riskScore() {
        RiskScore riskScore = new RiskScore();
        riskScore.setId(10L);
        riskScore.setItemCode("1001");
        riskScore.setScoreDate(LocalDate.of(2026, 7, 24));
        riskScore.setRiskGrade("ALERT");
        riskScore.setFinalScore(75);
        riskScore.setPriceIncreaseRate(new BigDecimal("12.50"));
        riskScore.setNormalYearComparisonRate(new BigDecimal("18.00"));
        riskScore.setPriceVolatilityRate(new BigDecimal("8.50"));
        riskScore.setWeatherReason("폭염 가능성");
        riskScore.setNewsReason("출하량 감소");
        return riskScore;
    }

    private ActionRecommendation recommendation(String content, RecommendationGenerationType generationType) {
        ActionRecommendation recommendation = new ActionRecommendation();
        recommendation.setId(1L);
        recommendation.setItemCode("1001");
        recommendation.setItemName("배추");
        recommendation.setRiskScoreId(10L);
        recommendation.setInputHash("hash");
        recommendation.setRiskGrade("ALERT");
        recommendation.setFinalScore(75);
        recommendation.setRecommendation(content);
        recommendation.setGenerationType(generationType);
        return recommendation;
    }
}
