package com.engcoach.config;

import com.engcoach.auth.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Slice S3: validates the {@code Authorization: Bearer <token>} header on
 * every request and, when the token is valid, authenticates it — making
 * every non-public route behind {@code SecurityConfig}'s
 * {@code anyRequest().authenticated()} actually reachable with a real JWT
 * instead of unconditionally 401ing (see SecurityConfig's S1/S2 Javadoc).
 *
 * Deliberately does nothing (doesn't throw, doesn't short-circuit the
 * chain) when the header is missing or the token is invalid: it just
 * leaves the SecurityContext empty and lets the request continue. Spring
 * Security's own authorization check further down the chain is what then
 * rejects it — via {@link RestAuthenticationEntryPoint}, which is what
 * turns that rejection into the 401 RFC 9457 body the API contract
 * promises (Spring Security's default for an unauthenticated request is
 * 403, not 401 — that default would be wrong here).
 *
 * The authenticated principal is the user's id (a {@code Long}), per
 * JwtService's "subject = user id only" contract — never the email. No
 * authorities/roles are granted because EngCoach's MVP has no role-based
 * authorization, only per-user data ownership (enforced by individual
 * controllers/services starting in later slices, e.g. the attempts
 * endpoints), not by Spring Security authorities.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        extractBearerToken(request)
                .flatMap(jwtService::validateAndGetUserId)
                .ifPresent(userId -> {
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()));
    }
}
