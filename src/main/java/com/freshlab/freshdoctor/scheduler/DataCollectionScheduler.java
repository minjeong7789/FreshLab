package com.freshlab.freshdoctor.scheduler;

import com.freshlab.freshdoctor.service.DataCollectionJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "scheduler.data-collection", name = "enabled", havingValue = "true")
public class DataCollectionScheduler {
    private final DataCollectionJobService jobService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${scheduler.data-collection.zone:Asia/Seoul}")
    private String zone;

    @Scheduled(cron = "${scheduler.data-collection.cron:0 0 8 * * *}", zone = "${scheduler.data-collection.zone:Asia/Seoul}")
    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Scheduler invocation skipped because the previous execution is still running.");
            return;
        }
        try {
            jobService.run(LocalDate.now(ZoneId.of(zone)));
        } finally {
            running.set(false);
        }
    }
}
