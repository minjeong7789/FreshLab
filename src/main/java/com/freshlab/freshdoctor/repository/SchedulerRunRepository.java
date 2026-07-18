package com.freshlab.freshdoctor.repository;

import com.freshlab.freshdoctor.domain.SchedulerRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface SchedulerRunRepository extends JpaRepository<SchedulerRun, Long> {
    boolean existsByJobNameAndExecutionDate(String jobName, LocalDate executionDate);
}
