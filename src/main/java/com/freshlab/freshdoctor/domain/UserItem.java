package com.freshlab.freshdoctor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
