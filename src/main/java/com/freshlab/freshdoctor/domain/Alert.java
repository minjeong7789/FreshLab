package com.freshlab.freshdoctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "alert",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_alert_user_item_type_date",
                columnNames = {"user_id", "item_code", "alert_type", "risk_score_date"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "item_code")
    private String itemCode;

    @Column(name = "item_name")
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 50)
    private AlertType alertType;

    @Column(name = "previous_score")
    private Integer previousScore;

    @Column(name = "previous_grade", length = 20)
    private String previousGrade;

    @Column(name = "current_score")
    private Integer currentScore;

    @Column(name = "current_grade", length = 20)
    private String currentGrade;

    @Column(name = "title")
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "risk_score_date")
    private LocalDate riskScoreDate;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @Column(name = "is_read")
    private Boolean isRead = false;

    // 기존 조회 코드와 DB 컬럼 호환용 필드
    @Column(name = "score")
    private Integer score;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (occurredAt == null) {
            occurredAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (score == null) {
            score = currentScore;
        }
        if (message == null) {
            message = description;
        }
    }
}
