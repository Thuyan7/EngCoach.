package com.engcoach.common;

/** Thrown when an answer targets a question outside the attempt or an invalid option. */
public class InvalidAnswerException extends RuntimeException {

    public InvalidAnswerException() {
        super("The question or chosen option is not valid for this attempt.");
    }
}
