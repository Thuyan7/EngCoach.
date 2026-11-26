package com.engcoach.common;

/** The learner already saved this word (ignoring case). Maps to 409. */
public class VocabWordAlreadyExistsException extends RuntimeException {

    public VocabWordAlreadyExistsException() {
        super("You have already saved this word.");
    }
}
