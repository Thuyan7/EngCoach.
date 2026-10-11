package com.engcoach.auth;

import com.engcoach.auth.dto.AuthResponse;
import com.engcoach.auth.dto.ForgotPasswordRequest;
import com.engcoach.auth.dto.LoginRequest;
import com.engcoach.auth.dto.RegisterRequest;
import com.engcoach.auth.dto.ResetPasswordRequest;
import com.engcoach.common.EmailAlreadyRegisteredException;
import com.engcoach.common.InvalidCredentialsException;
import com.engcoach.common.InvalidOrExpiredResetTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * S1: register(). S2 adds login(). Token validation on protected routes
 * (S3) is intentionally not implemented here yet — see
 * implementation-plan.md. S4 adds forgotPassword()/resetPassword().
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // feature-specification.md §3 only says "time-limited"; 30 minutes is
    // an AI-proposed default, not a value taken from an approved source —
    // flagged in SLICE-S4-NOTES.md for human confirmation.
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // feature-specification.md §2: "wrong email/password → generic error,
        // no field disclosure" — unknown email and wrong password must be
        // indistinguishable, so both paths throw the exact same exception.
        if (request.email() == null || request.password() == null) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        JwtService.IssuedToken issuedToken = jwtService.issueAccessToken(user.getId());
        return AuthResponse.bearer(issuedToken.token(), issuedToken.expiresAt());
    }

    /**
     * feature-specification.md §3: "unregistered email -> same generic
     * confirmation message" — this method never reveals whether the email
     * exists. It only *acts* (creates a token) when the email matches a
     * real user; the caller returns the same response either way.
     */
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request.email() == null) {
            return;
        }

        Optional<User> user = userRepository.findByEmailIgnoreCase(request.email());
        if (user.isEmpty()) {
            return;
        }

        String token = generateResetToken();
        Instant expiresAt = Instant.now().plus(RESET_TOKEN_TTL);
        passwordResetTokenRepository.save(
                new PasswordResetToken(token, user.get().getId(), expiresAt));

        // KNOWN GAP (see SLICE-S4-NOTES.md): no email provider is approved
        // in any ADR, so there is no real delivery mechanism yet. Logging
        // the link is a deliberate, clearly-flagged stand-in, not a
        // silent decision to skip delivery.
        log.info("Password reset requested for user id {}. DEV-ONLY STAND-IN "
                        + "(no email provider approved yet, see SLICE-S4-NOTES.md): "
                        + "reset token = {}",
                user.get().getId(), token);
    }

    /**
     * feature-specification.md §3: token is single-use and time-limited;
     * a successful reset makes the old password invalid (overwriting the
     * hash achieves this with no separate "invalidate" step needed).
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(request.token())
                .filter(t -> t.isUsable(now))
                .orElseThrow(InvalidOrExpiredResetTokenException::new);

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(InvalidOrExpiredResetTokenException::new);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.markUsed(now);

        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);
    }

    private String generateResetToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
