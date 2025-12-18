package world.usan.usan.config;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import world.usan.usan.dto.OAuthUserInfo;
import world.usan.usan.service.OAuthUserInfoMapper;
import world.usan.usan.service.SocialLoginService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final SocialLoginService socialLoginService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuthUserInfo info = OAuthUserInfoMapper.from(registrationId, attributes);

        String accessToken = userRequest.getAccessToken().getTokenValue();
        String refreshToken = userRequest.getAdditionalParameters().get("refresh_token") == null
                ? null
                : String.valueOf(userRequest.getAdditionalParameters().get("refresh_token"));

        LocalDateTime expiresAt = userRequest.getAccessToken().getExpiresAt() == null
                ? null
                : LocalDateTime.ofInstant(userRequest.getAccessToken().getExpiresAt(), ZoneId.systemDefault());

        //DB 저장/연결
        var snap = socialLoginService.upsertUserAndLinkSocial(info, accessToken, refreshToken, expiresAt);

        //세션 principal로 쓰기 위해 attributes에 우리 userId 주입
        //nameAttributeKey는 provider 별로 다를 수 있어서, 그냥 attributes map에 추가
        //(불변 Map인 경우 대비: 새 Map 만들어 반환)
        Map<String, Object> newAttrs = new java.util.HashMap<>(attributes);
        newAttrs.put("app_user_id", snap.userId());
        newAttrs.put("app_user_email", snap.email());
        newAttrs.put("app_user_nickname", snap.nickname());

        //DefaultOAuth2User 생성에 필요한 nameAttributeKey
        String nameKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                newAttrs,
                nameKey
        );
    }
}
