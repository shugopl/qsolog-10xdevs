package com.pl.shugo.gsolog.infrastructure.config;

import com.pl.shugo.gsolog.infrastructure.security.JwtAuthenticationConverter;
import com.pl.shugo.gsolog.infrastructure.security.JwtAuthenticationManager;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.http.HttpStatus;

/**
 * Security configuration with JWT authentication.
 *
 * Anonymous access:
 * - /api/v1/ping
 * - /api/v1/auth/register
 * - /api/v1/auth/login
 * - /actuator/**
 * - /swagger-ui.html, /api-docs, /webjars/**
 * - /api/v1/lookup/**
 * - /api/v1/ai/qso-description
 *
 * ADMIN-only access:
 * - /api/v1/admin/**
 *
 * Authenticated access (JWT required):
 * - /api/v1/qso/**
 * - /api/v1/stats/**
 * - /api/v1/export/**
 * - /api/v1/ai/** (except qso-description)
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtAuthenticationManager jwtAuthenticationManager;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityConfig(JwtAuthenticationManager jwtAuthenticationManager,
                          JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtAuthenticationManager = jwtAuthenticationManager;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        AuthenticationWebFilter authenticationWebFilter = new AuthenticationWebFilter(jwtAuthenticationManager);
        authenticationWebFilter.setServerAuthenticationConverter(jwtAuthenticationConverter);

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
                )
                .authorizeExchange(exchanges -> exchanges
                        // Always allow CORS preflight
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Allow anonymous access
                        .pathMatchers("/api/v1/ping").permitAll()
                        .pathMatchers("/api/v1/auth/register").permitAll()
                        .pathMatchers("/api/v1/auth/login").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**").permitAll()
                        .pathMatchers("/api-docs", "/api-docs/**").permitAll()
                        .pathMatchers("/v3/api-docs/**").permitAll()
                        .pathMatchers("/webjars/**").permitAll()
                        .pathMatchers("/api/v1/lookup/**").permitAll()
                        .pathMatchers("/api/v1/ai/qso-description").permitAll()

                        // Require ADMIN role for admin endpoints
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // Require authentication for protected endpoints
                        .pathMatchers("/api/v1/qso/**").authenticated()
                        .pathMatchers("/api/v1/stats/**").authenticated()
                        .pathMatchers("/api/v1/export/**").authenticated()
                        .pathMatchers("/api/v1/ai/**").authenticated()

                        // Default: require authentication
                        .anyExchange().authenticated()
                )
                .addFilterAt(authenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
