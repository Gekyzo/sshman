package com.sshman.commands;

import com.sshman.Profile;
import com.sshman.ProfileStorage;
import com.sshman.utils.SshKeyUtils;
import com.sshman.utils.printer.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static com.sshman.utils.printer.Text.*;

@Command(
    name = "rotate",
    description = "Rotate SSH key(s) by archiving the old key, generating a new one, and updating references",
    mixinStandardHelpOptions = true,
    footer = "%nExamples:%n" +
        "  sshman rotate id_rsa%n" +
        "  sshman rotate id_work_ed25519 --comment \"work key\"%n" +
        "  sshman rotate id_old --type ed25519%n" +
        "  sshman rotate key1 key2 key3%n" +
        "  sshman rotate id_rsa --dry-run%n" +
        "  sshman rotate id_rsa --upload user@example.com%n"
)
public class RotateCommand implements Callable<Integer> {

    @Mixin
    private Printer printer;

    @Parameters(
        index = "0..*",
        description = "Key name(s) to rotate (filename(s) in ~/.ssh)",
        completionCandidates = RotateKeyCompletionCandidates.class,
        arity = "1..*"
    )
    private List<String> keyNames;

    @Option(names = {"--path"}, description = "Custom SSH directory path")
    private String customPath;

    @Option(names = {"-f", "--force"}, description = "Skip all confirmation prompts")
    private boolean force;

    @Option(names = {"--type", "-t"}, description = "Change key type (ed25519, rsa, ecdsa). Default: preserve original type")
    private String newKeyType;

    @Option(names = {"--comment", "-c"}, description = "Comment for the new key. Default: preserve original comment")
    private String comment;

    @Option(names = {"--dry-run"}, description = "Preview changes without executing them")
    private boolean dryRun;

    @Option(names = {"--upload", "-u"}, description = "Upload new public key to remote host(s) using ssh-copy-id. Format: user@host or user@host1,user@host2")
    private String uploadTargets;

    @Option(names = {"--no-test"}, description = "Skip connection testing before archiving old key")
    private boolean noTest;

    @Option(names = {"--no-backup"}, description = "Skip backing up SSH config file")
    private boolean noBackup;

    // Private key header bytes: "-----BEGIN"
    private static final byte[] PRIVATE_KEY_MAGIC = "-----BEGIN".getBytes();

    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter BACKUP_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // Track all operations for logging
    private List<String> rotationLog = new ArrayList<>();
    private int successCount = 0;
    private int failureCount = 0;

    @Override
    public Integer call() {
        Path sshDir = getSshDirectory();
        Path archiveDir = sshDir.resolve("archived");

        if (dryRun) {
            printer.println(yellow("=== DRY RUN MODE - No changes will be made ==="));
            printer.emptyLine();
        }

        // Validate new key type if specified
        if (newKeyType != null && !isValidKeyType(newKeyType)) {
            printer.error(red("Invalid key type: "), bold(newKeyType));
            printer.error(gray("Valid types: ed25519, rsa, ecdsa"));
            return 1;
        }

        // Process each key
        boolean overallSuccess = true;
        for (String keyName : keyNames) {
            printer.println(bold(HEADER_LINE));
            printer.println(bold("  Rotating key: "), cyan(keyName));
            printer.println(bold(HEADER_LINE));
            printer.emptyLine();

            int result = rotateKey(keyName, sshDir, archiveDir);
            if (result != 0) {
                overallSuccess = false;
                failureCount++;
            } else {
                successCount++;
            }
            printer.emptyLine();
        }

        // Print summary
        printSummary();

        // Write log file
        if (!dryRun && !rotationLog.isEmpty()) {
            writeRotationLog(sshDir);
        }

        return overallSuccess ? 0 : 1;
    }

