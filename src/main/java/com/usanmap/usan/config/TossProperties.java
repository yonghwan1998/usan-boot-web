package com.usanmap.usan.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "toss.payments")
public class TossProperties {
    private String testClientKey;
    private String testSecretKey;
    private String liveClientKey;
    private String liveSecretKey;

    private static final String TEST_ACCOUNT = "test@naver.com";

    public boolean isTestAccount(String email) {
        return TEST_ACCOUNT.equals(email);
    }

    public String resolveClientKey(String email) {
        return TEST_ACCOUNT.equals(email) ? testClientKey : liveClientKey;
    }

    public String resolveSecretKey(String email) {
        return TEST_ACCOUNT.equals(email) ? testSecretKey : liveSecretKey;
    }
}
