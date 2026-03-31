package com.usanmap.usan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.usanmap.usan.repository")
public class JpaConfig {
}
