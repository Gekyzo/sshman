package com.sshman.exceptions;

/**
 * Exception thrown when the configuration directory cannot be created.
 */
public class ConfigDirectoryException extends SshManException {

    public ConfigDirectoryException(String message) {
        super(message);
    }

    public ConfigDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
