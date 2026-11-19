package com.engcoach.vocab;

import com.engcoach.vocab.VocabDtos.ReviewDeckResponse;
import com.engcoach.vocab.VocabDtos.ReviewRequest;
import com.engcoach.vocab.VocabDtos.VocabListResponse;
import com.engcoach.vocab.VocabDtos.WordRequest;
import com.engcoach.vocab.VocabDtos.WordResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vocab")
public class VocabController {

    private final VocabService vocabService;
    private final WordLookupSource lookupSource;

    public VocabController(VocabService vocabService, WordLookupSource lookupSource) {
        this.vocabService = vocabService;
        this.lookupSource = lookupSource;
    }

    private Long userId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }

    @GetMapping
    public ResponseEntity<VocabListResponse> list(Authentication authentication) {
        return ResponseEntity.ok(vocabService.list(userId(authentication)));
    }

    @PostMapping
    public ResponseEntity<WordResponse> add(@Valid @RequestBody WordRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vocabService.add(userId(authentication), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WordResponse> edit(@PathVariable Long id, @Valid @RequestBody WordRequest request,
                                             Authentication authentication) {
        return ResponseEntity.ok(vocabService.edit(userId(authentication), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        vocabService.delete(userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    /** Suggests a Vietnamese meaning for a selected word, so the learner can save it without typing. */
    @GetMapping("/lookup")
    public ResponseEntity<WordLookup> lookup(@RequestParam String word,
                                             @RequestParam(required = false) String context) {
        String trimmed = word.trim();
        if (trimmed.isEmpty() || trimmed.length() > 100) {
            return ResponseEntity.ok(WordLookup.empty());
        }
        String sentence = context == null ? null : context.substring(0, Math.min(context.length(), 500));
        return ResponseEntity.ok(lookupSource.find(trimmed, sentence));
    }

    /** The flashcards due now (default 20, at most 50). */
    @GetMapping("/review")
    public ResponseEntity<ReviewDeckResponse> deck(@RequestParam(required = false) Integer limit,
                                                   Authentication authentication) {
        return ResponseEntity.ok(vocabService.deck(userId(authentication), limit));
    }

    /** Record whether the learner remembered a card; moves it between boxes and schedules the next review. */
    @PostMapping("/{id}/review")
    public ResponseEntity<WordResponse> review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request,
                                               Authentication authentication) {
        return ResponseEntity.ok(vocabService.review(userId(authentication), id, request.remembered()));
    }
}
