package com.freshlab.freshdoctor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduler_stage_log")
@Getter
@Setter
@NoArgsConstructor
public class SchedulerStageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scheduler_run_id", nullable = false)
    private SchedulerRun schedulerRun;
    @Column(name = "item_code", nullable = false, length = 30)
    private String itemCode;
    @Column(name = "stage", nullable = false, length = 50)
    private String stage;
    @Column(name = "success", nullable = false)
    private boolean success;
    @Column(name = "saved_count", nullable = false)
    private int savedCount;
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;
    @Column(name = "ended_at", nullable = false)
    private LocalDateTime endedAt;
    @Column(columnDefinition = "TEXT")
    private String error;
}
