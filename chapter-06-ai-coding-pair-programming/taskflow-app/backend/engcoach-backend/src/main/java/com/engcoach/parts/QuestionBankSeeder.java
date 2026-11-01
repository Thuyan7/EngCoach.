package com.engcoach.parts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional local-development seed for the 200-question TOEIC-style bank.
 * Disabled by default. It never mixes sample rows into a non-empty bank.
 */
@Component
@ConditionalOnProperty(prefix = "engcoach.question-bank", name = "seed-samples", havingValue = "true")
public class QuestionBankSeeder implements CommandLineRunner {

    private static final String RESOURCE_PATH = "classpath:question-bank/toeic-mock-bank.json";

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public QuestionBankSeeder(QuestionRepository questionRepository, ObjectMapper objectMapper,
                              ResourceLoader resourceLoader) {
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        if (questionRepository.count() > 0) {
            return;
        }

        Resource resource = resourceLoader.getResource(RESOURCE_PATH);
        try (InputStream input = resource.getInputStream()) {
            List<SeedQuestion> rows = objectMapper.readValue(input, new TypeReference<>() { });
            validateBank(rows);
            List<Question> questions = new ArrayList<>();
            for (SeedQuestion row : rows) {
                validate(row);
                String optionsJson = objectMapper.writeValueAsString(row.options());
                questions.add(new Question(row.part(), row.prompt(), optionsJson, row.correctOption(),
                        row.passage(), row.imageUrl(), row.audioUrl(), row.audioText()));
            }
            questionRepository.saveAll(questions);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the sample question bank from " + RESOURCE_PATH, e);
        }
    }

    private void validateBank(List<SeedQuestion> rows) {
        java.util.Map<String, Long> expected = java.util.Map.of(
                "Part 1", 6L, "Part 2", 25L, "Part 3", 39L, "Part 4", 30L,
                "Part 5", 30L, "Part 6", 16L, "Part 7", 54L);
        java.util.Map<String, Long> actual = rows.stream().collect(
                java.util.stream.Collectors.groupingBy(SeedQuestion::part, java.util.stream.Collectors.counting()));
        if (rows.size() != 200 || !actual.equals(expected)) {
            throw new IllegalStateException("The mock question bank must contain exactly 200 questions "
                    + "with the standard Part distribution; found " + actual);
        }
    }

    private void validate(SeedQuestion row) {
        if (row == null || row.part() == null || row.part().isBlank()
                || row.prompt() == null || row.prompt().isBlank()
                || row.options() == null || row.options().size() < 2
                || row.options().stream().anyMatch(option -> option == null || option.isBlank())
                || row.correctOption() == null || !row.options().contains(row.correctOption())
                || ("Part 1".equals(row.part()) && (isBlank(row.imageUrl()) || isBlank(row.audioText())))
                || (List.of("Part 2", "Part 3", "Part 4").contains(row.part()) && isBlank(row.audioText()))
                || (List.of("Part 6", "Part 7").contains(row.part()) && isBlank(row.passage()))) {
            throw new IllegalStateException("Invalid sample question in " + RESOURCE_PATH
                    + ": each row needs a part, prompt, at least two options, and a correctOption in options");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SeedQuestion(String part, String prompt, List<String> options, String correctOption,
                                String passage, String imageUrl, String audioUrl, String audioText) {
    }
}
