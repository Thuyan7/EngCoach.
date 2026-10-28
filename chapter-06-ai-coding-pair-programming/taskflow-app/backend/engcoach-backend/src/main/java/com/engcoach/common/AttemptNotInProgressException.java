package com.engcoach.common;

/** Thrown when an answer is changed after its attempt has been submitted. */
public class AttemptNotInProgressException extends RuntimeException {

    public AttemptNotInProgressException() {
        super("Answers can only be changed while the attempt is in progress.");
    }
}
