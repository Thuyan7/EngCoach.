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
    void seedsCompleteSevenPartMockBankOnlyWhenBankIsEmpty() {
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
        assertEquals(200, saved.size());
        assertEquals(6, saved.stream().filter(question -> "Part 1".equals(question.getPart())).count());
        assertEquals(25, saved.stream().filter(question -> "Part 2".equals(question.getPart())).count());
        assertEquals(39, saved.stream().filter(question -> "Part 3".equals(question.getPart())).count());
        assertEquals(30, saved.stream().filter(question -> "Part 4".equals(question.getPart())).count());
        assertEquals(30, saved.stream().filter(question -> "Part 5".equals(question.getPart())).count());
        assertEquals(16, saved.stream().filter(question -> "Part 6".equals(question.getPart())).count());
        assertEquals(54, saved.stream().filter(question -> "Part 7".equals(question.getPart())).count());
        assertTrue(saved.stream().filter(question -> question.getPart().equals("Part 1"))
                .allMatch(question -> question.getImageUrl() != null && question.getAudioText() != null));
        assertTrue(saved.stream().filter(question -> question.getPart().equals("Part 2")
                || question.getPart().equals("Part 3") || question.getPart().equals("Part 4"))
                .allMatch(question -> question.getAudioText() != null && !question.getAudioText().isBlank()));
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
