package com.engcoach.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralizes RFC 9457 problem responses (design-patterns.md §5,
 * software-architecture.md §5) so every failure across the API has the same
 * type/title/status/detail/instance shape, instead of each controller
 * formatting its own error body.
 *
 * S1 added the first two failure cases (409 duplicate email, 422
 * validation). S2 adds a third (401 invalid credentials). S4 adds a fourth
 * (410 invalid/expired reset token). S6a adds three more (404 attempt not
 * found, 403 attempt belongs to another user, 422 Part has no questions).
 * S8b adds two more (404 answer not found, 409 explanation retry not
 * allowed). More exception mappings are added here, not duplicated
 * elsewhere, as later slices introduce new failure cases.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE = "https://engcoach.app/problems/";

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex,
                                                        HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "email-already-registered"));
        problem.setTitle("Email already registered");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex,
                                                    HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "invalid-credentials"));
        problem.setTitle("Invalid credentials");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(InvalidOrExpiredResetTokenException.class)
    public ProblemDetail handleInvalidOrExpiredResetToken(InvalidOrExpiredResetTokenException ex,
                                                            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "invalid-or-expired-reset-token"));
        problem.setTitle("Invalid or expired reset token");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(AttemptNotFoundException.class)
    public ProblemDetail handleAttemptNotFound(AttemptNotFoundException ex,
                                                 HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "attempt-not-found"));
        problem.setTitle("Attempt not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(AttemptAccessDeniedException.class)
    public ProblemDetail handleAttemptAccessDenied(AttemptAccessDeniedException ex,
                                                     HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "attempt-access-denied"));
        problem.setTitle("Attempt belongs to another user");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(AttemptNotInProgressException.class)
    public ProblemDetail handleAttemptNotInProgress(AttemptNotInProgressException ex,
                                                      HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "attempt-not-in-progress"));
        problem.setTitle("Attempt is not in progress");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(InvalidAnswerException.class)
    public ProblemDetail handleInvalidAnswer(InvalidAnswerException ex,
                                               HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "invalid-answer"));
        problem.setTitle("Invalid answer");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(PartHasNoQuestionsException.class)
    public ProblemDetail handlePartHasNoQuestions(PartHasNoQuestionsException ex,
                                                    HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "part-has-no-questions"));
        problem.setTitle("Part has no questions");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(AnswerNotFoundException.class)
    public ProblemDetail handleAnswerNotFound(AnswerNotFoundException ex,
                                                HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "answer-not-found"));
        problem.setTitle("Answer not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(ExplanationRetryNotAllowedException.class)
    public ProblemDetail handleExplanationRetryNotAllowed(ExplanationRetryNotAllowedException ex,
                                                             HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "explanation-retry-not-allowed"));
        problem.setTitle("Explanation retry not allowed");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                           HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "One or more fields are invalid.");
        problem.setType(URI.create(PROBLEM_BASE + "validation-error"));
        problem.setTitle("Validation failed");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }
}
