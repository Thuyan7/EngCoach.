package com.engcoach.attempts;

import com.engcoach.attempts.dto.AnswerResponse;
import com.engcoach.attempts.dto.AttemptDetailResponse;
import com.engcoach.attempts.dto.AttemptResponse;
import com.engcoach.attempts.dto.AttemptSummaryResponse;
import com.engcoach.attempts.dto.ExplanationResponse;
import com.engcoach.attempts.dto.SaveAnswerRequest;
import com.engcoach.attempts.dto.StartAttemptRequest;
import com.engcoach.attempts.dto.SubmitAttemptResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S6a: {@code POST /attempts} and
 * {@code PUT /attempts/{id}/answers/{questionId}}. S7a adds
 * {@code GET /attempts/{id}}. S8a adds
 * {@code POST /attempts/{id}/submit}. S8b adds
 * {@code POST /attempts/{id}/answers/{questionId}/retry-explanation}. S10a
 * adds {@code GET /attempts} and {@code DELETE /attempts/{id}}. All seven
 * declared in
 * chapter-05-ai-for-software-design-architecture/design/openapi.yaml.
 *
 * No {@code permitAll()} entry needed in SecurityConfig — protected by the
 * existing {@code anyRequest().authenticated()} default plus
 * JwtAuthenticationFilter (S3), same as PartsController (S5a). The
 * authenticated principal (the caller's user id) comes from
 * {@link Authentication#getPrincipal()}, per JwtAuthenticationFilter's
 * contract (subject = user id only, no roles).
 */
@RestController
@RequestMapping("/attempts")
public class AttemptsController {

    private final AttemptsService attemptsService;

    public AttemptsController(AttemptsService attemptsService) {
        this.attemptsService = attemptsService;
    }

    @PostMapping
    public ResponseEntity<AttemptResponse> startAttempt(@Valid @RequestBody StartAttemptRequest request,
                                                          Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AttemptResponse response = attemptsService.startAttempt(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/answers/{questionId}")
    public ResponseEntity<AnswerResponse> saveAnswer(@PathVariable Long id,
                                                       @PathVariable Long questionId,
                                                       @Valid @RequestBody SaveAnswerRequest request,
                                                       Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AnswerResponse response = attemptsService.saveAnswer(userId, id, questionId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttemptDetailResponse> getAttempt(@PathVariable Long id,
                                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        AttemptDetailResponse response = attemptsService.getAttemptDetail(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmitAttemptResponse> submitAttempt(@PathVariable Long id,
                                                                  Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        SubmitAttemptResponse response = attemptsService.submitAttempt(userId, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/answers/{questionId}/retry-explanation")
    public ResponseEntity<ExplanationResponse> retryExplanation(@PathVariable Long id,
                                                                    @PathVariable Long questionId,
                                                                    Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        ExplanationResponse response = attemptsService.retryExplanation(userId, id, questionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AttemptSummaryResponse>> listHistory(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        List<AttemptSummaryResponse> response = attemptsService.listHistory(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttempt(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        attemptsService.deleteAttempt(userId, id);
        return ResponseEntity.noContent().build();
    }
}
