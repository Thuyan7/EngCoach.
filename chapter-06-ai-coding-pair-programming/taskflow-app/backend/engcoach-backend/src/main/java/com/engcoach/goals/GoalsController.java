package com.engcoach.goals;

import com.engcoach.goals.GoalsDtos.GoalsResponse;
import com.engcoach.goals.GoalsDtos.UpdateGoalsRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/goals")
public class GoalsController {

    private final StudyGoalService studyGoalService;

    public GoalsController(StudyGoalService studyGoalService) {
        this.studyGoalService = studyGoalService;
    }

    /** Goals, today's progress and streak. {@code timeZone} is an IANA name such as Asia/Saigon (default UTC). */
    @GetMapping
    public ResponseEntity<GoalsResponse> goals(@RequestParam(required = false) String timeZone,
                                               Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(studyGoalService.goalsOf(userId, timeZone));
    }

    @PutMapping
    public ResponseEntity<GoalsResponse> update(@Valid @RequestBody UpdateGoalsRequest request,
                                                @RequestParam(required = false) String timeZone,
                                                Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(studyGoalService.update(userId, request, timeZone));
    }
}
