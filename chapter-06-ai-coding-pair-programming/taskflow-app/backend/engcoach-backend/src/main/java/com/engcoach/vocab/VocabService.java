package com.engcoach.vocab;

import com.engcoach.common.VocabWordAlreadyExistsException;
import com.engcoach.common.VocabWordNotFoundException;
import com.engcoach.vocab.VocabDtos.ReviewDeckResponse;
import com.engcoach.vocab.VocabDtos.VocabListResponse;
import com.engcoach.vocab.VocabDtos.WordRequest;
import com.engcoach.vocab.VocabDtos.WordResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** A learner's vocabulary notebook and flashcard review. */
@Service
public class VocabService {

    static final int MAX_DECK_SIZE = 50;
    static final int DEFAULT_DECK_SIZE = 20;

    private final VocabWordRepository repository;

    public VocabService(VocabWordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public VocabListResponse list(Long userId) {
        List<WordResponse> words = repository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(WordResponse::from).toList();
        return new VocabListResponse(words.size(), repository.countByUserIdAndLearned(userId, true),
                repository.countByUserIdAndLearned(userId, false), repository.countByUserIdAndMarkedTrue(userId), words);
    }

    @Transactional
    public WordResponse add(Long userId, WordRequest request) {
        if (repository.existsByUserIdAndWordKey(userId, VocabWord.keyOf(request.word()))) {
            throw new VocabWordAlreadyExistsException();
        }
        Instant now = Instant.now();
        VocabWord saved = repository.save(new VocabWord(userId, request.word(), request.meaning(),
                request.example(), request.questionId(), now));
        return WordResponse.from(saved);
    }

    @Transactional
    public WordResponse edit(Long userId, Long id, WordRequest request) {
        VocabWord word = repository.findByIdAndUserId(id, userId).orElseThrow(VocabWordNotFoundException::new);
        repository.findByUserIdAndWordKey(userId, VocabWord.keyOf(request.word()))
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new VocabWordAlreadyExistsException();
                });
        word.edit(request.word(), request.meaning(), request.example());
        return WordResponse.from(repository.save(word));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(repository.findByIdAndUserId(id, userId).orElseThrow(VocabWordNotFoundException::new));
    }

    /** Which words a flashcard session covers. */
    public enum Scope {
        ALL, LEARNING, LEARNED, MARKED;

        /** Unknown or missing names mean every word. */
        public static Scope parse(String name) {
            if (name == null) return ALL;
            for (Scope scope : values()) {
                if (scope.name().equalsIgnoreCase(name.trim())) return scope;
            }
            return ALL;
        }
    }

    /** Flashcards can be studied at any time: the chosen group of words, the ones reviewed longest ago first. */
    @Transactional(readOnly = true)
    public ReviewDeckResponse deck(Long userId, Scope scope, Integer limit) {
        int size = limit == null ? DEFAULT_DECK_SIZE : Math.max(1, Math.min(limit, MAX_DECK_SIZE));
        PageRequest page = PageRequest.of(0, size);
        List<VocabWord> words;
        long total;
        switch (scope) {
            case LEARNING -> {
                words = repository.findByUserIdAndLearnedOrderByNextReviewAtAscIdAsc(userId, false, page);
                total = repository.countByUserIdAndLearned(userId, false);
            }
            case LEARNED -> {
                words = repository.findByUserIdAndLearnedOrderByNextReviewAtAscIdAsc(userId, true, page);
                total = repository.countByUserIdAndLearned(userId, true);
            }
            case MARKED -> {
                words = repository.findByUserIdAndMarkedTrueOrderByNextReviewAtAscIdAsc(userId, page);
                total = repository.countByUserIdAndMarkedTrue(userId);
            }
            default -> {
                words = repository.findByUserIdOrderByNextReviewAtAscIdAsc(userId, page);
                total = repository.countByUserId(userId);
            }
        }
        return new ReviewDeckResponse(total, words.stream().map(WordResponse::from).toList());
    }

    /** Stars a word for flashcards and/or sets whether the learner knows it. */
    @Transactional
    public WordResponse flag(Long userId, Long id, Boolean learned, Boolean marked) {
        VocabWord word = repository.findByIdAndUserId(id, userId).orElseThrow(VocabWordNotFoundException::new);
        word.flag(learned, marked);
        return WordResponse.from(repository.save(word));
    }

    /** Records the answer on a card. Remembering it counts as learned, forgetting it as still learning. */
    @Transactional
    public WordResponse review(Long userId, Long id, boolean remembered) {
        VocabWord word = repository.findByIdAndUserId(id, userId).orElseThrow(VocabWordNotFoundException::new);
        word.review(remembered, Instant.now());
        return WordResponse.from(repository.save(word));
    }
}
