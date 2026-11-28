package com.engcoach.common;

/** Thrown when POST /attempts names an attempt type the service does not support. */
public class UnsupportedAttemptTypeException extends RuntimeException {

    public UnsupportedAttemptTypeException() {
        super("Unsupported attempt type.");
    }
}
