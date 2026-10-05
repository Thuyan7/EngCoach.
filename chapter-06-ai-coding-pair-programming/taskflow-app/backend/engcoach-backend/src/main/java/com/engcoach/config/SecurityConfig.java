package com.engcoach.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * S1 scope only.
 *
 * - /auth/register is public (feature-specification.md §1: "public action,
 *   no auth required").
 * - Everything else default-denies (authenticated()) even though no other
 *   endpoint exists yet in this slice — this is deliberate so S5a/S6a etc.
 *   don't need to remember to add authorization later; they inherit
 *   default-deny by just being added to the app.
 * - Stateless (no HTTP session) because auth is JWT-based (ADR-003).
 * - The actual JwtAuthenticationFilter that validates a Bearer token on
 *   protected routes is Slice S3 — not built yet. Until S3 exists, any
 *   non-/auth/** endpoint added by a future slice will correctly return 401
 *   for every request (including valid ones) because nothing yet populates
 *   the SecurityContext — that is expected and should not be "fixed" inside
 *   a later slice without first doing S3.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
