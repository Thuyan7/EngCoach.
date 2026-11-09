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

    /**
     * Backs S8a's grading (approved design decision #2): every question in
     * a Part, used to compute {@code totalQuestions} and to find which
     * ones were never answered ("unanswered = incorrect") — not just the
     * ones the caller happened to save an `Answer` for.
     */
    List<Question> findByPart(String part);

    /**
     * Backs S7b's question list on {@code GET /attempts/{id}}: the questions
     * of a Part in a stable order (by id), so a refresh shows the same
     * question sequence every time.
     */
    List<Question> findByPartOrderByIdAsc(String part);

    /**
     * S11: the {@code MOCK_TEST} equivalent of {@link #findByPartOrderByIdAsc}
     * — every question in the whole bank, in a stable (by id) order,
     * backing both {@code AttemptGradingService}'s grading (approved
     * decisions #1/#2: the full bank is a {@code MOCK_TEST}'s "assigned
     * question set") and {@code GET /attempts/{id}}'s question list
     * (same "whatever grading uses, the detail view shows" precedent S7b
     * established for {@code PART_PRACTICE}).
     */
    List<Question> findAllByOrderByIdAsc();

    /** Câu của một Part nằm trong ít nhất một đề (Test), theo thứ tự id = thứ tự đề được nạp. */
    @org.springframework.data.jpa.repository.Query("SELECT q FROM Question q WHERE q.part = :part AND EXISTS "
            + "(SELECT 1 FROM com.engcoach.tests.TestQuestion tq WHERE tq.questionId = q.id) ORDER BY q.id")
    List<Question> findLinkedToTestsByPart(@org.springframework.data.repository.query.Param("part") String part);

    /**
     * Bộ câu của bài luyện theo Part: ưu tiên các câu thuộc đề (có ảnh, audio, nhóm hội thoại/đoạn văn đầy đủ);
     * nếu Part chưa có câu nào trong đề thì dùng tất cả câu của Part như trước.
     */
    default List<Question> findPracticeSetByPart(String part) {
        List<Question> linked = findLinkedToTestsByPart(part);
        return linked.isEmpty() ? findByPartOrderByIdAsc(part) : linked;
    }
}
