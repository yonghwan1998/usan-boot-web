package com.usanmap.usan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_social_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_social_provider_user", columnNames = {"provider", "providerUserId"})
)
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class UserSocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;


    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false, length = 128)
    private String providerUserId;

    @Column(name = "email")
    private String email;

    @Column(name = "nickname")
    private String nickname;

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @JdbcTypeCode(Types.LONGVARCHAR)
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Provider { GOOGLE, KAKAO, NAVER }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
