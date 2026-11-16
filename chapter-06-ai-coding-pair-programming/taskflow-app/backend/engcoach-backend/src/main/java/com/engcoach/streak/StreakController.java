package com.engcoach.streak;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/streak")
public class StreakController {

    private final StreakService streakService;

    public StreakController(StreakService streakService) {
        this.streakService = streakService;
    }

    /** Current and longest streak plus the study days of {@code month} (yyyy-MM, default this month). */
    @GetMapping
    public ResponseEntity<StreakResponse> streak(@RequestParam(required = false) String timeZone,
                                                 @RequestParam(required = false) String month,
                                                 Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(streakService.streakOf(userId, timeZone, month));
    }
}
