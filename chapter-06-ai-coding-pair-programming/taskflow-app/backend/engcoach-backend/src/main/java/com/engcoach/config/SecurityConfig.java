package com.engcoach.config;

import com.engcoach.auth.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * S1 + S2 + S3 scope.
 *
 * - /auth/register and /auth/login are public (feature-specification.md
 *   §1, §2: both are "no auth required" actions).
 * - Everything else default-denies (authenticated()) — enforced for real as
 *   of S3 by JwtAuthenticationFilter, registered below ahead of Spring
 *   Security's own UsernamePasswordAuthenticationFilter so a valid Bearer
 *   token is recognized before the chain's authorization check runs.
 * - Stateless (no HTTP session) because auth is JWT-based (ADR-003).
 * - RestAuthenticationEntryPoint overrides Spring Security's default
 *   (403 Forbidden on missing/invalid auth) with the 401 RFC 9457 body the
 *   API contract requires. 403 is reserved for "valid JWT, wrong owner" —
 *   that per-resource ownership check is enforced in each endpoint's own
 *   service/controller starting with the slices that add owned resources
 *   (e.g. /attempts/*, S6a+), not here; this filter only answers "is there
 *   a valid token at all."
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
        return new JwtAuthenticationFilter(jwtService);
    }

    /**
     * Spring Boot auto-registers any {@code Filter} bean as a global servlet
     * filter on top of whatever Spring Security's own chain does with it via
     * {@code addFilterBefore} below — which would run this filter twice per
     * request. Disabling the auto-registration here means
     * {@code addFilterBefore} in {@link #filterChain} is the only place this
     * filter is wired in.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableAutoRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                            JwtAuthenticationFilter jwtAuthenticationFilter,
                                            ObjectMapper objectMapper) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
