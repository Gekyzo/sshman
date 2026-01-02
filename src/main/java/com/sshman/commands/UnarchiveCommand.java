package com.sshman.commands;

import com.sshman.constants.SshManConstants;
import com.sshman.utils.SshKeyUtils;
import com.sshman.utils.printer.Printer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static com.sshman.constants.SshManConstants.*;
import static com.sshman.utils.printer.Text.*;

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

    @Mixin
    private Printer printer;

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

    // ========================================================================
    // Constants
    // ========================================================================

    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);

    // Private key header bytes
    private static final byte[] PRIVATE_KEY_MAGIC = PrivateKeyHeaders.PRIVATE_KEY_MAGIC;

    // ========================================================================
    // Main Entry Point
    // ========================================================================

    @Override
    public Integer call() {
        printer.println(gray("Equivalent SSH command: "), gray("none"));
        printer.emptyLine();

        Path sshDir = getSshDirectory();
        Path archiveDir = sshDir.resolve(DirectoryNames.ARCHIVED);

        // Resolve the archived key path
        Path archivedKeyPath = archiveDir.resolve(keyName);

        // Check if archived directory exists
        if (!Files.exists(archiveDir)) {
            printer.error(red("Archive directory does not exist: "), bold(archiveDir.toString()));
            printer.println(gray("No keys have been archived yet."));
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
                printer.error(red("Archived key not found: "), bold(keyName));
                printer.emptyLine();
                printer.println(yellow("Available archived keys:"));
                listAvailableArchivedKeys(archiveDir);
                logger.error("Archived key not found: {}", keyName);
                return 1;
            }
        }

        // Verify it's a private key
        if (!isPrivateKey(archivedKeyPath)) {
            printer.error(red("Not a valid private key: "), bold(archivedKeyPath.toString()));
            logger.error("Not a valid private key: {}", keyName);
            return 1;
        }

        // Calculate target path (restore to original location)
        Path targetKeyPath = sshDir.resolve(keyName);

        // Check if key already exists at target location
        if (Files.exists(targetKeyPath) && !force) {
            printer.error(red("Key already exists at target location: "), bold(keyName));
            printer.error(gray("Use "), bold("--force"), gray(" to overwrite the existing key."));
            logger.error("Key already exists (use --force to overwrite): {}", keyName);
            return 1;
        }

        if (Files.exists(targetKeyPath)) {
            printer.println(yellow("⚠ Overwriting existing key..."));
            logger.warn("Overwriting existing key: {}", keyName);
        }

        logger.info("Unarchiving key: {}", keyName);

        // Unarchive the key and its public key if it exists
        return unarchiveKey(archivedKeyPath, targetKeyPath);
    }

    // ========================================================================
    // Directory and Path Methods
    // ========================================================================

    private Path getSshDirectory() {
        if (customPath != null && !customPath.isEmpty()) {
            return Path.of(customPath);
        }
        return Path.of(System.getProperty(SystemProperties.USER_HOME), DirectoryNames.SSH);
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

    // ========================================================================
    // Key Validation Methods
    // ========================================================================

    private boolean isPrivateKey(Path path) {
        if (path.getFileName().toString().endsWith(FileExtensions.PUBLIC_KEY)) {
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

    // ========================================================================
    // Unarchive Methods
    // ========================================================================

    private int unarchiveKey(Path archivedKeyPath, Path targetKeyPath) {
        Path sshDir = getSshDirectory();
        Path archiveDir = sshDir.resolve(DirectoryNames.ARCHIVED);
        Path archivedPubKeyPath = Path.of(archivedKeyPath.toString() + FileExtensions.PUBLIC_KEY);
        Path archivedMetaPath = Path.of(archivedKeyPath.toString() + FileExtensions.METADATA);

        try {
            // Create parent directories for target if needed
            Path targetKeyParent = targetKeyPath.getParent();
            if (targetKeyParent != null && !Files.exists(targetKeyParent)) {
                Files.createDirectories(targetKeyParent);
                printer.println(green("✓ Created directory: "), gray(sshDir.relativize(targetKeyParent).toString()));
            }

            // Print header
            printer.println(bold(HEADER_LINE));
            printer.println(bold("  Restoring SSH Key"));
            printer.println(bold(HEADER_LINE));
            printer.emptyLine();

            Path relativeKeyPath = sshDir.relativize(targetKeyPath);

            // Move the private key
            Files.move(archivedKeyPath, targetKeyPath, StandardCopyOption.REPLACE_EXISTING);
            printer.println(
                green("✓ "),
                label("Private key"),
                gray(DirectoryNames.ARCHIVED + "/" + keyName),
                gray(" → "),
                cyan(relativeKeyPath.toString())
            );

            // Move the public key if it exists
            if (Files.exists(archivedPubKeyPath)) {
                Path targetPubKeyPath = Path.of(targetKeyPath.toString() + FileExtensions.PUBLIC_KEY);
                Files.move(archivedPubKeyPath, targetPubKeyPath, StandardCopyOption.REPLACE_EXISTING);
                printer.println(
                    green("✓ "),
                    label("Public key"),
                    gray(DirectoryNames.ARCHIVED + "/" + keyName + FileExtensions.PUBLIC_KEY),
                    gray(" → "),
                    cyan(relativeKeyPath + FileExtensions.PUBLIC_KEY)
                );
            }

            // Move the metadata file if it exists
            if (Files.exists(archivedMetaPath)) {
                Path targetMetaPath = Path.of(targetKeyPath.toString() + FileExtensions.METADATA);
                Files.move(archivedMetaPath, targetMetaPath, StandardCopyOption.REPLACE_EXISTING);
                printer.println(
                    green("✓ "),
                    label("Metadata"),
                    gray(DirectoryNames.ARCHIVED + "/" + keyName + FileExtensions.METADATA),
                    gray(" → "),
                    cyan(relativeKeyPath + FileExtensions.METADATA)
                );
            }

            // Clean up empty directories left behind in archive
            cleanupEmptyDirectories(archivedKeyPath.getParent(), archiveDir);

            printer.emptyLine();
            printer.println(gray(SEPARATOR_LINE));
            printer.println(green("✓ Key successfully restored!"));
            printer.println(green("   Restored to: "), gray(relativeKeyPath.toString()));
            printer.emptyLine();

            printer.emptyLine();
            printer.println(gray("Run '"), bold("sshman info %s", keyName), gray("' to see key details"));
            printer.println(bold(HEADER_LINE));

            logger.info("Unarchived key to: {}", relativeKeyPath);
            return 0;

        } catch (IOException e) {
            printer.error(red("Failed to unarchive key: "), textOf(e.getMessage()));
            logger.error("Failed to unarchive key: {}", e.getMessage());
            return 1;
        }
    }

    // ========================================================================
    // Cleanup Methods
    // ========================================================================

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
                        printer.println(gray("  Removed empty directory: "), gray(DirectoryNames.ARCHIVED + "/" + archiveDir.relativize(dir)));
                        // Recursively cleanup parent directories
                        cleanupEmptyDirectories(dir.getParent(), archiveDir);
                    }
                }
            }
        } catch (IOException e) {
            // Silently ignore cleanup errors
        }
    }

    // ========================================================================
    // Listing Methods
    // ========================================================================

    private void listAvailableArchivedKeys(Path archiveDir) {
        try (Stream<Path> files = Files.walk(archiveDir)) {
            List<Path> keys = files
                .filter(Files::isRegularFile)
                .filter(p -> !p.getFileName().toString().endsWith(FileExtensions.PUBLIC_KEY))
                .filter(p -> !p.getFileName().toString().endsWith(FileExtensions.METADATA))
                .filter(this::isPrivateKey)
                .sorted()
                .toList();

            if (keys.isEmpty()) {
                printer.println(gray("  (no archived keys found)"));
            } else {
                for (Path key : keys) {
                    Path relativePath = archiveDir.relativize(key);
                    printer.println("  ", gray("- "), textOf(relativePath.toString()));
                }
            }
        } catch (IOException e) {
            printer.error(red("  (error listing archived keys)"));
        }
    }

    // ========================================================================
    // Completion Candidates
    // ========================================================================

    /**
     * Custom completion candidates for archived key names.
     * This enables tab-completion for archived SSH keys.
     */
    public static class UnarchiveKeyCompletionCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return SshKeyUtils.getKeyCompletionCandidates(SshKeyUtils.KeyScanMode.ARCHIVED_ONLY);
        }
    }
}
