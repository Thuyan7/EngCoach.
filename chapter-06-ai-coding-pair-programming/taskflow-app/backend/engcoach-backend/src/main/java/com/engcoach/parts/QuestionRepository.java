package com.engcoach.parts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Backs {@code GET /parts} (implementation-plan.md S5a: "returns all
     * distinct Parts"). Returns an empty list if no questions exist yet — that's
     * the expected state until a question bank is seeded.
     */
    @Query("SELECT DISTINCT q.part FROM Question q ORDER BY q.part")
    List<String> findDistinctParts();

    /**
     * Used by the opt-in remote question-bank importer to preserve existing
     * questions and skip rows already present in the local bank.
     */
    boolean existsByPartAndPrompt(String part, String prompt);

    /**
     * Part 1 questions share a generic instruction prompt, so deduplication
     * uses the stable image URL for these photograph-description items.
     */
    boolean existsByPartAndImageUrl(String part, String imageUrl);

    /**
     * Backs S6a's {@code POST /attempts} validation: a Part practice attempt
     * can't be started for a part with zero questions.
     */
    boolean existsByPart(String part);

    /**
     * Questions in a Part, used for grading and unanswered-question handling.
     */
    List<Question> findByPart(String part);

    /**
     * Questions of a Part in stable ID order for attempt detail views.
     */
    List<Question> findByPartOrderByIdAsc(String part);

    /**
     * All questions in stable ID order for mock-test grading and detail views.
     */
    List<Question> findAllByOrderByIdAsc();
}
