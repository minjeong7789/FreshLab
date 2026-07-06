package com.freshlab.freshdoctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
    private String itemCode;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(name = "category")
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType;

    @Column(name = "origin_region")
    private String originRegion;

    @Column(name = "weather_region")
    private String weatherRegion;

    @Column(name = "weather_nx")
    private Integer weatherNx;

    @Column(name = "weather_ny")
    private Integer weatherNy;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_sensitivity")
    private WeatherSensitivity weatherSensitivity;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public enum ItemType {
        DOMESTIC, IMPORT, MIXED
    }

    public enum WeatherSensitivity {
        HIGH, MED, LOW
    }
}
