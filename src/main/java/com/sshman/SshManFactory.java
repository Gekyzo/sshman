package com.sshman;

import picocli.CommandLine.IFactory;

/**
 * Custom picocli IFactory implementation that enables dependency injection
 * for ProfileStorage in commands.
 */
public class SshManFactory implements IFactory {

    private final ProfileStorageProvider storageProvider;

    /**
     * Creates a factory with the default ProfileStorageProvider.
     */
    public SshManFactory() {
        this(ProfileStorageProvider.DEFAULT);
    }

    /**
     * Creates a factory with a custom ProfileStorageProvider.
     * Useful for testing with mock storage.
     */
    public SshManFactory(ProfileStorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        K instance = cls.getDeclaredConstructor().newInstance();
        if (instance instanceof ProfileStorageAware aware) {
            aware.setProfileStorageProvider(storageProvider);
        }
        return instance;
    }
}
