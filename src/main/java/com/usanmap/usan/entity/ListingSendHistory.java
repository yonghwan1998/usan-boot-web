package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "listing_send_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListingSendHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "listing_id", nullable = false)
    private Long listingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "broker_phone", length = 30, nullable = false)
    private String brokerPhone;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    void prePersist() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }
}
