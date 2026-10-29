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
 * Core fields: id, part, prompt, options, correct_option. Optional passage
 * and media URLs support TOEIC reading/listening material. The options field
 * remains a JSON array of strings so the existing grading contract stays
 * unchanged; correctOption must exactly match one of those strings.
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

    @Column(columnDefinition = "LONGTEXT")
    private String passage;

    @Column(length = 2048)
    private String imageUrl;

    @Column(length = 2048)
    private String audioUrl;

    @Lob
    @Column(name = "audio_text")
    private String audioText;

    protected Question() {
        // required by JPA
    }

    public Question(String part, String prompt, String options, String correctOption) {
        this(part, prompt, options, correctOption, null, null, null);
    }

    public Question(String part, String prompt, String options, String correctOption,
                    String passage, String imageUrl, String audioUrl) {
        this(part, prompt, options, correctOption, passage, imageUrl, audioUrl, null);
    }

    public Question(String part, String prompt, String options, String correctOption,
                    String passage, String imageUrl, String audioUrl, String audioText) {
        this.part = part;
        this.prompt = prompt;
        this.options = options;
        this.correctOption = correctOption;
        this.passage = passage;
        this.imageUrl = imageUrl;
        this.audioUrl = audioUrl;
        this.audioText = audioText;
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

    public String getPassage() {
        return passage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public String getAudioText() {
        return audioText;
    }
}
