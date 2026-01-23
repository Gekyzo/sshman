package com.sshman.exceptions;

/**
 * Exception thrown when profile storage operations fail (I/O errors).
 */
public class ProfileStorageException extends SshManException {

    public ProfileStorageException(String message) {
        super(message);
    }

    public ProfileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
