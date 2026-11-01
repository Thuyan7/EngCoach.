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
 * Opt-in additive seed of 24 original EngCoach Part 2 practice items.
 * These are text-based practice prompts; no MP3 files are included.
 */
@Component
@ConditionalOnProperty(prefix = "engcoach.question-bank", name = "seed-original-part2", havingValue = "true")
public class OriginalPartTwoSeeder implements CommandLineRunner {

    private static final String RESOURCE_PATH =
            "classpath:question-bank/original-part2-practice.json";

    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public OriginalPartTwoSeeder(QuestionRepository questionRepository, ObjectMapper objectMapper,
                                 ResourceLoader resourceLoader) {
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) {
        Resource resource = resourceLoader.getResource(RESOURCE_PATH);
        try (InputStream input = resource.getInputStream()) {
            List<SeedQuestion> rows = objectMapper.readValue(input, new TypeReference<>() { });
            List<Question> additions = new ArrayList<>();
            int skipped = 0;
            for (SeedQuestion row : rows) {
                validate(row);
                if (questionRepository.existsByPartAndPrompt("Part 2", row.prompt())) {
                    skipped++;
                    continue;
                }
                String optionsJson = objectMapper.writeValueAsString(row.options());
                additions.add(new Question("Part 2", row.prompt(), optionsJson, row.correctOption(),
                        null, null, null, row.audioText()));
            }
            if (!additions.isEmpty()) {
                questionRepository.saveAll(additions);
            }
            System.out.printf("EngCoach original Part 2 seed complete: added %d, skipped %d existing prompts. Text-only; no MP3 files included.%n",
                    additions.size(), skipped);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load original Part 2 practice data from "
                    + RESOURCE_PATH, e);
        }
    }

    private void validate(SeedQuestion row) {
        if (row == null || row.part() == null || !"Part 2".equals(row.part())
                || row.prompt() == null || row.prompt().isBlank()
                || row.options() == null || row.options().size() != 3
                || row.options().stream().anyMatch(option -> option == null || option.isBlank())
                || row.correctOption() == null || !row.options().contains(row.correctOption())
                || row.audioText() == null || row.audioText().isBlank()) {
            throw new IllegalStateException("Invalid original Part 2 question in " + RESOURCE_PATH);
        }
    }

    private record SeedQuestion(String part, String prompt, List<String> options,
                                 String correctOption, String passage, String imageUrl,
                                 String audioUrl, String audioText, String source) {
    }
}
