package com.freshlab.freshdoctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "action_recommendation",
        uniqueConstraints = @UniqueConstraint(name = "uk_recommendation_input_hash", columnNames = "input_hash")
)
@Getter
@Setter
@NoArgsConstructor
public class ActionRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false, length = 10)
    private String itemCode;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "risk_score_id", nullable = false)
    private Long riskScoreId;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "risk_grade")
    private String riskGrade;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "price_increase_rate", precision = 8, scale = 2)
    private BigDecimal priceIncreaseRate;

    @Column(name = "weather_issue", columnDefinition = "TEXT")
    private String weatherIssue;

    @Column(name = "news_issue", columnDefinition = "TEXT")
    private String newsIssue;

    @Column(name = "recommendation", nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_type", nullable = false, length = 20)
    private RecommendationGenerationType generationType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
