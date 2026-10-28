package com.engcoach.parts;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class QuestionBankSeederTest {

    @Test
    void seedsOriginalReadingQuestionsOnlyWhenBankIsEmpty() {
        QuestionRepository repository = mock(QuestionRepository.class);
        when(repository.count()).thenReturn(0L);

        QuestionBankSeeder seeder = new QuestionBankSeeder(
                repository, new ObjectMapper(), new DefaultResourceLoader());

        seeder.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Question>> captor =
                (ArgumentCaptor<Iterable<Question>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());

        List<Question> saved = StreamSupport.stream(captor.getValue().spliterator(), false).toList();
        assertEquals(6, saved.size());
        assertEquals("Part 5", saved.get(0).getPart());
        assertEquals("once", saved.get(0).getCorrectOption());
        assertTrue(saved.stream().anyMatch(question -> "Part 6".equals(question.getPart())
                && question.getPassage() != null));
        assertTrue(saved.stream().anyMatch(question -> "Part 7".equals(question.getPart())
                && question.getPassage() != null));
    }

    @Test
    void doesNotAppendSampleQuestionsToANonEmptyBank() {
        QuestionRepository repository = mock(QuestionRepository.class);
        when(repository.count()).thenReturn(1L);

        QuestionBankSeeder seeder = new QuestionBankSeeder(
                repository, new ObjectMapper(), new DefaultResourceLoader());

        seeder.run();

        verify(repository, never()).saveAll(any());
    }
}
