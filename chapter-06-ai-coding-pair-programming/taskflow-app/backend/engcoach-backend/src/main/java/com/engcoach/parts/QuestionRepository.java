package com.engcoach.parts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Backs {@code GET /parts} (implementation-plan.md S5a: "returns all
     * distinct Parts"). Returns an empty list if no questions exist yet —
     * that's the expected state until a question bank is seeded, not an
     * error (see PartsControllerTest and SLICE-S5A-NOTES.md).
     */
    @Query("SELECT DISTINCT q.part FROM Question q ORDER BY q.part")
    List<String> findDistinctParts();

    /**
     * Backs S6a's {@code POST /attempts} validation (approved design
     * decision #3): a Part practice attempt can't be started for a
     * {@code part} with zero questions — checked before creating the
     * {@code Attempt} row, not after.
     */
    boolean existsByPart(String part);
}
