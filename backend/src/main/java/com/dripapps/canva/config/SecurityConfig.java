package com.dripapps.canva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the application.
 * 
 * For this MVP, we disable most Spring Security features since:
 * - We handle authentication ourselves via Canva OAuth
 * - We don't need CSRF for a REST API
 * - All endpoints should be publicly accessible (OAuth handles auth)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Allow all requests - our controllers handle auth check
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())

                // Disable form login (we use Canva OAuth)
                .formLogin(AbstractHttpConfigurer::disable)

                // Disable HTTP Basic auth
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
