package com.engcoach.tests;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface TestQuestionRepository extends JpaRepository<TestQuestion, Long> {
    List<TestQuestion> findByTestIdAndQuestionIdInOrderBySortOrderAsc(Long testId, Collection<Long> questionIds);
    List<TestQuestion> findByTestIdOrderBySortOrderAsc(Long testId);
    boolean existsByTestIdAndQuestionId(Long testId, Long questionId);
}
