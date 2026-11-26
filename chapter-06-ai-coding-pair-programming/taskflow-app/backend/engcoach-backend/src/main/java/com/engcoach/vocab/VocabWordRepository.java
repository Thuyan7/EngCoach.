package com.engcoach.vocab;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VocabWordRepository extends JpaRepository<VocabWord, Long> {

    List<VocabWord> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    Optional<VocabWord> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndWordKey(Long userId, String wordKey);

    Optional<VocabWord> findByUserIdAndWordKey(Long userId, String wordKey);

    long countByUserId(Long userId);

    long countByUserIdAndLearned(Long userId, boolean learned);

    long countByUserIdAndMarkedTrue(Long userId);

    /** Study order: the words that waited longest since their last review come first. */
    List<VocabWord> findByUserIdOrderByNextReviewAtAscIdAsc(Long userId, Pageable pageable);

    List<VocabWord> findByUserIdAndLearnedOrderByNextReviewAtAscIdAsc(Long userId, boolean learned, Pageable pageable);

    List<VocabWord> findByUserIdAndMarkedTrueOrderByNextReviewAtAscIdAsc(Long userId, Pageable pageable);
}
