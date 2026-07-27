package com.freshlab.freshdoctor.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "fcm_delivery_log")
@Getter
@Setter
@NoArgsConstructor
public class FcmDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false)
    private Long alertId;

    @Column(name = "registration_id", nullable = false)
    private Long registrationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FcmDeliveryStatus status;

    @Column(name = "message_id", length = 255)
    private String messageId;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
