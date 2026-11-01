package com.engcoach.tests;

import com.engcoach.common.AttemptNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TestCatalogService {
    private final ToeicTestRepository testRepository;
    private final TestPartRepository testPartRepository;

    public TestCatalogService(ToeicTestRepository testRepository, TestPartRepository testPartRepository) {
        this.testRepository = testRepository;
        this.testPartRepository = testPartRepository;
    }

    @Transactional(readOnly = true)
    public List<ToeicTestView> listTests() {
        return testRepository.findByPublishedTrueOrderByIdAsc().stream()
                .map(test -> ToeicTestView.from(test, testPartRepository.findByTestIdOrderBySortOrderAsc(test.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ToeicTestView getTest(Long testId) {
        ToeicTest test = testRepository.findById(testId).filter(ToeicTest::isPublished)
                .orElseThrow(AttemptNotFoundException::new);
        return ToeicTestView.from(test, testPartRepository.findByTestIdOrderBySortOrderAsc(testId));
    }
}
