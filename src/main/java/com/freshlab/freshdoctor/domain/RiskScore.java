package com.freshlab.freshdoctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "risk_score")
@Getter
@Setter
@NoArgsConstructor
public class RiskScore {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore; // 0~100

    @Column(name = "grade")
    private String grade; // 안정/관심/주의/경계/심각

    @Column(name = "price_score")
    private Integer priceScore;

    @Column(name = "volatility_score")
    private Integer volatilityScore;

    @Column(name = "yearly_score")
    private Integer yearlyScore;

    @Column(name = "weather_score")
    private Integer weatherScore;

    @Column(name = "supply_score")
    private Integer supplyScore;

    @Column(name = "news_score")
    private Integer newsScore;

    @Column(name = "reasons", columnDefinition = "TEXT")
    private String reasons; // JSON 문자열로 저장
}