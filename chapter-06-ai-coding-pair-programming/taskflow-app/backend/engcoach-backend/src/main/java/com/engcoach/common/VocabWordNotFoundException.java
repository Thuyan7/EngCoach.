package com.engcoach.common;

/** A vocabulary word id that does not exist or belongs to another learner. Maps to 404. */
public class VocabWordNotFoundException extends RuntimeException {

    public VocabWordNotFoundException() {
        super("Vocabulary word not found.");
    }
}
