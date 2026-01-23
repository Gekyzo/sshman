package com.sshman;

import java.util.function.Supplier;

/**
 * Functional interface for providing ProfileStorage instances.
 * Enables dependency injection for testing and customization.
 */
@FunctionalInterface
public interface ProfileStorageProvider extends Supplier<ProfileStorage> {

    /**
     * Default provider that creates a standard ProfileStorage instance.
     */
    ProfileStorageProvider DEFAULT = ProfileStorage::new;
}
