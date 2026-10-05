package com.engcoach.auth;

import com.engcoach.auth.dto.AuthResponse;
import com.engcoach.auth.dto.RegisterRequest;
import com.engcoach.common.EmailAlreadyRegisteredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Slice S1 only: register(). Login (S2) and token validation on protected
 * routes (S3) are intentionally not implemented here yet — see
 * implementation-plan.md.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            // feature-specification.md §1: "Failure: email already registered"
            throw new EmailAlreadyRegisteredException(request.email());
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), passwordHash);
        user = userRepository.save(user);

        JwtService.IssuedToken issuedToken = jwtService.issueAccessToken(user.getId());
        return AuthResponse.bearer(issuedToken.token(), issuedToken.expiresAt());
    }
}
