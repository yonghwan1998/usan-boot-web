package com.usanmap.usan.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomOAuth2User implements OAuth2User {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(Long userId, String email, String nickname, Map<String, Object> attributes) {
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.attributes = attributes;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
