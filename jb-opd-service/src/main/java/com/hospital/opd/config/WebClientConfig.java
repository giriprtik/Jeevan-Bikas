package com.hospital.opd.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /** Lab calls forward the caller's own Bearer token, so the lab service enforces the doctor's role. */
    @Bean
    public WebClient labWebClient(@Value("${lab.service.url}") String labServiceUrl) {
        return WebClient.builder().baseUrl(labServiceUrl).build();
    }

    /** Internal auth lookups authenticate with the shared cluster secret (never exposed via the gateway). */
    @Bean
    public WebClient internalAuthWebClient(@Value("${auth.service.url}") String authServiceUrl,
                                           @Value("${app.internal-secret}") String internalSecret) {
        return WebClient.builder()
                .baseUrl(authServiceUrl)
                .defaultHeader("X-Internal-Secret", internalSecret)
                .build();
    }
}
