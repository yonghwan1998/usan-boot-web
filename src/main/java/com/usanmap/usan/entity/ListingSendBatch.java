package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "listing_send_batch")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListingSendBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long listingId;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 255) private String listingTitle;
    @Column(nullable = false, length = 512) private String listingAddress;
    private LocalDateTime listingCreatedAt;
    private LocalDateTime listingUpdatedAt;
    @Column(nullable = false, length = 10) private String messageType;
    @Column(nullable = false) private Integer recipientCount;
    @Column(nullable = false, length = 20) private String requestStatus;
    @Column(length = 100) private String providerRequestId;
    private Long creditLedgerId;
    private Integer creditBefore;
    private Integer creditUsed;
    private Integer creditAfter;
    @Column(nullable = false) private LocalDateTime requestedAt;
    private LocalDateTime acceptedAt;
    @Column(name = "selection_radius_m") private Integer selectionRadiusM;
    @Column(length = 500) private String selectionListingTypes;
    @Column(nullable = false) private LocalDateTime updatedAt;

    public void markRequest(String status, String requestId) {
        requestStatus = status;
        providerRequestId = requestId;
        updatedAt = LocalDateTime.now();
        if ("ACCEPTED".equals(status)) acceptedAt = updatedAt;
    }
}
