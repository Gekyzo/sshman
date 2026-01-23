package com.sshman.exceptions;

/**
 * Base exception class for all sshman-specific exceptions.
 */
public class SshManException extends RuntimeException {

    public SshManException(String message) {
        super(message);
    }

    public SshManException(String message, Throwable cause) {
        super(message, cause);
    }
}
