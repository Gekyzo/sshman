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

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static com.sshman.constants.SshManConstants.*;
import static com.sshman.utils.printer.Text.*;

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

    @Mixin
    private Printer printer;

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

    // ========================================================================
    // Constants
    // ========================================================================

    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);

    // ========================================================================
    // Main Entry Point
    // ========================================================================

    @Override
    public Integer call() {
        printer.println(gray("Equivalent SSH command: "), gray("none"));
        printer.emptyLine();

        Path sshDir = getSshDirectory();
        Path archiveDir = sshDir.resolve(DirectoryNames.ARCHIVED);

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
                printer.error(red("Key not found: "), bold(keyName));
                printer.emptyLine();
                printer.println(yellow("Available keys:"));
                SshKeyUtils.listAvailableKeys(sshDir, printer,
                    SshKeyUtils.ListKeysOptions.defaults()
                        .excludeArchivedKeys(true)
                        .excludeMetaFiles(true));
                logger.error("Key not found: {}", keyName);
                return 1;
            }
        }

        // Check if the key is already in the archived directory
        if (keyPath.startsWith(archiveDir)) {
            printer.error(red("Key is already archived: "), bold(keyName));
            logger.error("Key already archived: {}", keyName);
            return 1;
        }

        // Verify it's a private key
        if (!SshKeyUtils.isPrivateKey(keyPath)) {
            printer.error(red("Not a valid private key: "), bold(keyPath.toString()));
            logger.error("Not a valid private key: {}", keyName);
            return 1;
        }

        // Check if key is used in SSH config
        Path configPath = sshDir.resolve(FileNames.CONFIG);
        Set<String> affectedHosts = new HashSet<>();
        if (Files.exists(configPath)) {
            affectedHosts = findHostsUsingKey(configPath, keyPath);

            if (!affectedHosts.isEmpty() && !force) {
                printer.println(yellow("⚠ Warning: "), textOf("This key is currently used in SSH config for:"));
                for (String host : affectedHosts) {
                    printer.println("  ", gray("- "), cyan(host));
                }
                printer.emptyLine();

                // Prompt for confirmation
                if (!confirmArchive()) {
                    printer.println(gray("Archive cancelled."));
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
            printer.error(red("Failed to create archive directory: "), textOf(e.getMessage()));
            logger.error("Failed to create archive directory: {}", e.getMessage());
            return 1;
        }

        logger.info("Archiving key: {}", keyName);

        // Archive the key and its public key if it exists
        return archiveKey(keyPath, archiveDir, affectedHosts);
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
     * Recursively search for a key by filename in the SSH directory.
     * Excludes the archived directory from search.
     */
    private Path findKeyRecursive(Path baseDir, String keyName) {
        Path archiveDir = baseDir.resolve(DirectoryNames.ARCHIVED);
        try (Stream<Path> stream = Files.walk(baseDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> !p.startsWith(archiveDir))  // Exclude archived directory
                .filter(p -> p.getFileName().toString().equals(keyName))
                .filter(SshKeyUtils::isPrivateKey)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    // ========================================================================
    // Archive Methods
    // ========================================================================

    private int archiveKey(Path keyPath, Path archiveDir, Set<String> affectedHosts) {
        Path sshDir = getSshDirectory();
        Path pubKeyPath = Path.of(keyPath.toString() + FileExtensions.PUBLIC_KEY);
        Path metaPath = Path.of(keyPath.toString() + FileExtensions.METADATA);

        // Calculate relative path from SSH directory
        Path relativeKeyPath = sshDir.relativize(keyPath);

        // Destination paths in archive directory
        Path archivedKeyPath = archiveDir.resolve(relativeKeyPath);
        Path archivedPubKeyPath = Path.of(archivedKeyPath.toString() + FileExtensions.PUBLIC_KEY);
        Path archivedMetaPath = Path.of(archivedKeyPath.toString() + FileExtensions.METADATA);

        try {
            // Create parent directories in archive if needed
            Path archivedKeyParent = archivedKeyPath.getParent();
            if (archivedKeyParent != null) {
                Files.createDirectories(archivedKeyParent);
            }

            // Print header
            printer.println(bold(HEADER_LINE));
            printer.println(bold("  Archiving SSH Key"));
            printer.println(bold(HEADER_LINE));
            printer.emptyLine();

            // Move the private key
            Files.move(keyPath, archivedKeyPath, StandardCopyOption.REPLACE_EXISTING);
            printer.println(
                green("✓ "),
                label("Private key"),
                gray(relativeKeyPath.toString()),
                gray(" → "),
                cyan("archived/" + relativeKeyPath)
            );

            // Move the public key if it exists
            if (Files.exists(pubKeyPath)) {
                Files.move(pubKeyPath, archivedPubKeyPath, StandardCopyOption.REPLACE_EXISTING);
                printer.println(
                    green("✓ "),
                    label("Public key"),
                    gray(relativeKeyPath + FileExtensions.PUBLIC_KEY),
                    gray(" → "),
                    cyan(DirectoryNames.ARCHIVED + "/" + relativeKeyPath + FileExtensions.PUBLIC_KEY)
                );
            }

            // Move the metadata file if it exists
            if (Files.exists(metaPath)) {
                Files.move(metaPath, archivedMetaPath, StandardCopyOption.REPLACE_EXISTING);
                printer.println(
                    green("✓ "),
                    label("Metadata"),
                    gray(relativeKeyPath + FileExtensions.METADATA),
                    gray(" → "),
                    cyan(DirectoryNames.ARCHIVED + "/" + relativeKeyPath + FileExtensions.METADATA)
                );
            }

            // Clean up empty directories left behind
            cleanupEmptyDirectories(keyPath.getParent(), sshDir);

            printer.emptyLine();
            printer.println(gray(SEPARATOR_LINE));
            printer.println(green("✓ Key successfully archived!"));
            printer.println(green("   Archived to: "), gray(sshDir.relativize(archivedKeyPath).toString()));
            printer.emptyLine();

            // Show warning if key is still referenced in config
            if (!affectedHosts.isEmpty()) {
                printer.emptyLine();
                printer.println(yellow("⚠ Warning: "), textOf("Archived key is still referenced in " + PathPatterns.SSH_DIR_PATTERN + FileNames.CONFIG + ":"));
                for (String host : affectedHosts) {
                    printer.println("  ", gray("- "), cyan(host));
                }
                printer.emptyLine();
                printer.println(gray("Consider updating your SSH config to remove or update these references."));
            }

            printer.emptyLine();
            printer.println(gray("To restore this key, use: "), bold("sshman unarchive %s", keyName));
            printer.println(bold(HEADER_LINE));

            logger.info("Archived key to: {}/{}", DirectoryNames.ARCHIVED, relativeKeyPath);
            return 0;

        } catch (IOException e) {
            printer.error(red("Failed to archive key: "), textOf(e.getMessage()));
            logger.error("Failed to archive key: {}", e.getMessage());
            return 1;
        }
    }

    // ========================================================================
    // SSH Config Parsing Methods
    // ========================================================================

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
            String userHome = System.getProperty(SystemProperties.USER_HOME);

            String currentHost = null;
            List<String> lines = Files.readAllLines(configPath);

            for (String line : lines) {
                String trimmed = line.trim();

                // Skip comments and empty lines
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                // Parse Host directive
                if (trimmed.toLowerCase().startsWith(SshConfigKeywords.HOST)) {
                    currentHost = trimmed.substring(SshConfigKeywords.HOST.length()).trim();
                    continue;
                }

                // Parse IdentityFile directive
                if (currentHost != null && trimmed.toLowerCase().startsWith(SshConfigKeywords.IDENTITY_FILE)) {
                    String identityFile = trimmed.substring(SshConfigKeywords.IDENTITY_FILE.length()).trim();

                    // Remove quotes if present
                    if (identityFile.startsWith("\"") && identityFile.endsWith("\"")) {
                        identityFile = identityFile.substring(1, identityFile.length() - 1);
                    }

                    // Expand ~ to user home
                    if (identityFile.startsWith(PathPatterns.HOME_PREFIX)) {
                        identityFile = userHome + identityFile.substring(PathPatterns.HOME_PREFIX.length() - 1);
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

    // ========================================================================
    // User Interaction Methods
    // ========================================================================

    /**
     * Prompt user for confirmation to archive the key.
     * Returns true if user confirms, false otherwise.
     */
    private boolean confirmArchive() {
        Console console = System.console();

        if (console != null) {
            String response = console.readLine("%s%s",
                yellow("Archive this key anyway? "),
                gray("(y/N): ")
            );
            return response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"));
        } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                printer.prompt(yellow("Archive this key anyway? "), gray("(y/N): "));
                String response = reader.readLine();
                return response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"));
            } catch (IOException e) {
                return false;
            }
        }
    }

    // ========================================================================
    // Cleanup Methods
    // ========================================================================

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
                        printer.println(gray("  Removed empty directory: "), gray(sshDir.relativize(dir).toString()));
                        // Recursively cleanup parent directories
                        cleanupEmptyDirectories(dir.getParent(), sshDir);
                    }
                }
            }
        } catch (IOException e) {
            // Silently ignore cleanup errors
        }
    }

    // ========================================================================
    // Completion Candidates
    // ========================================================================

    /**
     * Custom completion candidates for key names.
     * This enables tab-completion for SSH keys, excluding archived keys.
     */
    public static class ArchiveKeyCompletionCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return SshKeyUtils.getKeyCompletionCandidates(SshKeyUtils.KeyScanMode.NON_ARCHIVED);
        }
    }
}
