package com.freshlab.freshdoctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "risk_score",
        uniqueConstraints = @UniqueConstraint(name = "uk_risk_item_date", columnNames = {"item_code", "score_date"})
)
@Getter
@Setter
@NoArgsConstructor
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "grade")
    private String grade;

    @Column(name = "price_increase_rate", precision = 8, scale = 2)
    private BigDecimal priceIncreaseRate;

    @Column(name = "price_increase_score")
    private Integer priceIncreaseScore;

    @Column(name = "price_score")
    private Integer priceScore;

    @Column(name = "normal_year_comparison_rate", precision = 8, scale = 2)
    private BigDecimal normalYearComparisonRate;

    @Column(name = "normal_year_score")
    private Integer normalYearScore;

    @Column(name = "price_volatility_rate", precision = 8, scale = 2)
    private BigDecimal priceVolatilityRate;

    @Column(name = "volatility_score")
    private Integer volatilityScore;

    @Column(name = "yearly_score")
    private Integer yearlyScore;

    @Column(name = "weather_risk_type")
    private String weatherRiskType;

    @Column(name = "weather_score")
    private Integer weatherScore;

    @Column(name = "weather_reason", columnDefinition = "TEXT")
    private String weatherReason;

    @Column(name = "weather_base_date")
    private LocalDate weatherBaseDate;

    @Column(name = "weather_base_time", length = 4)
    private String weatherBaseTime;

    @Column(name = "supply_score")
    private Integer supplyScore;

    @Column(name = "news_risk_type")
    private String newsRiskType;

    @Column(name = "news_score")
    private Integer newsScore;

    @Column(name = "news_reason", columnDefinition = "TEXT")
    private String newsReason;

    @Column(name = "representative_news_article_id")
    private Long representativeNewsArticleId;

    @Column(name = "raw_score")
    private Integer rawScore;

    @Column(name = "final_score")
    private Integer finalScore;

    @Column(name = "risk_grade")
    private String riskGrade;

    @Column(name = "unavailable_items", columnDefinition = "TEXT")
    private String unavailableItems;

    @Column(name = "unavailable_reasons", columnDefinition = "TEXT")
    private String unavailableReasons;

    @Column(name = "recommendation", columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "reasons", columnDefinition = "TEXT")
    private String reasons;

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
