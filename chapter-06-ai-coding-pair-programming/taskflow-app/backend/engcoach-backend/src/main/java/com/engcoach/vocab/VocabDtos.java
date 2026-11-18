package com.engcoach.vocab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Request and response bodies of the vocabulary API. */
public final class VocabDtos {

    private VocabDtos() {
    }

    public record WordRequest(
            @NotBlank @Size(max = 100) String word,
            @NotBlank @Size(max = 500) String meaning,
            @Size(max = 500) String example,
            Long questionId) {
    }

    public record ReviewRequest(@NotNull Boolean remembered) {
    }

    public record WordResponse(Long id, String word, String meaning, String example, Long questionId,
                               int box, Instant nextReviewAt, int reviewCount, Instant createdAt, boolean due) {

        static WordResponse from(VocabWord word, Instant now) {
            return new WordResponse(word.getId(), word.getWord(), word.getMeaning(), word.getExample(),
                    word.getQuestionId(), word.getBox(), word.getNextReviewAt(), word.getReviewCount(),
                    word.getCreatedAt(), !word.getNextReviewAt().isAfter(now));
        }
    }

    /** All saved words (newest first) and how many are due for review now. */
    public record VocabListResponse(int total, long dueCount, List<WordResponse> words) {
    }

    /** The cards to study now, at most the requested limit; {@code dueCount} counts all due words. */
    public record ReviewDeckResponse(long dueCount, List<WordResponse> cards) {
    }
}
