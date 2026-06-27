package com.ctbe.eventflow.config;

import com.resend.Resend;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ResendConfig {

    @Value("${app.resend.api-key:}")
    private String apiKey;

    @Bean
    public Resend resend() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not set — emails will not be sent");
            // Return a no-op instance with a dummy key so the context
            // starts cleanly in tests and local dev without crashing
            return new Resend("re_dummy_no_op_key");
        }
        return new Resend(apiKey);
    }
}