    private int rotateKey(String keyName, Path sshDir, Path archiveDir) {
        // Resolve the key path
        Path keyPath = sshDir.resolve(keyName);

        // Check if key exists
        if (!Files.exists(keyPath)) {
            Path foundKey = findKeyRecursive(sshDir, keyName);
            if (foundKey != null) {
                keyPath = foundKey;
                keyName = sshDir.relativize(foundKey).toString();
            } else {
                printer.error(red("✗ Key not found: "), bold(keyName));
                logOperation("FAILED", "Key not found: " + keyName);
                return 1;
            }
        }

        // Check if the key is already in the archived directory
        if (keyPath.startsWith(archiveDir)) {
            printer.error(red("✗ Key is already archived: "), bold(keyName));
            logOperation("FAILED", "Key already archived: " + keyName);
            return 1;
        }

        // Verify it's a private key
        if (!isPrivateKey(keyPath)) {
            printer.error(red("✗ Not a valid private key: "), bold(keyPath.toString()));
            logOperation("FAILED", "Not a private key: " + keyName);
            return 1;
        }

        // Detect key type and comment from existing key
        String originalKeyType = detectKeyType(keyPath);
        String originalComment = extractComment(keyPath);
        String targetKeyType = newKeyType != null ? newKeyType : originalKeyType;
        String targetComment = comment != null ? comment : (originalComment != null ? originalComment : generateDefaultComment(keyName));

        printer.println(gray("Original key type: "), cyan(originalKeyType));
        printer.println(gray("New key type: "), cyan(targetKeyType));
        printer.println(label("Comment"), gray(targetComment));
        printer.emptyLine();

        // Find hosts using this key in SSH config
        Path configPath = sshDir.resolve("config");
        Set<String> affectedHosts = new HashSet<>();
        if (Files.exists(configPath)) {
            affectedHosts = findHostsUsingKey(configPath, keyPath);
        }

        // Find profiles using this key
        List<Profile> affectedProfiles = findProfilesUsingKey(keyPath);

        // Display affected hosts and profiles
        if (!affectedHosts.isEmpty()) {
            printer.println(bold("Hosts using this key in SSH config:"));
            for (String host : affectedHosts) {
                printer.println("  ", cyan("- "), textOf(host));
            }
            printer.emptyLine();
        }

        if (!affectedProfiles.isEmpty()) {
            printer.println(bold("Connection profiles using this key:"));
            for (Profile profile : affectedProfiles) {
                printer.println("  ", cyan("- "), textOf(profile.getAlias()));
            }
            printer.emptyLine();
        }

        // Test connection if requested
        if (!noTest && !affectedHosts.isEmpty() && !dryRun) {
            printer.println(gray("Testing connection with old key..."));
            if (!testConnection(affectedHosts.iterator().next(), keyPath)) {
                if (!force) {
                    printer.println(red("✗ Connection test failed. Use --force to skip or --no-test to disable testing."));
                    logOperation("FAILED", "Connection test failed for: " + keyName);
                    return 1;
                }
                printer.println(yellow("⚠ Connection test failed, but continuing due to --force"));
            } else {
                printer.println(green("✓ Connection test passed"));
            }
            printer.emptyLine();
        }

        // Confirm rotation
        if (!force && !dryRun) {
            if (!confirmRotation(keyName, affectedHosts, affectedProfiles)) {
                printer.println(gray("Rotation cancelled."));
                logOperation("CANCELLED", "User cancelled rotation: " + keyName);
                return 1;
            }
        }

        if (dryRun) {
            printer.println(yellow("[DRY RUN] Would perform the following:"));
            printer.println("  ", gray("1. Backup SSH config to: archived/config_backups/config_" + BACKUP_TIMESTAMP_FORMAT.format(LocalDateTime.now())));
            printer.println("  ", gray("2. Generate new " + targetKeyType + " key at: " + keyPath));
            printer.println("  ", gray("3. Archive old key to: archived/" + keyName));
            printer.println("  ", gray("4. Update " + affectedHosts.size() + " host(s) in SSH config"));
            printer.println("  ", gray("5. Update " + affectedProfiles.size() + " connection profile(s)"));
            if (uploadTargets != null) {
                printer.println("  ", gray("6. Upload new public key to: " + uploadTargets));
            }
            logOperation("DRY-RUN", "Simulated rotation: " + keyName);
            return 0;
        }

        // Backup SSH config
        if (!noBackup && Files.exists(configPath)) {
            if (!backupSshConfig(configPath, archiveDir)) {
                printer.println(red("✗ Failed to backup SSH config"));
                logOperation("FAILED", "Config backup failed for: " + keyName);
                return 1;
            }
        }

        // Generate new key
        Path tempNewKeyPath = null;
        try {
            // Generate to temporary location first
            tempNewKeyPath = Files.createTempFile("sshman_rotate_", "_" + keyPath.getFileName().toString());

            if (!generateNewKey(tempNewKeyPath, targetKeyType, targetComment)) {
                printer.println(red("✗ Failed to generate new key"));
                logOperation("FAILED", "Key generation failed for: " + keyName);
                return 1;
            }

            printer.println(green("✓ Generated new "), cyan(targetKeyType), green(" key"));
            logOperation("GENERATED", "New " + targetKeyType + " key for: " + keyName);

            // Test new key if connection test is enabled
            if (!noTest && !affectedHosts.isEmpty()) {
                printer.println(gray("Testing connection with new key..."));
                if (!testConnection(affectedHosts.iterator().next(), tempNewKeyPath)) {
                    printer.println(red("✗ New key connection test failed"));
                    logOperation("FAILED", "New key connection test failed for: " + keyName);
                    Files.deleteIfExists(tempNewKeyPath);
                    Files.deleteIfExists(Path.of(tempNewKeyPath.toString() + ".pub"));
                    return 1;
                }
                printer.println(green("✓ New key connection test passed"));
            }

            // Archive old key
            if (!archiveOldKey(keyPath, archiveDir, sshDir)) {
                printer.println(red("✗ Failed to archive old key"));
                logOperation("FAILED", "Key archival failed for: " + keyName);
                Files.deleteIfExists(tempNewKeyPath);
                Files.deleteIfExists(Path.of(tempNewKeyPath.toString() + ".pub"));
                return 1;
            }

            printer.println(green("✓ Archived old key"));
            logOperation("ARCHIVED", "Old key: " + keyName);

            // Move new key to final location
            Files.move(tempNewKeyPath, keyPath, StandardCopyOption.REPLACE_EXISTING);
            Path tempPubPath = Path.of(tempNewKeyPath.toString() + ".pub");
            Path finalPubPath = Path.of(keyPath.toString() + ".pub");
            if (Files.exists(tempPubPath)) {
                Files.move(tempPubPath, finalPubPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Set proper permissions
            setKeyPermissions(keyPath);

            printer.println(green("✓ New key installed at: "), gray(keyPath.toString()));

            // Update SSH config
            if (!affectedHosts.isEmpty()) {
                int updatedHosts = updateSshConfig(configPath, keyPath, affectedHosts);
                if (updatedHosts > 0) {
                    printer.println(green("✓ Updated "), textOf(String.valueOf(updatedHosts)), green(" host(s) in SSH config"));
                    logOperation("UPDATED-CONFIG", "Updated " + updatedHosts + " host(s) for: " + keyName);
                }
            }

            // Update connection profiles
            if (!affectedProfiles.isEmpty()) {
                int updatedProfiles = updateProfiles(keyPath, affectedProfiles);
                if (updatedProfiles > 0) {
                    printer.println(green("✓ Updated "), textOf(String.valueOf(updatedProfiles)), green(" connection profile(s)"));
                    logOperation("UPDATED-PROFILES", "Updated " + updatedProfiles + " profile(s) for: " + keyName);
                }
            }

            // Upload new key if requested
            if (uploadTargets != null) {
                uploadPublicKey(finalPubPath, uploadTargets);
            }

            printer.emptyLine();
            printer.println(green("✓ Key rotation completed successfully: "), bold(keyName));
            logOperation("SUCCESS", "Completed rotation: " + keyName);

            return 0;

        } catch (Exception e) {
            printer.println(red("✗ Rotation failed: "), textOf(e.getMessage()));
            logOperation("FAILED", "Exception during rotation of " + keyName + ": " + e.getMessage());

            // Cleanup temporary files
            if (tempNewKeyPath != null) {
                try {
                    Files.deleteIfExists(tempNewKeyPath);
                    Files.deleteIfExists(Path.of(tempNewKeyPath.toString() + ".pub"));
                } catch (IOException cleanupEx) {
                    // Ignore cleanup errors
                }
            }

            return 1;
        }
    }

    private Path getSshDirectory() {
        if (customPath != null && !customPath.isEmpty()) {
            return Path.of(customPath);
        }
        return Path.of(System.getProperty("user.home"), ".ssh");
    }

    private Path findKeyRecursive(Path baseDir, String keyName) {
        Path archiveDir = baseDir.resolve("archived");
        try (Stream<Path> stream = Files.walk(baseDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> !p.startsWith(archiveDir))
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

    private boolean isValidKeyType(String type) {
        return type.equals("ed25519") || type.equals("rsa") || type.equals("ecdsa");
    }

    private String detectKeyType(Path keyPath) {
        try {
            List<String> lines = Files.readAllLines(keyPath);
            for (String line : lines) {
                if (line.contains("BEGIN OPENSSH PRIVATE KEY") || line.contains("BEGIN SSH2 ENCRYPTED PRIVATE KEY")) {
                    // Need to check public key or use ssh-keygen
                    return detectKeyTypeFromPublicKey(keyPath);
                }
                if (line.contains("BEGIN RSA PRIVATE KEY")) {
                    return "rsa";
                }
                if (line.contains("BEGIN EC PRIVATE KEY")) {
                    return "ecdsa";
                }
                if (line.contains("BEGIN DSA PRIVATE KEY")) {
                    return "dsa";
                }
            }
            // Default to checking public key
            return detectKeyTypeFromPublicKey(keyPath);
        } catch (IOException e) {
            return "ed25519"; // Default fallback
        }
    }

    private String detectKeyTypeFromPublicKey(Path keyPath) {
        Path pubKeyPath = Path.of(keyPath.toString() + ".pub");
        if (!Files.exists(pubKeyPath)) {
            return "ed25519"; // Default
        }

        try {
            String pubKeyContent = Files.readString(pubKeyPath);
            if (pubKeyContent.startsWith("ssh-ed25519")) {
                return "ed25519";
            } else if (pubKeyContent.startsWith("ssh-rsa")) {
                return "rsa";
            } else if (pubKeyContent.startsWith("ecdsa-sha2-")) {
                return "ecdsa";
            }
        } catch (IOException e) {
            // Ignore
        }

        return "ed25519"; // Default
    }

    private String extractComment(Path keyPath) {
        Path pubKeyPath = Path.of(keyPath.toString() + ".pub");
        if (!Files.exists(pubKeyPath)) {
            return null;
        }

        try {
            String pubKeyContent = Files.readString(pubKeyPath).trim();
            String[] parts = pubKeyContent.split("\\s+");
            if (parts.length >= 3) {
                // Comment is the third part and onwards
                return String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
            }
        } catch (IOException e) {
            // Ignore
        }

        return null;
    }

    private String generateDefaultComment(String keyName) {
        String user = System.getProperty("user.name");
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "localhost";
        }
        return String.format("%s@%s (rotated %s)", user, host, keyName);
    }

    private Set<String> findHostsUsingKey(Path configPath, Path keyPath) {
        Set<String> affectedHosts = new HashSet<>();

        try {
            Path canonicalKeyPath = keyPath.toRealPath();
            Path sshDir = getSshDirectory();
            String userHome = System.getProperty("user.home");

            String currentHost = null;
            List<String> lines = Files.readAllLines(configPath);

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (trimmed.toLowerCase().startsWith("host ")) {
                    currentHost = trimmed.substring(5).trim();
                    continue;
                }

                if (currentHost != null && trimmed.toLowerCase().startsWith("identityfile ")) {
                    String identityFile = trimmed.substring(13).trim();

                    if (identityFile.startsWith("\"") && identityFile.endsWith("\"")) {
                        identityFile = identityFile.substring(1, identityFile.length() - 1);
                    }

                    if (identityFile.startsWith("~/")) {
                        identityFile = userHome + identityFile.substring(1);
                    }

                    Path identityPath = Path.of(identityFile);
                    if (!identityPath.isAbsolute()) {
                        identityPath = sshDir.resolve(identityFile);
                    }

                    try {
                        if (identityPath.toRealPath().equals(canonicalKeyPath)) {
                            affectedHosts.add(currentHost);
                        }
                    } catch (IOException e) {
                        if (identityPath.normalize().equals(keyPath.normalize())) {
                            affectedHosts.add(currentHost);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Return empty set
        }

        return affectedHosts;
    }

    private List<Profile> findProfilesUsingKey(Path keyPath) {
        List<Profile> affected = new ArrayList<>();

        try {
            ProfileStorage storage = new ProfileStorage();
            List<Profile> allProfiles = storage.loadProfiles();

            String keyPathStr = keyPath.toString();
            Path canonicalKeyPath = keyPath.toRealPath();

            for (Profile profile : allProfiles) {
                if (profile.getSshKey() != null) {
                    Path profileKeyPath = Path.of(profile.getSshKey());
                    try {
                        if (profileKeyPath.toRealPath().equals(canonicalKeyPath)) {
                            affected.add(profile);
                        }
                    } catch (IOException e) {
                        if (profileKeyPath.normalize().equals(keyPath.normalize())) {
                            affected.add(profile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Return empty list
        }

        return affected;
    }

    private boolean confirmRotation(String keyName,
                                    Set<String> affectedHosts,
                                    List<Profile> affectedProfiles) {
        Console console = System.console();

        String message = String.format(
            "Rotate key '%s' (affects %d host(s) and %d profile(s))? (y/N): ",
            keyName, affectedHosts.size(), affectedProfiles.size()
        );

        if (console != null) {
            String response = console.readLine(yellow(message).toString());
            return response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"));
        } else {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                printer.prompt(yellow(message));
                String response = reader.readLine();
                return response != null && (response.equalsIgnoreCase("y") || response.equalsIgnoreCase("yes"));
            } catch (IOException e) {
                return false;
            }
        }
    }

    private boolean backupSshConfig(Path configPath, Path archiveDir) {
        try {
            Path backupDir = archiveDir.resolve("config_backups");
            Files.createDirectories(backupDir);

            String timestamp = BACKUP_TIMESTAMP_FORMAT.format(LocalDateTime.now());
            Path backupPath = backupDir.resolve("config_" + timestamp);

            Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);

            printer.println(green("✓ Backed up SSH config to: "), gray("archived/config_backups/config_" + timestamp));
            logOperation("BACKUP", "SSH config backed up: config_" + timestamp);

            return true;
        } catch (IOException e) {
            printer.println(red("Failed to backup SSH config: "), textOf(e.getMessage()));
            return false;
        }
    }

    private boolean generateNewKey(Path keyPath, String keyType, String comment) {
        List<String> command = new ArrayList<>();
        command.add("ssh-keygen");
        command.add("-t");
        command.add(keyType);
        command.add("-f");
        command.add(keyPath.toString());
        command.add("-C");
        command.add(comment);
        command.add("-N");
        command.add(""); // No passphrase

        // Add default bits for RSA
        if (keyType.equals("rsa")) {
            command.add("-b");
            command.add("4096");
        } else if (keyType.equals("ecdsa")) {
            command.add("-b");
            command.add("256");
        }

        // Display equivalent SSH command
        printer.println(gray("Equivalent SSH command: "), textOf(formatCommand(command)));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Suppress ssh-keygen output unless there's an error
                    if (line.toLowerCase().contains("error") || line.toLowerCase().contains("failed")) {
                        printer.println(red(line));
                    }
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            printer.println(red("Failed to generate key: "), textOf(e.getMessage()));
            return false;
        }
    }

    private boolean archiveOldKey(Path keyPath, Path archiveDir, Path sshDir) {
        try {
            Path relativeKeyPath = sshDir.relativize(keyPath);
            Path archivedKeyPath = archiveDir.resolve(relativeKeyPath);

            Path archivedKeyParent = archivedKeyPath.getParent();
            if (archivedKeyParent != null) {
                Files.createDirectories(archivedKeyParent);
            }

            Files.move(keyPath, archivedKeyPath, StandardCopyOption.REPLACE_EXISTING);

            Path pubKeyPath = Path.of(keyPath.toString() + ".pub");
            if (Files.exists(pubKeyPath)) {
                Path archivedPubKeyPath = Path.of(archivedKeyPath.toString() + ".pub");
                Files.move(pubKeyPath, archivedPubKeyPath, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (IOException e) {
            printer.println(red("Failed to archive old key: "), textOf(e.getMessage()));
            return false;
        }
    }

    private void setKeyPermissions(Path keyPath) {
        try {
            // Private key: 600 (rw-------)
            keyPath.toFile().setReadable(false, false);
            keyPath.toFile().setWritable(false, false);
            keyPath.toFile().setExecutable(false, false);
            keyPath.toFile().setReadable(true, true);
            keyPath.toFile().setWritable(true, true);

            // Public key: 644 (rw-r--r--)
            Path pubKeyPath = Path.of(keyPath.toString() + ".pub");
            if (Files.exists(pubKeyPath)) {
                pubKeyPath.toFile().setReadable(true, false);
                pubKeyPath.toFile().setWritable(false, false);
                pubKeyPath.toFile().setExecutable(false, false);
                pubKeyPath.toFile().setWritable(true, true);
            }
        } catch (Exception e) {
            // Silently ignore permission errors
        }
    }

    private int updateSshConfig(Path configPath,
                                Path newKeyPath,
                                Set<String> affectedHosts) {
        try {
            List<String> lines = Files.readAllLines(configPath);
            List<String> updatedLines = new ArrayList<>();
            String currentHost = null;
            int updatedCount = 0;
            Path sshDir = getSshDirectory();
            String relativePath = sshDir.relativize(newKeyPath).toString();

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.toLowerCase().startsWith("host ")) {
                    currentHost = trimmed.substring(5).trim();
                    updatedLines.add(line);
                    continue;
                }

                if (currentHost != null && affectedHosts.contains(currentHost) &&
                    trimmed.toLowerCase().startsWith("identityfile ")) {

                    // Replace the IdentityFile line
                    String indent = line.substring(0, line.indexOf("IdentityFile"));
                    updatedLines.add(indent + "IdentityFile ~/.ssh/" + relativePath);
                    updatedCount++;
                    logOperation("UPDATED-HOST", "SSH config host: " + currentHost);
                } else {
                    updatedLines.add(line);
                }
            }

            Files.write(configPath, updatedLines);
            return updatedCount;

        } catch (IOException e) {
            printer.println(red("Failed to update SSH config: "), textOf(e.getMessage()));
            return 0;
        }
    }

    private int updateProfiles(Path newKeyPath, List<Profile> affectedProfiles) {
        try {
            ProfileStorage storage = new ProfileStorage();
            List<Profile> allProfiles = storage.loadProfiles();
            int updatedCount = 0;

            for (Profile profile : allProfiles) {
                if (affectedProfiles.contains(profile)) {
                    profile.setSshKey(newKeyPath.toString());
                    updatedCount++;
                    logOperation("UPDATED-PROFILE", "Connection profile: " + profile.getAlias());
                }
            }

            storage.saveProfiles(allProfiles);
            return updatedCount;

        } catch (Exception e) {
            printer.println(red("Failed to update profiles: "), textOf(e.getMessage()));
            return 0;
        }
    }

    private boolean testConnection(String host, Path keyPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add("ssh");
            command.add("-i");
            command.add(keyPath.toString());
            command.add("-o");
            command.add("BatchMode=yes");
            command.add("-o");
            command.add("ConnectTimeout=10");
            command.add("-o");
            command.add("StrictHostKeyChecking=no");
            command.add(host);
            command.add("exit");

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private void uploadPublicKey(Path pubKeyPath, String targets) {
        String[] targetList = targets.split(",");

        for (String target : targetList) {
            target = target.trim();
            printer.println(gray("Uploading public key to: "), cyan(target));

            try {
                List<String> command = new ArrayList<>();
                command.add("ssh-copy-id");
                command.add("-i");
                command.add(pubKeyPath.toString());
                command.add(target);

                // Display equivalent SSH command
                printer.println(gray("Equivalent SSH command: "), textOf(formatCommand(command)));

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.inheritIO();
                Process process = pb.start();

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    printer.println(green("✓ Uploaded public key to: "), cyan(target));
                    logOperation("UPLOADED", "Public key to: " + target);
                } else {
                    printer.println(red("✗ Failed to upload public key to: "), cyan(target));
                    logOperation("UPLOAD-FAILED", "Failed to upload to: " + target);
                }

            } catch (IOException | InterruptedException e) {
                printer.println(red("✗ Error uploading to "), cyan(target), red(": "), textOf(e.getMessage()));
                logOperation("UPLOAD-ERROR", "Error uploading to " + target + ": " + e.getMessage());
            }
        }
    }

    private void logOperation(String status, String message) {
        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now());
        rotationLog.add(String.format("- **[%s]** %s - %s", timestamp, status, message));
    }

    private void printSummary() {
        printer.println(bold(HEADER_LINE));
        printer.println(bold("  Rotation Summary"));
        printer.println(bold(HEADER_LINE));
        printer.println(gray("Total keys processed: "), textOf(String.valueOf(keyNames.size())));
        printer.println(label("Successful"), green(" " + successCount));
        printer.println(label("Failed"), failureCount > 0 ? red(" " + failureCount) : textOf(" " + failureCount));
        printer.println(bold(HEADER_LINE));
    }

    private void writeRotationLog(Path sshDir) {
        try {
            Path logFile = sshDir.resolve("rotation.log");
            boolean logExists = Files.exists(logFile);

            List<String> logLines = new ArrayList<>();

            // Add header if new file
            if (!logExists) {
                logLines.add("# SSH Key Rotation Log");
                logLines.add("");
                logLines.add("This file contains a history of all SSH key rotations performed by sshman.");
                logLines.add("");
            } else {
                // Read existing content
                logLines.addAll(Files.readAllLines(logFile));
                logLines.add("");
            }

            // Add new rotation entry
            logLines.add("## Rotation Session - " + TIMESTAMP_FORMAT.format(LocalDateTime.now()));
            logLines.add("");
            logLines.addAll(rotationLog);
            logLines.add("");

            Files.write(logFile, logLines);

            printer.println(green("✓ Rotation log updated: "), gray(sshDir.relativize(logFile).toString()));

        } catch (IOException e) {
            printer.println(yellow("⚠ Failed to write rotation log: "), textOf(e.getMessage()));
        }
    }

    /**
     * Custom completion candidates for key names.
     * This enables tab-completion for SSH keys, excluding archived keys.
     */
    public static class RotateKeyCompletionCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return SshKeyUtils.getKeyCompletionCandidates(SshKeyUtils.KeyScanMode.NON_ARCHIVED);
        }
    }

    /**
     * Format a command list for display, escaping arguments with spaces or special characters.
     */
    private String formatCommand(List<String> command) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < command.size(); i++) {
            String arg = command.get(i);

            // Hide passphrase values
            if (i > 0 && "-N".equals(command.get(i - 1))) {
                sb.append("\"***\"");
            } else if (arg.contains(" ") || arg.contains("\"") || arg.contains("'")) {
                // Quote arguments with spaces or quotes
                sb.append("\"").append(arg.replace("\"", "\\\"")).append("\"");
            } else {
                sb.append(arg);
            }

            if (i < command.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
