package com.freshlab.freshdoctor.service;

import com.freshlab.freshdoctor.domain.*;
import com.freshlab.freshdoctor.dto.*;
import com.freshlab.freshdoctor.repository.ItemRepository;
import com.freshlab.freshdoctor.repository.SchedulerRunRepository;
import com.freshlab.freshdoctor.repository.SchedulerStageLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataCollectionJobService {
    static final String JOB_NAME = "DAILY_DATA_COLLECTION";

    private final ItemRepository itemRepository;
    private final KamisPriceService kamisPriceService;
    private final WeatherService weatherService;
    private final NaverNewsService naverNewsService;
    private final TotalRiskService totalRiskService;
    private final RecommendationService recommendationService;
    private final DailySummaryAlertService dailySummaryAlertService;
    private final SchedulerRunRepository schedulerRunRepository;
    private final SchedulerStageLogRepository stageLogRepository;

    public SchedulerRun run(LocalDate executionDate) {
        LocalDate date = executionDate == null ? LocalDate.now() : executionDate;
        if (schedulerRunRepository.existsByJobNameAndExecutionDate(JOB_NAME, date)) {
            log.info("Scheduler skipped duplicate execution. job={}, date={}", JOB_NAME, date);
            return null;
        }
        SchedulerRun run = new SchedulerRun();
        run.setJobName(JOB_NAME);
        run.setExecutionDate(date);
        run.setStatus(SchedulerRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        try {
            run = schedulerRunRepository.saveAndFlush(run);
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Scheduler skipped concurrent duplicate. job={}, date={}", JOB_NAME, date);
            return null;
        }

        List<Item> items = itemRepository.findByActiveTrueOrderByItemNameAsc();
        run.setItemCount(items.size());
        int successfulItems = 0;
        int failedItems = 0;
        int totalSaved = 0;
        StringBuilder errors = new StringBuilder();
        log.info("Scheduler started. runId={}, items={}", run.getId(), items.size());

        for (Item item : items) {
            String itemCode = item.getItemCode();
            boolean itemSuccess = true;
            StageResult price = execute(run, itemCode, "KAMIS_PRICE", () -> {
                KamisPriceCollectResult result = kamisPriceService.collectDailyPrice(itemCode, date);
                return result(result.savedCount(), result.message());
            });
            StageResult weather = execute(run, itemCode, "WEATHER", () -> {
                WeatherCollectResult result = weatherService.collectForecast(itemCode, null, null, null, null, null);
                return result(result.savedCount(), result.message());
            });
            StageResult news = execute(run, itemCode, "NEWS", () -> {
                NewsCollectResult result = naverNewsService.collectNews(itemCode, null, 20);
                return result(result.savedCount(), result.message());
            });
            StageResult risk = execute(run, itemCode, "RISK_CALCULATION", () -> {
                totalRiskService.calculateAndSave(itemCode, date);
                return StageResult.success(1);
            });
            StageResult recommendation = execute(run, itemCode, "GPT_RECOMMENDATION", () -> {
                recommendationService.generate(itemCode, date);
                return StageResult.success(1);
            });
            for (StageResult stage : List.of(price, weather, news, risk, recommendation)) {
                totalSaved += stage.savedCount();
                if (!stage.success()) {
                    itemSuccess = false;
                    appendError(errors, itemCode + "/" + stage.error());
                }
            }
            if (itemSuccess) successfulItems++; else failedItems++;
        }

        StageResult summary = execute(run, "ALL", "DAILY_SUMMARY",
                () -> StageResult.success(dailySummaryAlertService.create(date)));
        totalSaved += summary.savedCount();
        if (!summary.success()) appendError(errors, "ALL/" + summary.error());

        run.setSuccessCount(successfulItems);
        run.setFailureCount(failedItems + (summary.success() ? 0 : 1));
        run.setSavedCount(totalSaved);
        run.setError(errors.isEmpty() ? null : errors.toString());
        run.setEndedAt(LocalDateTime.now());
        run.setStatus(run.getFailureCount() == 0 ? SchedulerRunStatus.SUCCESS
                : successfulItems == 0 ? SchedulerRunStatus.FAILED : SchedulerRunStatus.PARTIAL_FAILURE);
        SchedulerRun saved = schedulerRunRepository.save(run);
        log.info("Scheduler ended. runId={}, status={}, success={}, failure={}, saved={}",
                saved.getId(), saved.getStatus(), successfulItems, saved.getFailureCount(), totalSaved);
        return saved;
    }

    private StageResult execute(SchedulerRun run, String itemCode, String stage, Supplier<StageResult> action) {
        LocalDateTime startedAt = LocalDateTime.now();
        StageResult result;
        try {
            result = action.get();
        } catch (Exception exception) {
            result = StageResult.failure(exception.getMessage());
        }
        SchedulerStageLog stageLog = new SchedulerStageLog();
        stageLog.setSchedulerRun(run);
        stageLog.setItemCode(itemCode);
        stageLog.setStage(stage);
        stageLog.setSuccess(result.success());
        stageLog.setSavedCount(result.savedCount());
        stageLog.setError(result.error());
        stageLog.setStartedAt(startedAt);
        stageLog.setEndedAt(LocalDateTime.now());
        stageLogRepository.save(stageLog);
        if (result.success()) log.info("Scheduler stage succeeded. item={}, stage={}, saved={}", itemCode, stage, result.savedCount());
        else log.warn("Scheduler stage failed. item={}, stage={}, error={}", itemCode, stage, result.error());
        return result;
    }

    private StageResult result(int savedCount, String message) {
        if (message != null && message.toLowerCase().contains("failed")) return StageResult.failure(message);
        return StageResult.success(savedCount);
    }

    private void appendError(StringBuilder errors, String error) {
        if (!errors.isEmpty()) errors.append("; ");
        errors.append(error);
    }

    record StageResult(boolean success, int savedCount, String error) {
        static StageResult success(int count) { return new StageResult(true, count, null); }
        static StageResult failure(String error) { return new StageResult(false, 0, error == null ? "Unknown error" : error); }
    }
}
