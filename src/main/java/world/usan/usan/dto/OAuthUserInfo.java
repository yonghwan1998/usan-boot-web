package world.usan.usan.dto;

import lombok.Builder;

@Builder
public record OAuthUserInfo(
        String provider,
        String providerUserId,
        String email,
        String nickname
) {}
