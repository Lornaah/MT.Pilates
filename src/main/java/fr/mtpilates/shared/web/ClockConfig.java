package fr.mtpilates.shared.web;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** Fuseau du studio : les dates « du jour » (séries, semaines) se calculent en heure de Paris. */
    public static final ZoneId STUDIO_ZONE = ZoneId.of("Europe/Paris");

    @Bean
    public Clock clock() {
        return Clock.system(STUDIO_ZONE);
    }
}
