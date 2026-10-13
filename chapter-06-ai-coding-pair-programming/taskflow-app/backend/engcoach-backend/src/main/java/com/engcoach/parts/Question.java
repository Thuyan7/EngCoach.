package com.engcoach.parts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Maps 1:1 to the {@code Question} entity in
 * chapter-05-ai-for-software-design-architecture/design/data-model.mmd.
 * Fields: id, part, prompt, options, correct_option.
 *
 * S5a scope only: this slice reads {@code part} (via
 * {@link QuestionRepository#findDistinctParts()}) to back {@code GET
 * /parts}. {@code prompt}/{@code options}/{@code correctOption} are mapped
 * here because they're part of the approved entity, but nothing in this
 * slice reads or writes them — no question-authoring/seeding mechanism and
 * no grading logic exist yet (grading is S8a+). {@code options} is stored
 * as a raw JSON string, not parsed into a structured type, since no caller
 * needs it parsed until a later slice does.
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String part;

    @Lob
    @Column(nullable = false)
    private String prompt;

    @Lob
    @Column(nullable = false)
    private String options;

    @Column(name = "correct_option", nullable = false)
    private String correctOption;

    protected Question() {
        // required by JPA
    }

    public Question(String part, String prompt, String options, String correctOption) {
        this.part = part;
        this.prompt = prompt;
        this.options = options;
        this.correctOption = correctOption;
    }

    public Long getId() {
        return id;
    }

    public String getPart() {
        return part;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getOptions() {
        return options;
    }

    public String getCorrectOption() {
        return correctOption;
    }
}
