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

    /** Either flag may be left out to keep its current value. */
    public record FlagsRequest(Boolean learned, Boolean marked) {
    }

    public record WordResponse(Long id, String word, String meaning, String example, Long questionId,
                               int box, Instant nextReviewAt, int reviewCount, Instant createdAt,
                               boolean learned, boolean marked) {

        static WordResponse from(VocabWord word) {
            return new WordResponse(word.getId(), word.getWord(), word.getMeaning(), word.getExample(),
                    word.getQuestionId(), word.getBox(), word.getNextReviewAt(), word.getReviewCount(),
                    word.getCreatedAt(), word.isLearned(), word.isMarked());
        }
    }

    /** All saved words (newest first) with how many are learned, still being learned and starred. */
    public record VocabListResponse(int total, long learnedCount, long learningCount, long markedCount,
                                    List<WordResponse> words) {
    }

    /** The cards to study, at most the requested limit; {@code total} counts every word in the chosen group. */
    public record ReviewDeckResponse(long total, List<WordResponse> cards) {
    }
}
