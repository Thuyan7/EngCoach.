package com.engcoach.parts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.engcoach.tests.QuestionGroup;
import com.engcoach.tests.QuestionGroupRepository;
import com.engcoach.tests.TestPart;
import com.engcoach.tests.TestPartRepository;
import com.engcoach.tests.TestQuestion;
import com.engcoach.tests.TestQuestionRepository;
import com.engcoach.tests.ToeicTest;
import com.engcoach.tests.ToeicTestRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ToeicTestRepository toeicTestRepository;
    private final TestPartRepository testPartRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionGroupRepository questionGroupRepository;

    @Autowired
    public QuestionBankSeeder(QuestionRepository questionRepository, ObjectMapper objectMapper,
                              ResourceLoader resourceLoader, ToeicTestRepository toeicTestRepository,
                              TestPartRepository testPartRepository, TestQuestionRepository testQuestionRepository,
                              QuestionGroupRepository questionGroupRepository) {
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.toeicTestRepository = toeicTestRepository;
        this.testPartRepository = testPartRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.questionGroupRepository = questionGroupRepository;
    }

    // Retained for focused seeder unit tests that only verify question-row validation.
    public QuestionBankSeeder(QuestionRepository questionRepository, ObjectMapper objectMapper,
                              ResourceLoader resourceLoader) {
        this.questionRepository = questionRepository;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.toeicTestRepository = null;
        this.testPartRepository = null;
        this.testQuestionRepository = null;
        this.questionGroupRepository = null;
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
            List<Question> savedQuestions = questionRepository.saveAll(questions);
            if (toeicTestRepository != null) {
                createFirstTest(savedQuestions);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the sample question bank from " + RESOURCE_PATH, e);
        }
    }

    private void createFirstTest(List<Question> questions) {
        ToeicTest test = toeicTestRepository.save(new ToeicTest(
                "TOEIC Practice Test 1",
                "200 câu luyện tập đủ 7 Part. Có thể làm toàn bộ đề hoặc chọn các Part muốn luyện.",
                true));

        int[] durations = {180, 540, 1020, 960, 600, 600, 3300};
        for (int i = 1; i <= 7; i++) {
            String part = "Part " + i;
            int count = (int) questions.stream().filter(q -> part.equals(q.getPart())).count();
            if (count > 0) {
                testPartRepository.save(new TestPart(test.getId(), part, count, durations[i - 1], i));
            }
        }

        java.util.Map<String, QuestionGroup> groups = new java.util.HashMap<>();
        List<TestQuestion> links = new ArrayList<>();
        int position = 1;
        for (Question question : questions) {
            String part = question.getPart();
            String groupType = null;
            String content = null;
            String key = null;
            if ("Part 1".equals(part)) {
                groupType = "PHOTO";
                content = question.getAudioText();
                key = part + ":" + question.getId();
            } else if ("Part 2".equals(part)) {
                groupType = "LISTENING_ITEM";
                content = question.getAudioText();
                key = part + ":" + question.getId();
            } else if ("Part 3".equals(part)) {
                groupType = "CONVERSATION";
                content = question.getAudioText();
                key = part + ":" + content;
            } else if ("Part 4".equals(part)) {
                groupType = "TALK";
                content = question.getAudioText();
                key = part + ":" + content;
            } else if ("Part 6".equals(part) || "Part 7".equals(part)) {
                groupType = "PASSAGE";
                content = question.getPassage();
                key = part + ":" + content;
            }

            Long groupId = null;
            if (key != null) {
                QuestionGroup group = groups.get(key);
                if (group == null) {
                    group = questionGroupRepository.save(new QuestionGroup(
                            part, groupType, content, question.getImageUrl(), question.getAudioUrl()));
                    groups.put(key, group);
                }
                groupId = group.getId();
            }
            links.add(new TestQuestion(test.getId(), question.getId(), groupId, position++));
        }
        testQuestionRepository.saveAll(links);
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
