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
        Instant now = Instant.now();
        List<WordResponse> words = repository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(word -> WordResponse.from(word, now)).toList();
        return new VocabListResponse(words.size(), repository.countByUserIdAndNextReviewAtLessThanEqual(userId, now), words);
    }

    @Transactional
    public WordResponse add(Long userId, WordRequest request) {
        if (repository.existsByUserIdAndWordKey(userId, VocabWord.keyOf(request.word()))) {
            throw new VocabWordAlreadyExistsException();
        }
        Instant now = Instant.now();
        VocabWord saved = repository.save(new VocabWord(userId, request.word(), request.meaning(),
                request.example(), request.questionId(), now));
        return WordResponse.from(saved, now);
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
        return WordResponse.from(repository.save(word), Instant.now());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(repository.findByIdAndUserId(id, userId).orElseThrow(VocabWordNotFoundException::new));
    }

    /** The words due now, the ones waiting longest first. */
    @Transactional(readOnly = true)
    public ReviewDeckResponse deck(Long userId, Integer limit) {
        int size = limit == null ? DEFAULT_DECK_SIZE : Math.max(1, Math.min(limit, MAX_DECK_SIZE));
        Instant now = Instant.now();
        List<WordResponse> cards = repository
                .findByUserIdAndNextReviewAtLessThanEqualOrderByNextReviewAtAscIdAsc(userId, now, PageRequest.of(0, size))
                .stream().map(word -> WordResponse.from(word, now)).toList();
        return new ReviewDeckResponse(repository.countByUserIdAndNextReviewAtLessThanEqual(userId, now), cards);
    }

    @Transactional
    public WordResponse review(Long userId, Long id, boolean remembered) {
        VocabWord word = repository.findByIdAndUserId(id, userId).orElseThrow(VocabWordNotFoundException::new);
        Instant now = Instant.now();
        word.review(remembered, now);
        return WordResponse.from(repository.save(word), now);
    }
}
