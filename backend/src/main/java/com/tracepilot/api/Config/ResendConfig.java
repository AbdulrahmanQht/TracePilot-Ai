package com.tracepilot.api.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.resend.Resend;

@Configuration
public class ResendConfig {

    @Bean
    Resend resend(@Value("${resend.api-key}") String apiKey) {
        return new Resend(apiKey);
    }
}