package com.usanmap.usan.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login/**",
                                "/join/**",
                                "/search",
                                "/map",
                                "/map/api/**",
                                "/broker/**",
                                "/l/**",
                                "/b/**",
                                "/download/**",
                                "/policy/**",
                                "/geo/**",
                                "/credits/hecto/noti",
                                "/.well-known/**",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/fonts/**",
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers("/actuator/prometheus")
                            .access(new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1') or hasIpAddress('::1')"))
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/login");
                        }))
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("loginId")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/")
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .defaultSuccessUrl("/", false)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                );

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

}
