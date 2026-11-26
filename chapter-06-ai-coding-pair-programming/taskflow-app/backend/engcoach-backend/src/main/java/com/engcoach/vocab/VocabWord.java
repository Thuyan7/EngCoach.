package com.engcoach.vocab;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Locale;

/** A word or phrase saved by a learner, with its flashcard review state. */
@Entity
@Table(name = "vocab_words",
        uniqueConstraints = @UniqueConstraint(name = "uk_vocab_user_word", columnNames = {"user_id", "word_key"}))
public class VocabWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String word;

    /** Lower-case form of the word, so "Invoice" and "invoice" count as the same word for one learner. */
    @Column(name = "word_key", nullable = false, length = 100)
    private String wordKey;

    @Column(nullable = false, length = 500)
    private String meaning;

    @Column(length = 500)
    private String example;

    /** The question the word came from, if it was saved while reviewing one. */
    @Column(name = "question_id")
    private Long questionId;

    @Column(nullable = false)
    private int box;

    @Column(name = "next_review_at", nullable = false)
    private Instant nextReviewAt;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    /** The learner says they know this word. Flashcards can still be used with it at any time. */
    @Column(nullable = false)
    private boolean learned;

    /** The learner starred this word to study it as flashcards. */
    @Column(nullable = false)
    private boolean marked;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VocabWord() {
        // required by JPA
    }

    public VocabWord(Long userId, String word, String meaning, String example, Long questionId, Instant now) {
        this.userId = userId;
        this.questionId = questionId;
        this.box = ReviewSchedule.FIRST_BOX;
        this.nextReviewAt = now; // a new word can be studied straight away
        this.createdAt = now;
        edit(word, meaning, example);
    }

    public static String keyOf(String word) {
        return word.trim().toLowerCase(Locale.ROOT);
    }

    public void edit(String word, String meaning, String example) {
        this.word = word.trim();
        this.wordKey = keyOf(word);
        this.meaning = meaning.trim();
        this.example = example == null || example.isBlank() ? null : example.trim();
    }

    public void review(boolean remembered, Instant now) {
        ReviewSchedule.Next next = ReviewSchedule.after(box, remembered, now);
        this.box = next.box();
        this.nextReviewAt = next.nextReviewAt();
        this.reviewCount++;
        this.learned = remembered;
    }

    /** Changes only the flags that are given. */
    public void flag(Boolean learned, Boolean marked) {
        if (learned != null) this.learned = learned;
        if (marked != null) this.marked = marked;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getWord() { return word; }
    public String getMeaning() { return meaning; }
    public String getExample() { return example; }
    public Long getQuestionId() { return questionId; }
    public int getBox() { return box; }
    public Instant getNextReviewAt() { return nextReviewAt; }
    public int getReviewCount() { return reviewCount; }
    public boolean isLearned() { return learned; }
    public boolean isMarked() { return marked; }
    public Instant getCreatedAt() { return createdAt; }
}
