package world.usan.usan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class KakaoClientConfig {

    @Bean
    public WebClient kakaoWebClient(@Value("${kakao.rest-api-key}") String restApiKey) {
        return WebClient.builder()
                .baseUrl("https://dapi.kakao.com")
                .defaultHeader("Authorization", "KakaoAK " + restApiKey)
                .build();
    }
}
