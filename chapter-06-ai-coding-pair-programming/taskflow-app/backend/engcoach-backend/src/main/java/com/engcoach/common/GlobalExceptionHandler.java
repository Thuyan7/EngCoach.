package com.engcoach.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

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
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(AttemptTimeExpiredException.class)
    public ProblemDetail handleAttemptTimeExpired(AttemptTimeExpiredException ex,
                                                    HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "attempt-time-expired"));
        problem.setTitle("Attempt time has expired");
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

    @ExceptionHandler(UnsupportedAttemptTypeException.class)
    public ProblemDetail handleUnsupportedAttemptType(UnsupportedAttemptTypeException ex,
                                                        HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "unsupported-attempt-type"));
        problem.setTitle("Unsupported attempt type");
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

    @ExceptionHandler(VocabWordNotFoundException.class)
    public ProblemDetail handleVocabWordNotFound(VocabWordNotFoundException ex,
                                                  HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "vocab-word-not-found"));
        problem.setTitle("Vocabulary word not found");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(VocabWordAlreadyExistsException.class)
    public ProblemDetail handleVocabWordAlreadyExists(VocabWordAlreadyExistsException ex,
                                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setType(URI.create(PROBLEM_BASE + "vocab-word-already-exists"));
        problem.setTitle("Word already saved");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    /** 422 with per-field messages; same body as before this class extended ResponseEntityExceptionHandler. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   HttpHeaders headers, HttpStatusCode status,
                                                                   WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "One or more fields are invalid.");
        problem.setType(URI.create(PROBLEM_BASE + "validation-error"));
        problem.setTitle("Validation failed");
        problem.setInstance(URI.create(path(request)));
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).headers(headers).body(problem);
    }

    /** 400 when the request body is not readable JSON. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                   HttpHeaders headers, HttpStatusCode status,
                                                                   WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON.");
        problem.setType(URI.create(PROBLEM_BASE + "malformed-request"));
        problem.setTitle("Malformed request");
        problem.setInstance(URI.create(path(request)));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).headers(headers).body(problem);
    }

    /** 409 for a database constraint violation; the database message is never sent to the client. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex,
                                              HttpServletRequest request) {
        log.warn("Data integrity violation on {}", request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The request conflicts with existing data.");
        problem.setType(URI.create(PROBLEM_BASE + "data-conflict"));
        problem.setTitle("Data conflict");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    /**
     * Last resort: any other exception becomes a generic 500 problem. Spring MVC's own exceptions
     * (405, 415, type mismatch, ...) are handled by the parent class, and Spring Security's
     * exceptions are re-thrown so the security filters still answer 401/403.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) throws Exception {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setType(URI.create(PROBLEM_BASE + "internal-error"));
        problem.setTitle("Internal server error");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private static String path(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }
}
