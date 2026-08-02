package com.freshlab.freshdoctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "news_article",
        uniqueConstraints = @UniqueConstraint(name = "uk_news_item_link", columnNames = {"item_code", "link_hash"})
)
@Getter
@Setter
@NoArgsConstructor
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "query_text", nullable = false)
    private String queryText;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "link", nullable = false, length = 1000)
    private String link;

    @Column(name = "link_hash", nullable = false, length = 64)
    private String linkHash;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "matched_keywords")
    private String matchedKeywords;

    @Column(name = "news_risk_type")
    private String newsRiskType;

    @Column(name = "risk_reason")
    private String riskReason;

    @Column(name = "representative_risk", nullable = false)
    private Boolean representativeRisk = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "news_risk_score")
    private Integer newsRiskScore;

    @Column(name = "source", nullable = false)
    private String source = "NAVER";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
