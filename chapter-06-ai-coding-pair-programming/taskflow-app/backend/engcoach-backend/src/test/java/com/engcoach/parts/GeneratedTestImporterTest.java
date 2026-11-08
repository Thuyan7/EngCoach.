package com.engcoach.parts;

import com.engcoach.tests.QuestionGroup;
import com.engcoach.tests.QuestionGroupRepository;
import com.engcoach.tests.TestPart;
import com.engcoach.tests.TestPartRepository;
import com.engcoach.tests.TestQuestion;
import com.engcoach.tests.TestQuestionRepository;
import com.engcoach.tests.ToeicTest;
import com.engcoach.tests.ToeicTestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GeneratedTestImporterTest {

    private QuestionRepository questionRepository;
    private ToeicTestRepository toeicTestRepository;
    private TestPartRepository testPartRepository;
    private TestQuestionRepository testQuestionRepository;
    private QuestionGroupRepository questionGroupRepository;
    private GeneratedTestImporter importer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        questionRepository = mock(QuestionRepository.class);
        toeicTestRepository = mock(ToeicTestRepository.class);
        testPartRepository = mock(TestPartRepository.class);
        testQuestionRepository = mock(TestQuestionRepository.class);
        questionGroupRepository = mock(QuestionGroupRepository.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        // Chạy callback ngay, không cần transaction thật trong unit test.
        doAnswer(invocation -> {
            ((Consumer<Object>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        when(questionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(toeicTestRepository.save(any(ToeicTest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionGroupRepository.save(any(QuestionGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        importer = new GeneratedTestImporter(questionRepository, toeicTestRepository, testPartRepository,
                testQuestionRepository, questionGroupRepository, new ObjectMapper(),
                new PathMatchingResourcePatternResolver(), transactionTemplate,
                "classpath*:importer-fixtures/test-*.json", false);
    }

    @Test
    void importsQuestionsPartsGroupsAndLinksFromAGeneratedFile() throws Exception {
        when(toeicTestRepository.existsByTitle("Fixture Generated Test")).thenReturn(false);

        importer.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Question>> questions = ArgumentCaptor.forClass(List.class);
        verify(questionRepository).saveAll(questions.capture());
        assertEquals(5, questions.getValue().size());
        Question first = questions.getValue().get(0);
        assertEquals("Part 1", first.getPart());
        assertEquals("/part1/pexels-1.jpg", first.getImageUrl());
        assertEquals("/audio/test-01/t01-p1-g01.mp3", first.getAudioUrl());
        assertEquals("Cars are parked.", first.getCorrectOption());

        // Part 1, Part 3, Part 5, Part 7 -> 4 TestPart; Part 5 không có group.
        verify(testPartRepository, times(4)).save(any(TestPart.class));
        // Hai câu Part 3 cùng groupKey phải dùng chung một group: 3 group tổng cộng (Part 1, Part 3, Part 7).
        verify(questionGroupRepository, times(3)).save(any(QuestionGroup.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TestQuestion>> links = ArgumentCaptor.forClass(List.class);
        verify(testQuestionRepository).saveAll(links.capture());
        assertEquals(5, links.getValue().size());
    }

    @Test
    void skipsATestWhoseTitleAlreadyExists() throws Exception {
        when(toeicTestRepository.existsByTitle("Fixture Generated Test")).thenReturn(true);

        importer.run();

        verify(questionRepository, never()).saveAll(anyList());
        verify(toeicTestRepository, never()).save(any(ToeicTest.class));
    }

    @Test
    void refreshUpdatesAnExistingTestInPlaceWhenEnabled() throws Exception {
        GeneratedTestImporter refreshing = new GeneratedTestImporter(questionRepository, toeicTestRepository,
                testPartRepository, testQuestionRepository, questionGroupRepository, new ObjectMapper(),
                new PathMatchingResourcePatternResolver(), transactionTemplateRunningCallbacks(),
                "classpath*:importer-fixtures/test-*.json", true);
        ToeicTest existing = mock(ToeicTest.class);
        when(existing.getId()).thenReturn(9L);
        when(toeicTestRepository.existsByTitle("Fixture Generated Test")).thenReturn(true);
        when(toeicTestRepository.findFirstByTitle("Fixture Generated Test")).thenReturn(java.util.Optional.of(existing));
        List<TestQuestion> links = new java.util.ArrayList<>();
        List<Question> stored = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Question q = mock(Question.class);
            when(q.getId()).thenReturn(100L + i);
            stored.add(q);
            links.add(new TestQuestion(9L, 100L + i, null, i + 1));
        }
        when(testQuestionRepository.findByTestIdOrderBySortOrderAsc(9L)).thenReturn(links);
        when(questionRepository.findAllById(any())).thenReturn(stored);

        refreshing.run();

        for (Question q : stored) {
            verify(q).refreshFrom(any(), any(), any(), any(), any(), any(), any(), any());
        }
        verify(toeicTestRepository, never()).save(any(ToeicTest.class));
    }

    @Test
    void refreshFailsClearlyWhenQuestionCountDiffers() {
        GeneratedTestImporter refreshing = new GeneratedTestImporter(questionRepository, toeicTestRepository,
                testPartRepository, testQuestionRepository, questionGroupRepository, new ObjectMapper(),
                new PathMatchingResourcePatternResolver(), transactionTemplateRunningCallbacks(),
                "classpath*:importer-fixtures/test-*.json", true);
        ToeicTest existing = mock(ToeicTest.class);
        when(existing.getId()).thenReturn(9L);
        when(toeicTestRepository.existsByTitle("Fixture Generated Test")).thenReturn(true);
        when(toeicTestRepository.findFirstByTitle("Fixture Generated Test")).thenReturn(java.util.Optional.of(existing));
        when(testQuestionRepository.findByTestIdOrderBySortOrderAsc(9L)).thenReturn(List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, refreshing::run);
        assertTrue(e.getMessage().contains("xóa đề cũ"));
    }

    @SuppressWarnings("unchecked")
    private TransactionTemplate transactionTemplateRunningCallbacks() {
        TransactionTemplate template = mock(TransactionTemplate.class);
        doAnswer(invocation -> {
            ((Consumer<Object>) invocation.getArgument(0)).accept(null);
            return null;
        }).when(template).executeWithoutResult(any());
        return template;
    }

    @Test
    void rejectsAQuestionWhoseCorrectOptionIsNotAmongItsOptions() {
        GeneratedTestImporter.GeneratedQuestion bad = new GeneratedTestImporter.GeneratedQuestion(
                "Part 5", "The ______ is ready.", List.of("a", "b"), "c", null, null, null, null, "g1");
        GeneratedTestImporter.GeneratedTest test = new GeneratedTestImporter.GeneratedTest("T", "d", List.of(bad));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> importer.validate(test, "bad.json"));
        assertTrue(e.getMessage().contains("bad.json"));
    }

    @Test
    void rejectsPartOneWithoutAnImage() {
        GeneratedTestImporter.GeneratedQuestion bad = new GeneratedTestImporter.GeneratedQuestion(
                "Part 1", "Look at the photo.", List.of("a", "b", "c", "d"), "a", null, null, null,
                "Number 1. A. a", "g1");
        GeneratedTestImporter.GeneratedTest test = new GeneratedTestImporter.GeneratedTest("T", "d", List.of(bad));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> importer.validate(test, "p1.json"));
        assertTrue(e.getMessage().contains("nguồn ảnh Part 1"));
    }
}
