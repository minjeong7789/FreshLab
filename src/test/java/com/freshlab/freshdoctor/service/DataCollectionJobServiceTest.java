package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.Item;
import com.freshlab.freshdoctor.domain.SchedulerRun;
import com.freshlab.freshdoctor.domain.SchedulerRunStatus;
import com.freshlab.freshdoctor.dto.KamisPriceCollectResult;
import com.freshlab.freshdoctor.dto.NewsCollectResult;
import com.freshlab.freshdoctor.dto.WeatherCollectResult;
import com.freshlab.freshdoctor.repository.ItemRepository;
import com.freshlab.freshdoctor.repository.SchedulerRunRepository;
import com.freshlab.freshdoctor.repository.SchedulerStageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataCollectionJobServiceTest {
    @Mock ItemRepository itemRepository;
    @Mock KamisPriceService kamisPriceService;
    @Mock WeatherService weatherService;
    @Mock NaverNewsService naverNewsService;
    @Mock TotalRiskService totalRiskService;
    @Mock RecommendationService recommendationService;
    @Mock DailySummaryAlertService dailySummaryAlertService;
    @Mock SchedulerRunRepository schedulerRunRepository;
    @Mock SchedulerStageLogRepository stageLogRepository;

    DataCollectionJobService service;

    @BeforeEach
    void setUp() {
        service = new DataCollectionJobService(itemRepository, kamisPriceService, weatherService,
                naverNewsService, totalRiskService, recommendationService, dailySummaryAlertService,
                schedulerRunRepository, stageLogRepository);
    }

    @Test
    void continuesWithOtherStagesAndItemsWhenOneCollectionFails() {
        LocalDate date = LocalDate.of(2026, 7, 28);
        when(schedulerRunRepository.existsByJobNameAndExecutionDate(DataCollectionJobService.JOB_NAME, date))
                .thenReturn(false);
        when(schedulerRunRepository.saveAndFlush(any(SchedulerRun.class))).thenAnswer(invocation -> {
            SchedulerRun run = invocation.getArgument(0);
            run.setId(1L);
            return run;
        });
        when(schedulerRunRepository.save(any(SchedulerRun.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByActiveTrueOrderByItemNameAsc()).thenReturn(List.of(item("1001"), item("1002")));
        when(kamisPriceService.collectDailyPrice("1001", date))
                .thenReturn(new KamisPriceCollectResult("1001", 0, 0, "KAMIS collection failed: timeout"));
        when(kamisPriceService.collectDailyPrice("1002", date))
                .thenReturn(new KamisPriceCollectResult("1002", 2, 2, "completed"));
        when(weatherService.collectForecast(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> new WeatherCollectResult(invocation.getArgument(0), "region", 3, 3, "completed"));
        when(naverNewsService.collectNews(any(), any(), eq(20)))
                .thenAnswer(invocation -> new NewsCollectResult(invocation.getArgument(0), "query", 1, 1, "completed"));
        when(dailySummaryAlertService.create(date)).thenReturn(1);

        SchedulerRun result = service.run(date);

        assertThat(result.getStatus()).isEqualTo(SchedulerRunStatus.PARTIAL_FAILURE);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getSavedCount()).isEqualTo(15);
        assertThat(result.getError()).contains("1001").contains("timeout");
        verify(totalRiskService).calculateAndSave("1001", date);
        verify(totalRiskService).calculateAndSave("1002", date);
        verify(recommendationService).generate("1001", date);
        verify(recommendationService).generate("1002", date);
        verify(stageLogRepository, org.mockito.Mockito.times(11)).save(any());
    }

    @Test
    void skipsAlreadyExecutedDate() {
        LocalDate date = LocalDate.of(2026, 7, 28);
        when(schedulerRunRepository.existsByJobNameAndExecutionDate(DataCollectionJobService.JOB_NAME, date))
                .thenReturn(true);

        assertThat(service.run(date)).isNull();
        verify(itemRepository, never()).findByActiveTrueOrderByItemNameAsc();
    }

    private Item item(String code) {
        Item item = new Item();
        item.setItemCode(code);
        item.setItemName(code);
        item.setActive(true);
        item.setItemType(Item.ItemType.DOMESTIC);
        return item;
    }
}
