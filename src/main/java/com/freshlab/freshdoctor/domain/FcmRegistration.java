package com.freshlab.freshdoctor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fcm_registration",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fcm_registration_key",
                columnNames = "registration_key"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class FcmRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "registration_key", nullable = false, length = 512)
    private String registrationKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FcmPlatform platform;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "deactivated_at")
    private LocalDateTime deactivatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (registeredAt == null) {
            registeredAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
        if (active == null) {
            active = true;
        }
    }

    public void activateFor(User user, FcmPlatform platform, String deviceName) {
        this.user = user;
        this.platform = platform;
        this.deviceName = deviceName;
        this.active = true;
        this.lastSeenAt = LocalDateTime.now();
        this.deactivatedAt = null;
    }

    public void deactivate() {
        this.active = false;
        this.deactivatedAt = LocalDateTime.now();
    }
}
