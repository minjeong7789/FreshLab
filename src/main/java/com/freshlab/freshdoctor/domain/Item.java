package com.freshlab.freshdoctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    @Id
    @Column(name = "item_code", length = 10)
    private String itemCode; // KAMIS 품목 코드 (배추: 1001)

    @Column(name = "item_name", nullable = false)
    private String itemName; // "배추"

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType; // DOMESTIC, IMPORT, MIXED

    @Column(name = "origin_region")
    private String originRegion; // "강원 평창"

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_sensitivity")
    private WeatherSensitivity weatherSensitivity; // HIGH, MED, LOW

    public enum ItemType {
        DOMESTIC, IMPORT, MIXED
    }

    public enum WeatherSensitivity {
        HIGH, MED, LOW
    }
}