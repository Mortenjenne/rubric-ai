package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

// UserDetailsServiceAutoConfiguration is excluded because app.security.SecurityConfig
// authenticates against EducatorRepository directly rather than through Spring Security's
// UserDetailsService abstraction; left enabled, it logs a generated in-memory password on every
// startup for an account nothing ever uses.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class RubricAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(RubricAiApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
