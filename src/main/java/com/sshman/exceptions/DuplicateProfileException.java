package com.sshman.exceptions;

/**
 * Exception thrown when attempting to create a profile with a duplicate alias.
 */
public class DuplicateProfileException extends SshManException {

    private final String alias;

    public DuplicateProfileException(String alias, String profilesFile) {
        super("Profile with alias '" + alias + "' already exists in " + profilesFile);
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }
}
