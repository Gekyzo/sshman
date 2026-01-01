package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "unarchive",
    description = "Restore an archived SSH key back to active use",
    mixinStandardHelpOptions = true,
    footer = "%nExamples:%n" +
        "  sshman unarchive id_rsa%n" +
        "  sshman unarchive work/id_work_ed25519%n"
)
public class UnarchiveCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(UnarchiveCommand.class);

    @Spec
    private CommandSpec spec;

    @Parameters(
        index = "0",
        description = "Key name to unarchive (filename in ~/.ssh/archived)",
        completionCandidates = UnarchiveKeyCompletionCandidates.class
    )
    private String keyName;

    @Option(names = {"--path"}, description = "Custom SSH directory path")
    private String customPath;

    @Option(names = {"--force", "-f"}, description = "Overwrite existing key if it already exists")
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

        // Resolve the archived key path
        Path archivedKeyPath = archiveDir.resolve(keyName);

        // Check if archived directory exists
        if (!Files.exists(archiveDir)) {
            err.println("Archive directory does not exist: " + archiveDir);
            err.println("No keys have been archived yet.");
            logger.error("Archive directory does not exist");
            return 1;
        }

        // Check if archived key exists
        if (!Files.exists(archivedKeyPath)) {
            // Try to find the key recursively in archived directory
            Path foundKey = findKeyRecursive(archiveDir, keyName);
            if (foundKey != null) {
                archivedKeyPath = foundKey;
                // Update keyName to the relative path from archive directory
                keyName = archiveDir.relativize(foundKey).toString();
            } else {
                err.println("Archived key not found: " + keyName);
                err.println();
                err.println("Available archived keys:");
                listAvailableArchivedKeys(archiveDir, err);
                logger.error("Archived key not found: {}", keyName);
                return 1;
            }
        }

        // Verify it's a private key
        if (!isPrivateKey(archivedKeyPath)) {
            err.println("Not a valid private key: " + archivedKeyPath);
            logger.error("Not a valid private key: {}", keyName);
            return 1;
        }

        // Calculate target path (restore to original location)
        Path targetKeyPath = sshDir.resolve(keyName);
        Path targetPubKeyPath = Path.of(targetKeyPath.toString() + ".pub");

        // Check if key already exists at target location
        if (Files.exists(targetKeyPath) && !force) {
            err.println("Key already exists at target location: " + keyName);
            err.println("Use --force to overwrite the existing key.");
            logger.error("Key already exists (use --force to overwrite): {}", keyName);
            return 1;
        }

        if (Files.exists(targetKeyPath)) {
            logger.warn("Overwriting existing key: {}", keyName);
        }

        logger.info("Unarchiving key: {}", keyName);

        // Unarchive the key and its public key if it exists
        return unarchiveKey(archivedKeyPath, targetKeyPath, out, err);
    }

    private Path getSshDirectory() {
        if (customPath != null && !customPath.isEmpty()) {
            return Path.of(customPath);
        }
        return Path.of(System.getProperty("user.home"), ".ssh");
    }

    /**
     * Recursively search for a key by filename in the archived directory.
     */
    private Path findKeyRecursive(Path archiveDir, String keyName) {
        try (Stream<Path> stream = Files.walk(archiveDir)) {
            return stream
                .filter(Files::isRegularFile)
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

    private int unarchiveKey(Path archivedKeyPath, Path targetKeyPath, PrintWriter out, PrintWriter err) {
        Path sshDir = getSshDirectory();
        Path archiveDir = sshDir.resolve("archived");
        Path archivedPubKeyPath = Path.of(archivedKeyPath.toString() + ".pub");

        try {
            // Create parent directories for target if needed
            Path targetKeyParent = targetKeyPath.getParent();
            if (targetKeyParent != null && !Files.exists(targetKeyParent)) {
                Files.createDirectories(targetKeyParent);
            }

            // Move the private key
            Files.move(archivedKeyPath, targetKeyPath, StandardCopyOption.REPLACE_EXISTING);
            Path relativeKeyPath = sshDir.relativize(targetKeyPath);
            out.println("Restored: archived/" + keyName + " -> " + relativeKeyPath);

            // Move the public key if it exists
            if (Files.exists(archivedPubKeyPath)) {
                Path targetPubKeyPath = Path.of(targetKeyPath.toString() + ".pub");
                Files.move(archivedPubKeyPath, targetPubKeyPath, StandardCopyOption.REPLACE_EXISTING);
                out.println("Restored: archived/" + keyName + ".pub -> " + relativeKeyPath + ".pub");
            }

            // Clean up empty directories left behind in archive
            cleanupEmptyDirectories(archivedKeyPath.getParent(), archiveDir);

            out.println();
            out.println("Key successfully restored to: " + relativeKeyPath);

            logger.info("Unarchived key to: {}", relativeKeyPath);
            return 0;

        } catch (IOException e) {
            err.println("Failed to unarchive key: " + e.getMessage());
            logger.error("Failed to unarchive key: {}", e.getMessage());
            return 1;
        }
    }

    /**
     * Recursively remove empty directories up to the archive base directory.
     */
    private void cleanupEmptyDirectories(Path dir, Path archiveDir) {
        if (dir == null || dir.equals(archiveDir) || !dir.startsWith(archiveDir)) {
            return;
        }

        try {
            // Only delete if directory is empty
            if (Files.isDirectory(dir)) {
                try (Stream<Path> entries = Files.list(dir)) {
                    if (entries.findAny().isEmpty()) {
                        Files.delete(dir);
                        // Recursively cleanup parent directories
                        cleanupEmptyDirectories(dir.getParent(), archiveDir);
                    }
                }
            }
        } catch (IOException e) {
            // Silently ignore cleanup errors
        }
    }

    private void listAvailableArchivedKeys(Path archiveDir, PrintWriter err) {
        try (Stream<Path> files = Files.walk(archiveDir)) {
            files.filter(Files::isRegularFile)
                .filter(p -> !p.getFileName().toString().endsWith(".pub"))
                .filter(this::isPrivateKey)
                .sorted()
                .forEach(p -> {
                    Path relativePath = archiveDir.relativize(p);
                    err.println("  - " + relativePath);
                });
        } catch (IOException e) {
            err.println("  (error listing archived keys)");
        }
    }

    /**
     * Custom completion candidates for archived key names.
     * This enables tab-completion for archived SSH keys.
     */
    public static class UnarchiveKeyCompletionCandidates implements Iterable<String> {
        @Override
        public java.util.Iterator<String> iterator() {
            List<String> keys = new ArrayList<>();
            Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
            Path archiveDir = sshDir.resolve("archived");

            if (!Files.exists(archiveDir) || !Files.isDirectory(archiveDir)) {
                return keys.iterator();
            }

            try (Stream<Path> files = Files.walk(archiveDir)) {
                files.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".pub"))
                    .filter(UnarchiveCommand::isPrivateKeyStatic)
                    .sorted()
                    .forEach(p -> {
                        Path relativePath = archiveDir.relativize(p);
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
