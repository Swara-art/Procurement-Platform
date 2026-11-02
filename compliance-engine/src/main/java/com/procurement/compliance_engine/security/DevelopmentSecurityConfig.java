package com.procurement.compliance_engine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev")
public class DevelopmentSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/v1/organizations",
                        "/api/v1/organizations/**",
                        "/api/v1/tenders",
                        "/api/v1/tenders/**",
                        "/api/v1/bids",
                        "/api/v1/bids/**",
                        "/api/v1/documents/**"
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/api/v1/organizations",
                                "/api/v1/organizations/**",
                                "/api/v1/tenders",
                                "/api/v1/tenders/**",
                                "/api/v1/bids",
                                "/api/v1/bids/**",
                                "/api/v1/documents/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .build();
    }
}
