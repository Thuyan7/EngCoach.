package com.engcoach.common;

/** Raised when a timed mock-test attempt has reached its server-calculated deadline. */
public class AttemptTimeExpiredException extends RuntimeException {

    public AttemptTimeExpiredException() {
        super("The mock-test time limit has expired. No further answers can be saved.");
    }
}
