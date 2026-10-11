package com.engcoach.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.net.URI;

/**
 * Slice S3. Spring Security's own default for a request with no/invalid
 * authentication is to return 403 Forbidden (there's no login page to
 * redirect to, since formLogin/httpBasic are disabled). The API contract
 * (openapi.yaml, software-architecture.md §5) requires 401 for "no valid
 * JWT" and reserves 403 for "valid JWT, wrong owner" (authorization,
 * enforced later per-endpoint, not by this class) — so this entry point
 * overrides the default to return a 401 RFC 9457 problem body, the same
 * shape GlobalExceptionHandler uses for every other error.
 */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String PROBLEM_BASE = "https://engcoach.app/problems/";

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                          HttpServletResponse response,
                          AuthenticationException authException) throws IOException, ServletException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "A valid access token is required for this request.");
        problem.setType(URI.create(PROBLEM_BASE + "unauthenticated"));
        problem.setTitle("Authentication required");
        problem.setInstance(URI.create(request.getRequestURI()));

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
