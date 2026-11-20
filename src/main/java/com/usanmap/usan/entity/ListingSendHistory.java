package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import com.usanmap.usan.common.jpa.UuidToBytesConverter;

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

    private Long batchId;

    @Convert(converter = UuidToBytesConverter.class)
    @Column(columnDefinition = "BINARY(16)")
    private UUID brokerCode;
    @Column(length = 255) private String brokerOfficeName;
    @Column(length = 512) private String brokerAddress;
    @Column(nullable = false, length = 20) private String deliveryStatus;
    @Column(length = 100) private String providerMessageId;
    @Column(length = 100) private String resultCode;
    private LocalDateTime deliveredAt;
    private LocalDateTime failedAt;
    private Integer refundedCredit;
    private LocalDateTime refundedAt;
    private Long refundCreditLedgerId;

    public void markRequest(String status, String code) {
        deliveryStatus = status;
        resultCode = code;
        if ("FAILED".equals(status)) failedAt = LocalDateTime.now();
    }

    public String getDeliveryLabel() {
        if (deliveryStatus == null) return "결과 확인 불가";
        return switch (deliveryStatus) {
            case "PENDING" -> "발송 준비";
            case "ACCEPTED" -> "접수 완료";
            case "DELIVERED" -> "수신 성공";
            case "FAILED" -> "발송 실패";
            default -> "결과 확인 불가";
        };
    }

    public String getSendStatusLabel() {
        if (refundedCredit != null && refundedCredit > 0) return "환불 처리";
        if (deliveryStatus == null) return "결과 확인 불가";
        return switch (deliveryStatus) {
            case "PENDING", "ACCEPTED" -> "전송 중";
            case "DELIVERED" -> "전송 완료";
            case "FAILED" -> "전송 실패";
            default -> "결과 확인 불가";
        };
    }

    public String getSendStatusClass() {
        if (refundedCredit != null && refundedCredit > 0) return "is-refunded";
        return "DELIVERED".equals(deliveryStatus) ? "is-delivered" : "";
    }

    public String getBrokerShortAddress() {
        if (brokerAddress == null || brokerAddress.isBlank()) return "주소 정보 없음";
        String[] parts = brokerAddress.trim().split("\\s+");
        return String.join(" ", java.util.Arrays.copyOf(parts, Math.min(3, parts.length)));
    }

    @PrePersist
    void prePersist() {
        if (deliveryStatus == null) deliveryStatus = "UNKNOWN";
        if (sentAt == null) sentAt = LocalDateTime.now();
    }
}
