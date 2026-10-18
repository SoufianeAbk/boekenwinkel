package be.ehb.catalogusservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Via een Clock-bean kunnen testen met een vaste datum en tijd werken. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
