package com.sshman;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

/**
 * Metadata associated with an SSH key, stored in a companion .meta file.
 *
 * <p>Example: For key {@code ~/.ssh/id_ed25519}, metadata is stored in
 * {@code ~/.ssh/id_ed25519.meta}</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create metadata from use path
 * KeyMetadata meta = KeyMetadata.create("work/project-a", "My project key");
 *
 * // Save to file
 * meta.save(Path.of("/home/user/.ssh/id_ed25519"));
 *
 * // Load from file
 * Optional<KeyMetadata> loaded = KeyMetadata.load(Path.of("/home/user/.ssh/id_ed25519"));
 * }</pre>
 *
 * <h2>Use Path Parsing</h2>
 * <ul>
 *     <li>{@code "work"} → use=WORK, project=null</li>
 *     <li>{@code "personal"} → use=PERSONAL, project=null</li>
 *     <li>{@code "work/project-a"} → use=WORK, project="project-a"</li>
 *     <li>{@code "work/client/acme"} → use=WORK, project="client/acme"</li>
 *     <li>{@code "other/something"} → use=OTHER, project="something"</li>
 * </ul>
 *
 * @author sshman
 * @version 1.0
 * @since 1.0
 */
public record KeyMetadata(
    KeyUse use,
    String project,
    String description,
    Instant createdAt,
    String createdBy
) {

    // ========================================================================
    // Constants
    // ========================================================================

    private static final String META_EXTENSION = ".meta";

    private static final String KEY_USE = "use";
    private static final String KEY_PROJECT = "project";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_CREATED_BY = "created_by";

    // ========================================================================
    // Static Factory Methods
    // ========================================================================

    /**
     * Creates a new KeyMetadata by parsing a use path.
     *
     * <p>Examples:</p>
     * <ul>
     *     <li>{@code "work"} → use=WORK, project=null</li>
     *     <li>{@code "personal"} → use=PERSONAL, project=null</li>
     *     <li>{@code "work/project-a"} → use=WORK, project="project-a"</li>
     *     <li>{@code "work/client/acme"} → use=WORK, project="client/acme"</li>
     * </ul>
     *
     * @param usePath the use path (e.g., "work/project-a")
     * @return new KeyMetadata instance
     */
    public static KeyMetadata create(String usePath) {
        return create(usePath, null);
    }

    /**
     * Creates a new KeyMetadata by parsing a use path with description.
     *
     * @param usePath     the use path (e.g., "work/project-a"), can be null
     * @param description optional description
     * @return new KeyMetadata instance
     */
    public static KeyMetadata create(String usePath, String description) {
        ParsedUse parsed = parseUsePath(usePath);
        return new KeyMetadata(
            parsed.use(),
            parsed.project(),
            description,
            Instant.now(),
            System.getProperty("user.name")
        );
    }

    /**
     * Creates a new KeyMetadata with explicit use and project.
     *
     * @param use         the key use category
     * @param project     optional project/subfolder
     * @param description optional description
     * @return new KeyMetadata instance
     */
    public static KeyMetadata create(KeyUse use, String project, String description) {
        return new KeyMetadata(
            use != null ? use : KeyUse.OTHER,
            project,
            description,
            Instant.now(),
            System.getProperty("user.name")
        );
    }

    /**
     * Creates a new KeyMetadata with just the use category.
     *
     * @param use the key use category
     * @return new KeyMetadata instance
     */
    public static KeyMetadata create(KeyUse use) {
        return create(use, null, null);
    }

    // ========================================================================
    // Load / Save Methods
    // ========================================================================

    /**
     * Loads metadata for a key from its companion .meta file.
     *
     * @param keyPath path to the private key file
     * @return Optional containing the metadata, or empty if not found or on error
     */
    public static Optional<KeyMetadata> load(Path keyPath) {
        Path metaPath = getMetaPath(keyPath);

        if (!Files.exists(metaPath)) {
            return Optional.empty();
        }

        try (var reader = Files.newBufferedReader(metaPath)) {
            Properties props = new Properties();
            props.load(reader);

            KeyUse use = KeyUse.fromString(props.getProperty(KEY_USE));
            String project = props.getProperty(KEY_PROJECT);
            String description = props.getProperty(KEY_DESCRIPTION);

            Instant createdAt = parseInstant(props.getProperty(KEY_CREATED_AT));
            String createdBy = props.getProperty(KEY_CREATED_BY);

            return Optional.of(new KeyMetadata(use, project, description, createdAt, createdBy));

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Saves this metadata to a companion .meta file.
     *
     * @param keyPath path to the private key file
     * @throws IOException if writing fails
     */
    public void save(Path keyPath) throws IOException {
        Path metaPath = getMetaPath(keyPath);

        Properties props = new Properties();

        // Always save use
        props.setProperty(KEY_USE, use.getValue());

        // Optional fields
        if (hasProject()) {
            props.setProperty(KEY_PROJECT, project);
        }

        if (hasDescription()) {
            props.setProperty(KEY_DESCRIPTION, description);
        }

        if (createdAt != null) {
            props.setProperty(KEY_CREATED_AT, createdAt.toString());
        }

        if (createdBy != null && !createdBy.isBlank()) {
            props.setProperty(KEY_CREATED_BY, createdBy);
        }

        try (var writer = Files.newBufferedWriter(metaPath)) {
            props.store(writer, "SSH Key Metadata - Generated by sshman");
        }
    }

    /**
     * Deletes the metadata file for a key.
     *
     * @param keyPath path to the private key file
     * @return true if deleted, false otherwise
     */
    public static boolean delete(Path keyPath) {
        try {
            return Files.deleteIfExists(getMetaPath(keyPath));
        } catch (IOException e) {
            return false;
        }
    }

    // ========================================================================
    // Query Methods
    // ========================================================================

    /**
     * Checks if metadata exists for a key.
     *
     * @param keyPath path to the private key file
     * @return true if metadata file exists
     */
    public static boolean exists(Path keyPath) {
        return Files.exists(getMetaPath(keyPath));
    }

    /**
     * Gets the path to the metadata file for a key.
     *
     * @param keyPath path to the private key file
     * @return path to the .meta file
     */
    public static Path getMetaPath(Path keyPath) {
        return Path.of(keyPath.toString() + META_EXTENSION);
    }

    // ========================================================================
    // Instance Helper Methods
    // ========================================================================

    /**
     * Checks if this metadata has a project defined.
     *
     * @return true if project is present and not blank
     */
    public boolean hasProject() {
        return project != null && !project.isBlank();
    }

    /**
     * Checks if this metadata has a description defined.
     *
     * @return true if description is present and not blank
     */
    public boolean hasDescription() {
        return description != null && !description.isBlank();
    }

    /**
     * Checks if this metadata has creation info.
     *
     * @return true if createdAt is present
     */
    public boolean hasCreatedAt() {
        return createdAt != null;
    }

    /**
     * Checks if this metadata has creator info.
     *
     * @return true if createdBy is present and not blank
     */
    public boolean hasCreatedBy() {
        return createdBy != null && !createdBy.isBlank();
    }

    /**
     * Returns the full use path (e.g., "work/project-a").
     *
     * @return the full use path
     */
    public String getFullUsePath() {
        if (hasProject()) {
            return use.getValue() + "/" + project;
        }
        return use.getValue();
    }

    /**
     * Creates a copy of this metadata with a new description.
     *
     * @param newDescription the new description
     * @return new KeyMetadata instance
     */
    public KeyMetadata withDescription(String newDescription) {
        return new KeyMetadata(use, project, newDescription, createdAt, createdBy);
    }

    /**
     * Creates a copy of this metadata with a new use.
     *
     * @param newUse the new use category
     * @return new KeyMetadata instance
     */
    public KeyMetadata withUse(KeyUse newUse) {
        return new KeyMetadata(newUse, project, description, createdAt, createdBy);
    }

    /**
     * Creates a copy of this metadata with a new project.
     *
     * @param newProject the new project
     * @return new KeyMetadata instance
     */
    public KeyMetadata withProject(String newProject) {
        return new KeyMetadata(use, newProject, description, createdAt, createdBy);
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Parses a use path into use category and project.
     *
     * <p>Examples:</p>
     * <ul>
     *     <li>{@code null} → use=OTHER, project=null</li>
     *     <li>{@code ""} → use=OTHER, project=null</li>
     *     <li>{@code "work"} → use=WORK, project=null</li>
     *     <li>{@code "work/project-a"} → use=WORK, project="project-a"</li>
     *     <li>{@code "work/client/acme"} → use=WORK, project="client/acme"</li>
     *     <li>{@code "unknown/stuff"} → use=OTHER, project="unknown/stuff"</li>
     * </ul>
     *
     * @param usePath the use path (e.g., "work/project-a")
     * @return parsed use and project
     */
    private static ParsedUse parseUsePath(String usePath) {
        if (usePath == null || usePath.isBlank()) {
            return new ParsedUse(KeyUse.OTHER, null);
        }

        String normalized = usePath.trim();

        // Check if it starts with a known use category
        for (KeyUse use : KeyUse.values()) {
            if (use == KeyUse.OTHER) {
                continue; // Skip OTHER, handle it at the end
            }

            String prefix = use.getValue();

            if (normalized.equalsIgnoreCase(prefix)) {
                // Exact match: "work" or "personal"
                return new ParsedUse(use, null);
            }

            if (normalized.toLowerCase().startsWith(prefix.toLowerCase() + "/")) {
                // Has subfolder: "work/project-a"
                String project = normalized.substring(prefix.length() + 1);
                return new ParsedUse(use, project.isBlank() ? null : project);
            }
        }

        // Unknown use category - treat the whole thing as "other" with project
        return new ParsedUse(KeyUse.OTHER, normalized);
    }

    /**
     * Parses an Instant from a string, returning null on failure.
     *
     * @param value the string value
     * @return the parsed Instant or null
     */
    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    // ========================================================================
    // Inner Classes
    // ========================================================================

    /**
     * Internal record for parsed use path.
     */
    private record ParsedUse(KeyUse use, String project) {
    }
}
