package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "archive",
    description = "Archive an SSH key by moving it to ~/.ssh/archived/",
    mixinStandardHelpOptions = true,
    footer = "%nExamples:%n" +
        "  sshman archive id_rsa%n" +
        "  sshman archive work/id_work_ed25519%n" +
        "  sshman archive id_rsa --force%n"
)
public class ArchiveCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveCommand.class);

    @Spec
    private CommandSpec spec;

    @Parameters(
        index = "0",
        description = "Key name to archive (filename in ~/.ssh)",
        completionCandidates = ArchiveKeyCompletionCandidates.class
    )
    private String keyName;

    @Option(names = {"--path"}, description = "Custom SSH directory path")
    private String customPath;

    @Option(names = {"-f", "--force"}, description = "Skip confirmation prompt when key is in use")
    private boolean force;

    // Private key header bytes: "-----BEGIN"
    private static final byte[] PRIVATE_KEY_MAGIC = "-----BEGIN".getBytes();

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        out.println("Equivalent SSH command: none");
        out.println();

        Path sshDir = getSshDirectory();
        Path archiveDir = sshDir.resolve("archived");

        // Resolve the key path
        Path keyPath = sshDir.resolve(keyName);

        // Check if key exists
        if (!Files.exists(keyPath)) {
            // Try to find the key recursively (excluding archived directory)
            Path foundKey = findKeyRecursive(sshDir, keyName);
            if (foundKey != null) {
                keyPath = foundKey;
                // Update keyName to the relative path
                keyName = sshDir.relativize(foundKey).toString();
            } else {
                err.println("Key not found: " + keyName);
                err.println();
                err.println("Available keys:");
                listAvailableKeys(sshDir, err);
                logger.error("Key not found: {}", keyName);
                return 1;
            }
        }

        // Check if the key is already in the archived directory
        if (keyPath.startsWith(archiveDir)) {
            err.println("Key is already archived: " + keyName);
            logger.error("Key already archived: {}", keyName);
            return 1;
        }

        // Verify it's a private key
        if (!isPrivateKey(keyPath)) {
            err.println("Not a valid private key: " + keyPath);
            logger.error("Not a valid private key: {}", keyName);
            return 1;
        }

        // Check if key is used in SSH config
        Path configPath = sshDir.resolve("config");
        Set<String> affectedHosts = new HashSet<>();
        if (Files.exists(configPath)) {
            affectedHosts = findHostsUsingKey(configPath, keyPath);

            if (!affectedHosts.isEmpty() && !force) {
                err.println("Warning: This key is currently used in SSH config for:");
                for (String host : affectedHosts) {
                    err.println("  - " + host);
                }
                err.println();

                // Prompt for confirmation
                if (!confirmArchive(err)) {
                    err.println("Archive cancelled.");
                    logger.info("Archive cancelled by user: {}", keyName);
                    return 1;
                }
                logger.warn("Archiving key in use by {} host(s): {}", affectedHosts.size(), keyName);
            }
        }

        // Create archive directory if it doesn't exist
        try {
            Files.createDirectories(archiveDir);
        } catch (IOException e) {
            err.println("Failed to create archive directory: " + e.getMessage());
            logger.error("Failed to create archive directory: {}", e.getMessage());
            return 1;
        }

        logger.info("Archiving key: {}", keyName);

        // Archive the key and its public key if it exists
        return archiveKey(keyPath, archiveDir, affectedHosts, out, err);
    }

    private Path getSshDirectory() {
        if (customPath != null && !customPath.isEmpty()) {
            return Path.of(customPath);
        }
        return Path.of(System.getProperty("user.home"), ".ssh");
    }

    /**
     * Recursively search for a key by filename in the SSH directory.
     * Excludes the archived directory from search.
     */
    private Path findKeyRecursive(Path baseDir, String keyName) {
        Path archiveDir = baseDir.resolve("archived");
        try (Stream<Path> stream = Files.walk(baseDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> !p.startsWith(archiveDir))  // Exclude archived directory
                .filter(p -> p.getFileName().toString().equals(keyName))
                .filter(this::isPrivateKey)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isPrivateKey(Path path) {
        if (path.getFileName().toString().endsWith(".pub")) {
            return false;
        }

        try (InputStream is = Files.newInputStream(path)) {
            byte[] header = new byte[32];
            int bytesRead = is.read(header);

            if (bytesRead < PRIVATE_KEY_MAGIC.length) {
                return false;
            }

            // Check if starts with "-----BEGIN"
            for (int i = 0; i < PRIVATE_KEY_MAGIC.length; i++) {
                if (header[i] != PRIVATE_KEY_MAGIC[i]) {
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private int archiveKey(Path keyPath, Path archiveDir, Set<String> affectedHosts, PrintWriter out, PrintWriter err) {
        Path sshDir = getSshDirectory();
        Path pubKeyPath = Path.of(keyPath.toString() + ".pub");

        // Calculate relative path from SSH directory
        Path relativeKeyPath = sshDir.relativize(keyPath);

        // Destination paths in archive directory
        Path archivedKeyPath = archiveDir.resolve(relativeKeyPath);
        Path archivedPubKeyPath = Path.of(archivedKeyPath.toString() + ".pub");

        try {
            // Create parent directories in archive if needed
            Path archivedKeyParent = archivedKeyPath.getParent();
            if (archivedKeyParent != null) {
                Files.createDirectories(archivedKeyParent);
            }

            // Move the private key
            Files.move(keyPath, archivedKeyPath, StandardCopyOption.REPLACE_EXISTING);
            out.println("Archived: " + relativeKeyPath + " -> archived/" + relativeKeyPath);

            // Move the public key if it exists
            if (Files.exists(pubKeyPath)) {
                Files.move(pubKeyPath, archivedPubKeyPath, StandardCopyOption.REPLACE_EXISTING);
                out.println("Archived: " + relativeKeyPath + ".pub -> archived/" + relativeKeyPath + ".pub");
            }

            // Clean up empty directories left behind
            cleanupEmptyDirectories(keyPath.getParent(), sshDir);

            out.println();
            out.println("Key successfully archived to: " + sshDir.relativize(archivedKeyPath));

            // Show warning if key is still referenced in config
            if (!affectedHosts.isEmpty()) {
                out.println();
                out.println("Warning: Archived key is still referenced in ~/.ssh/config for hosts:");
                for (String host : affectedHosts) {
                    out.println("  - " + host);
                }
            }

            logger.info("Archived key to: archived/{}", relativeKeyPath);
            return 0;

        } catch (IOException e) {
            err.println("Failed to archive key: " + e.getMessage());
            logger.error("Failed to archive key: {}", e.getMessage());
            return 1;
        }
    }

    /**
     * Parse SSH config file and find hosts that use the specified key.
     * Returns a set of host names that reference the key in their IdentityFile directive.
     */
    private Set<String> findHostsUsingKey(Path configPath, Path keyPath) {
        Set<String> affectedHosts = new HashSet<>();

        try {
            // Get canonical path for comparison (handles symlinks, ~ expansion)
            Path canonicalKeyPath = keyPath.toRealPath();
            Path sshDir = getSshDirectory();
            String userHome = System.getProperty("user.home");

            String currentHost = null;
            List<String> lines = Files.readAllLines(configPath);

            for (String line : lines) {
                String trimmed = line.trim();

                // Skip comments and empty lines
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                // Parse Host directive
                if (trimmed.toLowerCase().startsWith("host ")) {
                    currentHost = trimmed.substring(5).trim();
                    continue;
                }

                // Parse IdentityFile directive
                if (currentHost != null && trimmed.toLowerCase().startsWith("identityfile ")) {
                    String identityFile = trimmed.substring(13).trim();

                    // Remove quotes if present
                    if (identityFile.startsWith("\"") && identityFile.endsWith("\"")) {
                        identityFile = identityFile.substring(1, identityFile.length() - 1);
                    }

                    // Expand ~ to user home
                    if (identityFile.startsWith("~/")) {
                        identityFile = userHome + identityFile.substring(1);
                    }

                    // Resolve to absolute path
                    Path identityPath = Path.of(identityFile);
                    if (!identityPath.isAbsolute()) {
                        identityPath = sshDir.resolve(identityFile);
                    }

                    // Compare canonical paths
                    try {
                        if (identityPath.toRealPath().equals(canonicalKeyPath)) {
                            affectedHosts.add(currentHost);
                        }
                    } catch (IOException e) {
                        // File doesn't exist or can't be resolved, try string comparison
                        if (identityPath.normalize().equals(keyPath.normalize())) {
                            affectedHosts.add(currentHost);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // If we can't read the config, return empty set
        }

        return affectedHosts;
    }

    /**
     * Prompt user for confirmation to archive the key.
     * Returns true if user confirms, false otherwise.
     */
    private boolean confirmArchive(PrintWriter err) {
        Console console = System.console();

        if (console != null) {
            // Interactive terminal available
            String response = console.readLine("Archive this key anyway? (y/N): ");
            return response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"));
        } else {
            // Non-interactive or redirected input - try reading from stdin
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                err.print("Archive this key anyway? (y/N): ");
                err.flush();
                String response = reader.readLine();
                return response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"));
            } catch (IOException e) {
                // Can't read input, default to no
                return false;
            }
        }
    }

    /**
     * Recursively remove empty directories up to the SSH base directory.
     */
    private void cleanupEmptyDirectories(Path dir, Path sshDir) {
        if (dir == null || dir.equals(sshDir) || !dir.startsWith(sshDir)) {
            return;
        }

        try {
            // Only delete if directory is empty
            if (Files.isDirectory(dir)) {
                try (Stream<Path> entries = Files.list(dir)) {
                    if (entries.findAny().isEmpty()) {
                        Files.delete(dir);
                        // Recursively cleanup parent directories
                        cleanupEmptyDirectories(dir.getParent(), sshDir);
                    }
                }
            }
        } catch (IOException e) {
            // Silently ignore cleanup errors
        }
    }

    private void listAvailableKeys(Path sshDir, PrintWriter err) {
        Path archiveDir = sshDir.resolve("archived");
        try (Stream<Path> files = Files.walk(sshDir)) {
            files.filter(Files::isRegularFile)
                .filter(p -> !p.startsWith(archiveDir))  // Exclude archived keys
                .filter(p -> !p.getFileName().toString().endsWith(".pub"))
                .filter(p -> !p.getFileName().toString().equals("config"))
                .filter(p -> !p.getFileName().toString().equals("known_hosts"))
                .filter(p -> !p.getFileName().toString().equals("authorized_keys"))
                .filter(this::isPrivateKey)
                .sorted()
                .forEach(p -> {
                    Path relativePath = sshDir.relativize(p);
                    err.println("  - " + relativePath);
                });
        } catch (IOException e) {
            err.println("  (error listing keys)");
        }
    }

    /**
     * Custom completion candidates for key names.
     * This enables tab-completion for SSH keys, excluding archived keys.
     */
    public static class ArchiveKeyCompletionCandidates implements Iterable<String> {
        @Override
        public java.util.Iterator<String> iterator() {
            List<String> keys = new ArrayList<>();
            Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
            Path archiveDir = sshDir.resolve("archived");

            if (!Files.exists(sshDir) || !Files.isDirectory(sshDir)) {
                return keys.iterator();
            }

            try (Stream<Path> files = Files.walk(sshDir)) {
                files.filter(Files::isRegularFile)
                    .filter(p -> !p.startsWith(archiveDir))  // Exclude archived directory
                    .filter(p -> !p.getFileName().toString().endsWith(".pub"))
                    .filter(p -> !p.getFileName().toString().equals("config"))
                    .filter(p -> !p.getFileName().toString().equals("known_hosts"))
                    .filter(p -> !p.getFileName().toString().equals("authorized_keys"))
                    .filter(ArchiveCommand::isPrivateKeyStatic)
                    .sorted()
                    .forEach(p -> {
                        Path relativePath = sshDir.relativize(p);
                        keys.add(relativePath.toString());
                    });
            } catch (IOException e) {
                // Return empty list on error
            }

            return keys.iterator();
        }
    }

    /**
     * Static version of isPrivateKey for use in completion candidates.
     */
    private static boolean isPrivateKeyStatic(Path path) {
        if (path.getFileName().toString().endsWith(".pub")) {
            return false;
        }

        try (InputStream is = Files.newInputStream(path)) {
            byte[] header = new byte[32];
            int bytesRead = is.read(header);

            if (bytesRead < PRIVATE_KEY_MAGIC.length) {
                return false;
            }

            // Check if starts with "-----BEGIN"
            for (int i = 0; i < PRIVATE_KEY_MAGIC.length; i++) {
                if (header[i] != PRIVATE_KEY_MAGIC[i]) {
                    return false;
                }
            }

            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
