package com.usanmap.usan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.usanmap.usan.dto.LoginUserSnapshot;
import com.usanmap.usan.dto.OAuthUserInfo;
import com.usanmap.usan.entity.User;
import com.usanmap.usan.entity.UserSocialAccount;
import com.usanmap.usan.repository.UserRepository;
import com.usanmap.usan.repository.UserSocialAccountRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;

    @Transactional
    public LoginUserSnapshot upsertUserAndLinkSocial(OAuthUserInfo info,
                                                     String accessToken,
                                                     String refreshToken,
                                                     LocalDateTime expiresAt) {

        UserSocialAccount.Provider provider = UserSocialAccount.Provider.valueOf(info.provider());

        //1. (provider, providerUserId)로 소셜 계정 존재 확인
        var socialOpt = userSocialAccountRepository.findByProviderAndProviderUserId(provider, info.providerUserId());

        if (socialOpt.isPresent()) {
            var social =  socialOpt.get();

            social.setEmail(info.email());
            social.setNickname(info.nickname());
            social.setAccessToken(accessToken);
            social.setRefreshToken(refreshToken);
            social.setTokenExpiresAt(expiresAt);

            User u = social.getUser();
            return new LoginUserSnapshot(u.getId(), u.getEmail(), u.getNickname());
        }

        //2. 소셜 계정이 처음이면 유저를 찾거나 생성
        //email이 null일 수도 있어서, null이면 새 유저 생성(중복 방지 어려움)
        User user;
        if (info.email() != null && !info.email().isEmpty()) {
            user = userRepository.findByEmail(info.email())
                    .orElseGet(() -> userRepository.save(User.builder()
                            .email(info.email())
                            .nickname(info.nickname())
                            .status(User.Status.ACTIVE)
                            .build()));
        } else {
            user = userRepository.save(User.builder()
                    .email(null)
                    .nickname(info.nickname())
                    .status(User.Status.ACTIVE)
                    .build());
        }

        // 3) 소셜 계정 생성 + 유저 연결
        userSocialAccountRepository.save(UserSocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(info.providerUserId())
                .email(info.email())
                .nickname(info.nickname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenExpiresAt(expiresAt)
                .build());

        return new LoginUserSnapshot(user.getId(), user.getEmail(), user.getNickname());
    }
}
