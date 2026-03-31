package com.usanmap.usan.service;

import com.usanmap.usan.dto.OAuthUserInfo;

import java.util.Map;

public class OAuthUserInfoMapper {

    @SuppressWarnings("unchecked")
    public static OAuthUserInfo from(String registrationId, Map<String, Object> attributes) {

        String rid = registrationId.toLowerCase();

        if (rid.equals("google")) {
            return OAuthUserInfo.builder()
                    .provider("GOOGLE")
                    .providerUserId(String.valueOf(attributes.get("sub")))
                    .email((String) attributes.get("email"))
                    .nickname((String) attributes.getOrDefault("name", attributes.get("email")))
                    .build();
        }

        if (rid.equals("kakao")) {
            Object id = attributes.get("id");
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            String email = kakaoAccount == null ? null : (String) kakaoAccount.get("email");

            String nickname = null;
            if (kakaoAccount != null) {
                Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                if (profile != null) {
                    nickname = (String) profile.get("nickname");
                }
            }

            return OAuthUserInfo.builder()
                    .provider("KAKAO")
                    .providerUserId(String.valueOf(id))
                    .email(email)
                    .nickname(nickname != null ? nickname : email)
                    .build();
        }

        if (rid.equals("naver")) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            String id = response == null ? null : (String) response.get("id");
            String email = response == null ? null : (String) response.get("email");
            String name = response == null ? null : (String) response.get("name");
            String nickname = response == null ? null : (String) response.get("nickname");

            return OAuthUserInfo.builder()
                    .provider("NAVER")
                    .providerUserId(id)
                    .email(email)
                    .nickname(nickname != null ? nickname : name != null ? name : email)
                    .build();
        }

        throw new IllegalArgumentException("Unsupported registrationId: " + registrationId);
    }
}
