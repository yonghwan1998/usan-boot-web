package com.usanmap.usan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NcpSensConfig {

    @Bean
    public WebClient sensWebClient() {
        return WebClient.builder()
                .baseUrl("https://sens.apigw.ntruss.com")
                .build();
    }
}
