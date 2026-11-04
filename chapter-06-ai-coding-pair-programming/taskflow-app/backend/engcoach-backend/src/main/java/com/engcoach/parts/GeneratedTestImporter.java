package com.engcoach.parts;

import com.engcoach.tests.QuestionGroup;
import com.engcoach.tests.QuestionGroupRepository;
import com.engcoach.tests.TestPart;
import com.engcoach.tests.TestPartRepository;
import com.engcoach.tests.TestQuestion;
import com.engcoach.tests.TestQuestionRepository;
import com.engcoach.tests.ToeicTest;
import com.engcoach.tests.ToeicTestRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nạp các đề do tools/question-generator sinh ra (classpath:question-bank/generated/test-*.json).
 * Tắt mặc định (engcoach.question-bank.import-generated=true để bật). Đề có tiêu đề đã tồn tại thì bỏ qua,
 * nên chạy lại nhiều lần không tạo đề trùng. Mỗi file được lưu trong một transaction và được kiểm tra
 * toàn bộ trước khi ghi: file lỗi làm ứng dụng báo lỗi rõ ràng thay vì lưu dở dang.
 */
@Component
@ConditionalOnProperty(prefix = "engcoach.question-bank", name = "import-generated", havingValue = "true")
public class GeneratedTestImporter implements CommandLineRunner {

    static final String DEFAULT_PATTERN = "classpath*:question-bank/generated/test-*.json";
    private static final int[] DURATIONS = {180, 540, 1020, 960, 600, 600, 3300};
    private static final Set<String> PARTS_WITH_GROUP = Set.of("Part 1", "Part 2", "Part 3", "Part 4", "Part 6", "Part 7");

    private final QuestionRepository questionRepository;
    private final ToeicTestRepository toeicTestRepository;
    private final TestPartRepository testPartRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resolver;
    private final TransactionTemplate transactionTemplate;
    private final String locationPattern;

    @Autowired
    public GeneratedTestImporter(QuestionRepository questionRepository, ToeicTestRepository toeicTestRepository,
                                 TestPartRepository testPartRepository, TestQuestionRepository testQuestionRepository,
                                 QuestionGroupRepository questionGroupRepository, ObjectMapper objectMapper,
                                 TransactionTemplate transactionTemplate) {
        this(questionRepository, toeicTestRepository, testPartRepository, testQuestionRepository,
                questionGroupRepository, objectMapper, new PathMatchingResourcePatternResolver(),
                transactionTemplate, DEFAULT_PATTERN);
    }

    GeneratedTestImporter(QuestionRepository questionRepository, ToeicTestRepository toeicTestRepository,
                          TestPartRepository testPartRepository, TestQuestionRepository testQuestionRepository,
                          QuestionGroupRepository questionGroupRepository, ObjectMapper objectMapper,
                          ResourcePatternResolver resolver, TransactionTemplate transactionTemplate,
                          String locationPattern) {
        this.questionRepository = questionRepository;
        this.toeicTestRepository = toeicTestRepository;
        this.testPartRepository = testPartRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.questionGroupRepository = questionGroupRepository;
        this.objectMapper = objectMapper;
        this.resolver = resolver;
        this.transactionTemplate = transactionTemplate;
        this.locationPattern = locationPattern;
    }

    @Override
    public void run(String... args) throws IOException {
        Resource[] files = resolver.getResources(locationPattern);
        Arrays.sort(files, Comparator.comparing(r -> String.valueOf(r.getFilename())));
        for (Resource file : files) {
            GeneratedTest test = read(file);
            validate(test, file.getFilename());
            if (toeicTestRepository.existsByTitle(test.title())) {
                continue;
            }
            transactionTemplate.executeWithoutResult(status -> save(test));
        }
    }

    private GeneratedTest read(Resource file) {
        try (InputStream in = file.getInputStream()) {
            return objectMapper.readValue(in, GeneratedTest.class);
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được đề " + file.getFilename(), e);
        }
    }

