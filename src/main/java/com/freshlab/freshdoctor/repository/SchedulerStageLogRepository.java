package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.SchedulerStageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerStageLogRepository extends JpaRepository<SchedulerStageLog, Long> {
}
