package com.engcoach.tests;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_questions")
public class TestQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_id", nullable = false)
    private Long testId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected TestQuestion() { }

    public TestQuestion(Long testId, Long questionId, Long groupId, int sortOrder) {
        this.testId = testId;
        this.questionId = questionId;
        this.groupId = groupId;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Long getTestId() { return testId; }
    public Long getQuestionId() { return questionId; }
    public Long getGroupId() { return groupId; }
    public int getSortOrder() { return sortOrder; }
}