    void validate(GeneratedTest test, String fileName) {
        if (test == null || isBlank(test.title()) || test.questions() == null || test.questions().isEmpty()) {
            throw invalid(fileName, "cần có title và ít nhất một câu hỏi");
        }
        int index = 0;
        for (GeneratedQuestion q : test.questions()) {
            index++;
            if (q == null || q.part() == null || !q.part().matches("Part [1-7]") || isBlank(q.prompt())
                    || q.options() == null || q.options().size() < 2
                    || q.options().stream().anyMatch(this::isBlank)
                    || q.correctOption() == null || !q.options().contains(q.correctOption())) {
                throw invalid(fileName, "câu thứ " + index + " thiếu part/prompt/options hoặc correctOption không nằm trong options");
            }
            if (q.correctOption().length() > 255) {
                throw invalid(fileName, "câu thứ " + index + ": correctOption dài hơn 255 ký tự");
            }
            if ("Part 1".equals(q.part()) && (isBlank(q.imageUrl()) || isBlank(q.audioText()))) {
                throw invalid(fileName, "câu thứ " + index + " (Part 1) thiếu ảnh hoặc audioText; sinh lại đề với nguồn ảnh Part 1 (Openverse, Pexels hoặc thư mục ảnh riêng)");
            }
            if (List.of("Part 2", "Part 3", "Part 4").contains(q.part()) && isBlank(q.audioText())) {
                throw invalid(fileName, "câu thứ " + index + " thiếu audioText");
            }
            if (List.of("Part 6", "Part 7").contains(q.part()) && isBlank(q.passage())) {
                throw invalid(fileName, "câu thứ " + index + " thiếu passage");
            }
            if (PARTS_WITH_GROUP.contains(q.part()) && isBlank(q.groupKey())) {
                throw invalid(fileName, "câu thứ " + index + " thiếu groupKey");
            }
        }
    }

    private void save(GeneratedTest test) {
        List<Question> questions = new ArrayList<>();
        for (GeneratedQuestion q : test.questions()) {
            try {
                questions.add(new Question(q.part(), q.prompt(), objectMapper.writeValueAsString(q.options()),
                        q.correctOption(), q.passage(), q.imageUrl(), q.audioUrl(), q.audioText()));
            } catch (IOException e) {
                throw new IllegalStateException("Không ghi được options của đề " + test.title(), e);
            }
        }
        List<Question> saved = questionRepository.saveAll(questions);

        ToeicTest toeicTest = toeicTestRepository.save(new ToeicTest(test.title(),
                isBlank(test.description()) ? "Đề luyện tập tạo bằng AI." : test.description(), true));

        for (int i = 1; i <= 7; i++) {
            String part = "Part " + i;
            int count = (int) test.questions().stream().filter(q -> part.equals(q.part())).count();
            if (count > 0) {
                testPartRepository.save(new TestPart(toeicTest.getId(), part, count, DURATIONS[i - 1], i));
            }
        }

        Map<String, QuestionGroup> groups = new HashMap<>();
        List<TestQuestion> links = new ArrayList<>();
        for (int i = 0; i < test.questions().size(); i++) {
            GeneratedQuestion q = test.questions().get(i);
            Long groupId = null;
            if (PARTS_WITH_GROUP.contains(q.part())) {
                String key = q.part() + ":" + q.groupKey();
                QuestionGroup group = groups.get(key);
                if (group == null) {
                    group = questionGroupRepository.save(new QuestionGroup(q.part(), groupType(q.part()),
                            "Part 6".equals(q.part()) || "Part 7".equals(q.part()) ? q.passage() : q.audioText(),
                            q.imageUrl(), q.audioUrl()));
                    groups.put(key, group);
                }
                groupId = group.getId();
            }
            links.add(new TestQuestion(toeicTest.getId(), saved.get(i).getId(), groupId, i + 1));
        }
        testQuestionRepository.saveAll(links);
    }

    private static String groupType(String part) {
        return switch (part) {
            case "Part 1" -> "PHOTO";
            case "Part 2" -> "LISTENING_ITEM";
            case "Part 3" -> "CONVERSATION";
            case "Part 4" -> "TALK";
            default -> "PASSAGE";
        };
    }

    private IllegalStateException invalid(String fileName, String reason) {
        return new IllegalStateException("Đề " + fileName + " không hợp lệ: " + reason);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeneratedTest(String title, String description, List<GeneratedQuestion> questions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeneratedQuestion(String part, String prompt, List<String> options, String correctOption,
                             String passage, String imageUrl, String audioUrl, String audioText, String groupKey) {
    }
}
