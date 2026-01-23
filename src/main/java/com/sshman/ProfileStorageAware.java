package com.sshman;

/**
 * Marker interface for commands that use ProfileStorage.
 * Commands implementing this interface will have their
 * ProfileStorageProvider injected by SshManFactory.
 */
public interface ProfileStorageAware {

    /**
     * Sets the ProfileStorageProvider to use for obtaining storage instances.
     */
    void setProfileStorageProvider(ProfileStorageProvider provider);
}
