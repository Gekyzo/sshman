package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "generate",
    description = "Generate a new SSH key",
    mixinStandardHelpOptions = true
)
public class GenerateCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

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

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        // Determine key name if not specified
        if (name == null || name.isBlank()) {
            if (use == null || use.isBlank()) {
                err.println("Either --name or --use must be specified");
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
            return 1;
        }

        // Check existing
        if (Files.exists(keyPath) || Files.exists(pubKeyPath)) {
            if (!force) {
                err.printf("Key already exists: %s%n", keyPath);
                err.println("Use --force to overwrite");
                return 1;
            }
            out.println("Overwriting existing key...");
        }

        // Ensure target directory exists
        if (!ensureDirectory(targetDir)) {
            return 1;
        }

        // Generate key
        return generateKey(keyPath);
    }

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
     * For example: --use work/project-a --algo ed25519 -> id_ed25519
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

    private boolean validateInputs(Path sshDir, Path keyPath) {
        PrintWriter err = spec.commandLine().getErr();

        // Validate name
        if (name == null || name.isBlank()) {
            err.println("Key name cannot be empty");
            return false;
        }

        if (name.contains("/") || name.contains("\\") || name.contains("..")) {
            err.println("Invalid key name: must not contain path separators");
            return false;
        }

        // Validate use path if specified
        if (use != null && !use.isBlank()) {
            if (use.contains("..") || use.startsWith("/") || use.startsWith("\\")) {
                err.println("Invalid use path: must be relative and not contain '..'");
                return false;
            }
        }

        // Validate bits if specified
        if (bits != null) {
            switch (algorithm) {
                case rsa:
                    if (bits < 2048 || bits > 16384) {
                        err.println("RSA key size must be between 2048 and 16384 bits");
                        return false;
                    }
                    break;
                case ecdsa:
                    if (bits != 256 && bits != 384 && bits != 521) {
                        err.println("ECDSA key size must be 256, 384, or 521 bits");
                        return false;
                    }
                    break;
                case ed25519:
                    err.println("ED25519 does not support custom key size");
                    return false;
            }
        }

        return true;
    }

    private boolean ensureDirectory(Path directory) {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
                // Set proper permissions (700)
                directory.toFile().setReadable(true, true);
                directory.toFile().setWritable(true, true);
                directory.toFile().setExecutable(true, true);
                out.println("Created directory: " + directory);
            } catch (IOException e) {
                err.println("Failed to create directory: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private int generateKey(Path keyPath) {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        List<String> command = buildCommand(keyPath);

        out.printf("Generating %s key: %s%n", algorithm.value.toUpperCase(), keyPath);
        out.println();

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
                    out.println(line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                out.println();
                printSuccess(keyPath);
                return 0;
            } else {
                err.println("ssh-keygen failed with exit code: " + exitCode);
                return exitCode;
            }

        } catch (IOException e) {
            err.println("Failed to execute ssh-keygen: " + e.getMessage());
            err.println("Make sure ssh-keygen is installed and in PATH");
            return 1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            err.println("Key generation interrupted");
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

    private void printSuccess(Path keyPath) {
        PrintWriter out = spec.commandLine().getOut();

        Path pubKeyPath = Path.of(keyPath.toString() + ".pub");

        out.println("✓ SSH key generated successfully!");
        out.println();
        out.println("Files created:");
        out.printf("  Private key: %s%n", keyPath);
        out.printf("  Public key:  %s%n", pubKeyPath);
        out.println();

        // Show public key content
        try {
            String pubKey = Files.readString(pubKeyPath).trim();
            out.println("Public key:");
            out.println(pubKey);
            out.println();
            out.println("You can copy this key to add to GitHub, GitLab, or remote servers.");
        } catch (IOException e) {
            // Ignore if we can't read the public key
        }
    }
}
