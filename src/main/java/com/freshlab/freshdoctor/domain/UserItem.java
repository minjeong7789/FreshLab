package com.freshlab.freshdoctor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "user_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_item_user_item_code",
                columnNames = {"user_id", "item_code"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class UserItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_code", nullable = false)
    private Item item;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled = true;

    @Column(
            name = "price_volatility_threshold",
            nullable = false,
            precision = 8,
            scale = 2,
            columnDefinition = "decimal(8,2) default 10.00"
    )
    private BigDecimal priceVolatilityThreshold = new BigDecimal("10.00");

    @Column(
            name = "price_increase_threshold",
            nullable = false,
            precision = 8,
            scale = 2,
            columnDefinition = "decimal(8,2) default 10.00"
    )
    private BigDecimal priceIncreaseThreshold = new BigDecimal("10.00");
}
