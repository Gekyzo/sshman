package com.sshman.commands;

import com.sshman.KeyMetadata;
import com.sshman.utils.printer.Printer;
import com.sshman.utils.printer.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.sshman.utils.printer.Text.*;

@Command(
    name = "generate",
    description = "Generate a new SSH key",
    mixinStandardHelpOptions = true
)
public class GenerateCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(GenerateCommand.class);

    @Mixin
    private Printer printer;

    @Option(names = {"-a", "--algo"},
        description = "Algorithm: ed25519, rsa, ecdsa (default: ${DEFAULT-VALUE})",
        defaultValue = "ed25519")
    private KeyAlgorithm algorithm;

    @Option(names = {"-u", "--use"},
        description = "Use case/folder structure (e.g., 'work', 'personal', 'work/project-a')")
    private String use;

    @Option(names = {"-n", "--name"},
        description = "Key name/filename (if not specified, generated from --use and algorithm)")
    private String name;

    @Option(names = {"-c", "--comment"},
        description = "Comment for the key (default: user@hostname)")
    private String comment;

    @Option(names = {"-d", "--description"},
        description = "Description for the key (stored in metadata)")
    private String description;

    @Option(names = {"-b", "--bits"},
        description = "Key size in bits (RSA: 2048-4096, ECDSA: 256/384/521)")
    private Integer bits;

    @Option(names = {"-p", "--passphrase"},
        description = "Passphrase for the key (empty for no passphrase)",
        interactive = true,
        arity = "0..1")
    private String passphrase;

    @Option(names = {"--no-passphrase"},
        description = "Generate key without passphrase")
    private boolean noPassphrase;

    @Option(names = {"-f", "--force"},
        description = "Overwrite existing key")
    private boolean force;

    // ========================================================================
    // Constants
    // ========================================================================

    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);

    // ========================================================================
    // Key Algorithm Enum
    // ========================================================================

    enum KeyAlgorithm {
        ed25519("ed25519", null),
        rsa("rsa", 4096),
        ecdsa("ecdsa", 256);

        final String value;
        final Integer defaultBits;

        KeyAlgorithm(String value, Integer defaultBits) {
            this.value = value;
            this.defaultBits = defaultBits;
        }
    }

    // ========================================================================
    // Main Entry Point
    // ========================================================================

    @Override
    public Integer call() {
        // Determine key name if not specified
        if (name == null || name.isBlank()) {
            if (use == null || use.isBlank()) {
                printer.error(red("Either "), bold("--name"), red(" or "), bold("--use"), red(" must be specified"));
                logger.error("Missing required parameter: --name or --use");
                return 1;
            }
            name = generateKeyName();
        }

        // Determine the target directory based on --use option
        Path sshDir = getSshDirectory();
        Path targetDir = determineTargetDirectory(sshDir);
        Path keyPath = targetDir.resolve(name);
        Path pubKeyPath = targetDir.resolve(name + ".pub");

        // Validate
        if (!validateInputs(sshDir, keyPath)) {
            logger.error("Invalid input parameters for key: {}", name);
            return 1;
        }

        // Check existing
        if (Files.exists(keyPath) || Files.exists(pubKeyPath)) {
            if (!force) {
                printer.error(red("Key already exists: "), bold(keyPath.toString()));
                printer.println(gray("Use "), bold("--force"), gray(" to overwrite"));
                logger.error("Key already exists (use --force to overwrite): {}", name);
                return 1;
            }
            printer.println(yellow("⚠ Overwriting existing key..."));
            logger.warn("Overwriting existing key: {}", name);
        }

        // Ensure target directory exists
        if (!ensureDirectory(targetDir)) {
            logger.error("Failed to create directory: {}", targetDir);
            return 1;
        }

        logger.info("Generating {} key: {}", algorithm.value, name);

        // Generate key
        int result = generateKey(keyPath);

        if (result == 0) {
            // Save metadata
            saveMetadata(keyPath);
            logger.info("Generated {} key: {}", algorithm.value, keyPath);
        } else {
            logger.error("Failed to generate key: {}", name);
        }

        return result;
    }

    // ========================================================================
    // Directory and Path Methods
    // ========================================================================

    private Path getSshDirectory() {
        return Path.of(System.getProperty("user.home"), ".ssh");
    }

    /**
     * Determine the target directory based on the --use option.
     * If --use contains slashes, create subdirectories (e.g., work/project-a).
     */
    private Path determineTargetDirectory(Path sshDir) {
        if (use == null || use.isBlank()) {
            return sshDir;
        }
        return sshDir.resolve(use);
    }

    /**
     * Generate a key name based on the --use option and algorithm.
     * For example: --use work/project-a --algo ed25519 -> id_project-a_ed25519
     */
    private String generateKeyName() {
        // Extract the last part of the use path for the key name
        String usePart = use;
        if (use.contains("/")) {
            String[] parts = use.split("/");
            usePart = parts[parts.length - 1];
        }
        return String.format("id_%s_%s", usePart, algorithm.value);
    }

    // ========================================================================
    // Validation Methods
    // ========================================================================

    private boolean validateInputs(Path sshDir, Path keyPath) {
        // Validate name
        if (name == null || name.isBlank()) {
            printer.error(red("Key name cannot be empty"));
            return false;
        }

        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            printer.error(red("Invalid key name: must not contain path separators"));
            return false;
        }

        // Validate use path if specified
        if (use != null && !use.isBlank()) {
            if (use.contains("..") || use.startsWith("/") || use.startsWith("\\")) {
                printer.error(red("Invalid use path: must be relative and not contain '..'"));
                return false;
            }
        }

        // Validate bits if specified
        if (bits != null) {
            switch (algorithm) {
                case rsa:
                    if (bits < 2048 || bits > 16384) {
                        printer.error(red("RSA key size must be between 2048 and 16384 bits"));
                        return false;
                    }
                    break;
                case ecdsa:
                    if (bits != 256 && bits != 384 && bits != 521) {
                        printer.error(red("ECDSA key size must be 256, 384, or 521 bits"));
                        return false;
                    }
                    break;
                case ed25519:
                    printer.error(red("ED25519 does not support custom key size"));
                    return false;
            }
        }

        return true;
    }

    private boolean ensureDirectory(Path directory) {
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
                // Set proper permissions (700)
                directory.toFile().setReadable(true, true);
                directory.toFile().setWritable(true, true);
                directory.toFile().setExecutable(true, true);
                printer.println(green("✓ Created directory: "), textOf(directory.toString()));
            } catch (IOException e) {
                printer.error(red("Failed to create directory: "), textOf(e.getMessage()));
                return false;
            }
        }
        return true;
    }

    // ========================================================================
    // Key Generation Methods
    // ========================================================================

    private int generateKey(Path keyPath) {
        List<String> command = buildCommand(keyPath);

        // Display header
        printer.println(bold(HEADER_LINE));
        printer.println(bold("  Generating SSH Key"));
        printer.println(bold(HEADER_LINE));
        printer.emptyLine();

        // Display equivalent SSH command
        printer.println(label("Command"), gray(formatCommand(command)));
        printer.println(label("Algorithm"), cyan(algorithm.value.toUpperCase()));
        printer.println(label("Key Path"), textOf(keyPath.toString()));
        printer.emptyLine();

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            // Handle passphrase via environment if empty
            if (noPassphrase || (passphrase != null && passphrase.isEmpty())) {
                pb.environment().put("SSH_ASKPASS", "/bin/true");
                pb.environment().put("SSH_ASKPASS_REQUIRE", "never");
            }

            Process process = pb.start();

            // Read and display output
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    printer.println(gray(line));
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                printer.emptyLine();
                printSuccess(keyPath);
                return 0;
            } else {
                printer.error(red("ssh-keygen failed with exit code: "), bold(String.valueOf(exitCode)));
                return exitCode;
            }

        } catch (IOException e) {
            printer.error(red("Failed to execute ssh-keygen: "), textOf(e.getMessage()));
            printer.println(gray("Make sure ssh-keygen is installed and in PATH"));
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            printer.error(red("Key generation interrupted"));
            return 1;
        }
    }

    private List<String> buildCommand(Path keyPath) {
        List<String> command = new ArrayList<>();

        command.add("ssh-keygen");
        command.add("-t");
        command.add(algorithm.value);
        command.add("-f");
        command.add(keyPath.toString());

        // Add bits if applicable
        Integer keyBits = bits != null ? bits : algorithm.defaultBits;
        if (keyBits != null && algorithm != KeyAlgorithm.ed25519) {
            command.add("-b");
            command.add(keyBits.toString());
        }

        // Add comment
        String keyComment = comment != null ? comment : generateDefaultComment();
        command.add("-C");
        command.add(keyComment);

        // Handle passphrase
        if (noPassphrase) {
            command.add("-N");
            command.add("");
        } else if (passphrase != null) {
            command.add("-N");
            command.add(passphrase);
        }

        return command;
    }

    private String generateDefaultComment() {
        String user = System.getProperty("user.name");
        String host;
        try {
            host = java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            host = "localhost";
        }
        return String.format("%s@%s (%s)", user, host, name);
    }

    // ========================================================================
    // Metadata Methods
    // ========================================================================

    private void saveMetadata(Path keyPath) {
        KeyMetadata metadata = KeyMetadata.create(use, description);

        try {
            metadata.save(keyPath);
            logger.debug("Saved metadata for key: {}", keyPath);
        } catch (IOException e) {
            // Non-fatal: just warn
            printer.println(yellow("⚠ Warning: Could not save key metadata: "), gray(e.getMessage()));
            logger.warn("Failed to save metadata for key {}: {}", keyPath, e.getMessage());
        }
    }

    // ========================================================================
    // Output Methods
    // ========================================================================

    private void printSuccess(Path keyPath) {
        Path pubKeyPath = Path.of(keyPath.toString() + ".pub");

        printer.println(green("✓ SSH key generated successfully!"));
        printer.emptyLine();

        printer.println(bold("Files created:"));
        printer.println("  ", label("Private key"), textOf(keyPath.toString()));
        printer.println("  ", label("Public key"), textOf(pubKeyPath.toString()));

        // Show metadata info if available
        KeyMetadata metadata = KeyMetadata.create(use, description);
        if (use != null && !use.isBlank()) {
            printer.emptyLine();
            printer.println(bold("Metadata:"));
            printer.println("  ", label("Use"), formatUse(metadata.use()));
            if (metadata.hasProject()) {
                printer.println("  ", label("Project"), cyan(formatProject(metadata.project())));
            }
            if (metadata.hasDescription()) {
                printer.println("  ", label("Description"), textOf(description));
            }
        }

        printer.emptyLine();
        printer.println(gray(SEPARATOR_LINE));

        // Show public key content
        try {
            String pubKey = Files.readString(pubKeyPath).trim();
            printer.println(bold("Public key:"));
            printer.emptyLine();
            printer.println(textOf(pubKey));
            printer.emptyLine();
            printer.println(gray(SEPARATOR_LINE));
            printer.println(gray("You can copy this key to add to GitHub, GitLab, or remote servers."));
        } catch (IOException e) {
            // Ignore if we can't read the public key
            logger.debug("Could not read public key: {}", e.getMessage());
        }

        printer.emptyLine();
        printer.println(gray("Run '"), bold("sshman info ", name), gray("' to see key details"));
        printer.println(bold(HEADER_LINE));
    }

    // ========================================================================
    // Formatting Methods
    // ========================================================================

    private Text formatUse(com.sshman.KeyUse use) {
        return switch (use) {
            case WORK -> cyan("Work");
            case PERSONAL -> green("Personal");
            case OTHER -> gray("Other");
        };
    }

    /**
     * Formats a project path for display.
     * Converts "project-a" to "Project A" and "client/acme" to "Client / Acme"
     *
     * @param project the raw project path
     * @return formatted project name
     */
    private String formatProject(String project) {
        if (project == null || project.isBlank()) {
            return "";
        }

        // Split by "/" and format each part
        String[] parts = project.split("/");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(" / ");
            }
            sb.append(formatProjectPart(parts[i]));
        }

        return sb.toString();
    }

    /**
     * Formats a single project part.
     * Converts "project-a" to "Project A"
     *
     * @param part the raw project part
     * @return formatted part
     */
    private String formatProjectPart(String part) {
        if (part == null || part.isBlank()) {
            return "";
        }

        // Replace hyphens and underscores with spaces
        String spaced = part.replace("-", " ").replace("_", " ");

        // Capitalize each word
        String[] words = spaced.split("\\s+");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            String word = words[i];
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }

        return sb.toString();
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
