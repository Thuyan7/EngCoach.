package com.engcoach.attempts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Maps to the {@code Answer} entity in
 * chapter-05-ai-for-software-design-architecture/design/data-model.mmd.
 * Fields: id, attempt_id, question_id, chosen_option, is_correct,
 * explanation_text, explanation_status.
 *
 * S6a scope only: writes/reads {@code attemptId}, {@code questionId},
 * {@code chosenOption} — one row per (attempt, question), auto-saved and
 * overwritten on every re-answer (see AttemptsService.saveAnswer).
 * {@code isCorrect}, {@code explanationText}, {@code explanationStatus}
 * are mapped here because they're part of the approved entity. S8a sets
 * {@code isCorrect} at grading time. S8b sets {@code explanationText}/
 * {@code explanationStatus}, but only for incorrect answers — a correct
 * answer's explanation fields stay {@code null} forever (no explanation
 * is ever requested for it).
 */
@Entity
@Table(name = "answers")
public class Answer {

    /** S8b: Anthropic call succeeded; {@code explanationText} is usable. */
    public static final String EXPLANATION_STATUS_OK = "OK";
    /**
     * S8b: Anthropic call failed, or no API key is configured
     * (ADR-004/decision #4/#7) — {@code explanationText} stays null. Not
     * an application error; grading still succeeds.
     */
    public static final String EXPLANATION_STATUS_UNAVAILABLE = "UNAVAILABLE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "chosen_option", nullable = false)
    private String chosenOption;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Lob
    @Column(name = "explanation_text")
    private String explanationText;

    @Column(name = "explanation_status")
    private String explanationStatus;

    protected Answer() {
        // required by JPA
    }

    public Answer(Long attemptId, Long questionId, String chosenOption) {
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.chosenOption = chosenOption;
    }

    public Long getId() {
        return id;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getChosenOption() {
        return chosenOption;
    }

    public void setChosenOption(String chosenOption) {
        this.chosenOption = chosenOption;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    /** S8a: set once, at grading time, by {@code AttemptsService.submitAttempt}. */
    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public String getExplanationText() {
        return explanationText;
    }

    public String getExplanationStatus() {
        return explanationStatus;
    }

    /**
     * S8b: set once per attempted explanation generation (first grading,
     * or a later retry) by {@code AttemptGradingService.saveExplanationResult}.
     * {@code explanationText} is null when {@code explanationStatus} is
     * {@link #EXPLANATION_STATUS_UNAVAILABLE}.
     */
    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
    }

    public void setExplanationStatus(String explanationStatus) {
        this.explanationStatus = explanationStatus;
    }
}
