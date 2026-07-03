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
@Table(name = "price_history")
@Getter
@Setter
@NoArgsConstructor
public class PriceHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "price", nullable = false)
    private Integer price; // 원/kg

    @Column(name = "market_type")
    private String marketType; // "도매" or "소매"
}