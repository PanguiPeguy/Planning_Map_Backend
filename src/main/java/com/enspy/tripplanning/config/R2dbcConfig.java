package com.enspy.tripplanning.config;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;

@Slf4j
@Configuration
public class R2dbcConfig extends AbstractR2dbcConfiguration {

    @Value("${spring.r2dbc.url}")
    private String url;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Override
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        String finalUrl = url;

        // Detect mismatch between URL and properties if needed, but primarily trust the
        // resolved URL
        // Append missing options for NeonDB
        if (finalUrl.contains("neon.tech") && !finalUrl.contains("options=endpoint")) {
            log.warn("Detected NeonDB URL without SNI options. Appending fix...");
            String separator = finalUrl.contains("?") ? "&" : "?";
            finalUrl += separator + "options=endpoint%3Dep-icy-lab-a4oyly5v-pooler";
        }

        log.info("Initializing R2DBC ConnectionFactory with URL: {}", finalUrl.replaceAll("://.*@", "://***@"));

        return ConnectionFactories.get(finalUrl);
    }
}
