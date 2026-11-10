package fr.mtpilates.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PROTECTED_PATHS = {"/app/**", "/onboarding"};

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        // Comme l'original, seules /app et /onboarding exigent une session : toute autre URL inconnue
        // doit aboutir à la page 404, pas à un refus. Le form login arrive au module M1.
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PROTECTED_PATHS).authenticated()
                        .anyRequest().permitAll())
                .build();
    }
}
