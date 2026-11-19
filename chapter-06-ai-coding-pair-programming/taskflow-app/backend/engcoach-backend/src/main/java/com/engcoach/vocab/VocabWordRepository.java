package com.engcoach.vocab;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VocabWordRepository extends JpaRepository<VocabWord, Long> {

    List<VocabWord> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<VocabWord> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndWordKey(Long userId, String wordKey);

    Optional<VocabWord> findByUserIdAndWordKey(Long userId, String wordKey);

    long countByUserIdAndNextReviewAtLessThanEqual(Long userId, Instant now);

    List<VocabWord> findByUserIdAndNextReviewAtLessThanEqualOrderByNextReviewAtAscIdAsc(
            Long userId, Instant now, Pageable pageable);
}
