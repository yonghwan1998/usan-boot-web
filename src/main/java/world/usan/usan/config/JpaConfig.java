package world.usan.usan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "world.usan.usan.repository")
public class JpaConfig {
}
