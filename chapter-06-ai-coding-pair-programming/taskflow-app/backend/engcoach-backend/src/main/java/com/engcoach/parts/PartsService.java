package com.engcoach.parts;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PartsService {

    private final QuestionRepository questionRepository;

    public PartsService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public List<String> listParts() {
        return questionRepository.findDistinctParts();
    }
}
