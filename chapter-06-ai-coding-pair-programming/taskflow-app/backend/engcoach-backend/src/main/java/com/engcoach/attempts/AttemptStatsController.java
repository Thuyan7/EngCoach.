package com.engcoach.attempts;

import com.engcoach.attempts.dto.ScoreResponses.AttemptScoreResponse;
import com.engcoach.attempts.dto.ScoreResponses.PartStatsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Điểm TOEIC quy đổi và thống kê theo Part. */
@RestController
@RequestMapping("/attempts")
public class AttemptStatsController {

    private final AttemptStatsService attemptStatsService;

    public AttemptStatsController(AttemptStatsService attemptStatsService) {
        this.attemptStatsService = attemptStatsService;
    }

    /** Tổng hợp mọi bài đã nộp của người dùng. */
    @GetMapping("/part-stats")
    public ResponseEntity<PartStatsResponse> partStats(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(attemptStatsService.statsOfUser(userId));
    }

    /** Điểm theo Part và điểm quy đổi của một bài đã nộp. */
    @GetMapping("/{id}/score")
    public ResponseEntity<AttemptScoreResponse> attemptScore(@PathVariable Long id, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(attemptStatsService.scoreOfAttempt(userId, id));
    }
}
