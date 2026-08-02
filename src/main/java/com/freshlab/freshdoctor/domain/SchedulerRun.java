package com.freshlab.freshdoctor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduler_run", uniqueConstraints = @UniqueConstraint(
        name = "uk_scheduler_run_job_date", columnNames = {"job_name", "execution_date"}))
@Getter
@Setter
@NoArgsConstructor
public class SchedulerRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;
    @Column(name = "execution_date", nullable = false)
    private LocalDate executionDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SchedulerRunStatus status;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    @Column(name = "item_count", nullable = false)
    private int itemCount;
    @Column(name = "success_count", nullable = false)
    private int successCount;
    @Column(name = "failure_count", nullable = false)
    private int failureCount;
    @Column(name = "saved_count", nullable = false)
    private int savedCount;
    @Column(columnDefinition = "TEXT")
    private String error;
}
