package com.engcoach.common;

/**
 * Invalid attempt type or Part selection supplied by the client.
 */
public class InvalidAttemptRequestException extends RuntimeException {

    public InvalidAttemptRequestException(String message) {
        super(message);
    }
}
