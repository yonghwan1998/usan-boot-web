package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "listing_link_visit", indexes = @Index(name = "idx_link_visit_history_time", columnList = "history_id,visited_at"))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ListingLinkVisit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "history_id", nullable = false)
    private Long historyId;
    @Column(name = "visited_at", nullable = false)
    private LocalDateTime visitedAt;
}
