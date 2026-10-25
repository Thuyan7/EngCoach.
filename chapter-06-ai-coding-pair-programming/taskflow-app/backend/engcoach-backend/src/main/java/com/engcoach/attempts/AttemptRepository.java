package com.engcoach.attempts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {

    /**
     * Backs the S6a resume semantics: "starting" a Part practice or mock
     * test attempt first looks for an existing {@code IN_PROGRESS} attempt
     * scoped by (user, type, part) and returns that instead of creating a
     * duplicate. For {@code MOCK_TEST}, {@code part} is always {@code null}
     * on both the existing row and the lookup, so the scoping still holds
     * (one in-progress mock test per user at a time).
     */
    Optional<Attempt> findFirstByUserIdAndTypeAndPartAndStatus(
            Long userId, String type, String part, String status);

    /**
     * S10a: backs {@code GET /attempts} (history). Approved decision #1:
     * only {@code SUBMITTED} attempts appear in history (feature-spec §7:
     * "reflects all completed attempts") — an {@code IN_PROGRESS} one is
     * resumed via {@code POST /attempts}, not listed here. Approved
     * decision #2: most-recent-first, ordered by {@code startedAt} since
     * no separate "submitted at" timestamp exists on {@code Attempt}.
     */
    List<Attempt> findByUserIdAndStatusOrderByStartedAtDesc(Long userId, String status);
}
