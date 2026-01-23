package com.sshman.commands;

import com.sshman.Profile;
import com.sshman.ProfileStorage;
import com.sshman.ProfileStorageAware;
import com.sshman.ProfileStorageProvider;
import com.sshman.constants.SshManConstants;
import com.sshman.utils.printer.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.sshman.constants.SshManConstants.*;
import static com.sshman.utils.printer.Text.*;

@Command(
    name = "connect-new",
    aliases = {"cn"},
    description = "Create a new SSH connection profile",
    mixinStandardHelpOptions = true
)
public class ConnectNewCommand implements Callable<Integer>, ProfileStorageAware {

    @Mixin
    private Printer printer;

    private ProfileStorageProvider storageProvider = ProfileStorageProvider.DEFAULT;

    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);

    @Override
    public void setProfileStorageProvider(ProfileStorageProvider provider) {
        this.storageProvider = provider;
    }

    @Override
    public Integer call() {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            // Print header
            printer.println(bold(HEADER_LINE));
            printer.println(bold("  Create New SSH Connection Profile"));
            printer.println(bold(HEADER_LINE));
            printer.emptyLine();

            String alias = promptRequired(reader, "Alias");
            String hostname = promptRequired(reader, "Hostname");
            String username = promptRequired(reader, "Username");

            Integer port = promptInteger(reader, "Port (default: 22)", 22);

            Path sshDir = Path.of(System.getProperty(SystemProperties.USER_HOME), DirectoryNames.SSH);
            String sshKey = promptWithSuggestions(reader, "SSH key path (optional)", sshDir);

            if (sshKey != null && !sshKey.isEmpty()) {
                Path keyPath = Path.of(sshKey);
                if (!keyPath.isAbsolute()) {
                    keyPath = sshDir.resolve(sshKey);
                }

                if (!Files.exists(keyPath)) {
                    printer.println(yellow("⚠ Warning: "), textOf("SSH key not found at "), gray(keyPath.toString()));
                    String confirm = promptRequired(reader, "Continue anyway? (y/n)");
                    if (!confirm.equalsIgnoreCase("y")) {
                        printer.println(gray("Cancelled"));
                        return 1;
                    }
                }
                sshKey = keyPath.toString();
            }

            Profile profile = new Profile(alias, hostname, username, port, sshKey);

            storageProvider.get().addProfile(profile);

            // Print success message
            printer.emptyLine();
            printer.println(gray(SEPARATOR_LINE));
            printer.emptyLine();
            printer.println(green("✓ "), bold("Profile created successfully!"));
            printer.emptyLine();
            printer.println(label("Alias"), cyan(alias));
            printer.println(label("Hostname"), textOf(hostname));
            printer.println(label("Username"), textOf(username));
            printer.println(label("Port"), textOf(String.valueOf(port)));
            if (sshKey != null && !sshKey.isEmpty()) {
                printer.println(label("SSH Key"), gray(sshKey));
            }
            printer.emptyLine();
            printer.println(gray("To connect, run:"));
            printer.println("  ", cyan("sshman connect %s", alias));
            printer.emptyLine();
            printer.println(bold(HEADER_LINE));

            // Offer to test the connection
            printer.emptyLine();
            printer.prompt(cyan("Test connection now? (y/n): "));
            String testConnection = reader.readLine();

            if (testConnection != null && testConnection.trim().equalsIgnoreCase("y")) {
                return testConnectionAndSuggestFix(reader, profile);
            }

            return 0;

        } catch (IOException e) {
            printer.error(red("Error reading input: "), textOf(e.getMessage()));
            return 1;
        } catch (IllegalArgumentException e) {
            printer.error(red("Error: "), textOf(e.getMessage()));
            return 1;
        }
    }

    // ========================================================================
    // Prompt Methods
    // ========================================================================

    private String promptRequired(BufferedReader reader, String prompt) throws IOException {
        String value;
        do {
            printer.prompt(cyan(prompt + ": "));
            value = reader.readLine();
            if (value == null || value.trim().isEmpty()) {
                printer.println(yellow("This field is required. Please enter a value."));
            }
        } while (value == null || value.trim().isEmpty());
        return value.trim();
    }

    private Integer promptInteger(BufferedReader reader,
                                  String prompt,
                                  Integer defaultValue) throws IOException {
        printer.prompt(cyan(prompt + ": "));
        String value = reader.readLine();

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            printer.println(gray("Invalid number, using default: "), textOf(String.valueOf(defaultValue)));
            return defaultValue;
        }
    }

    private String promptWithSuggestions(BufferedReader reader,
                                         String prompt,
                                         Path sshDir) throws IOException {
        if (Files.exists(sshDir) && Files.isDirectory(sshDir)) {
            printer.println(gray("Available SSH keys in "), textOf(sshDir.toString()), gray(":"));
            try (var files = Files.list(sshDir)) {
                files.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(FileExtensions.PUBLIC_KEY))
                    .filter(p -> !p.getFileName().toString().equals(FileNames.CONFIG))
                    .filter(p -> !p.getFileName().toString().equals(FileNames.KNOWN_HOSTS))
                    .filter(p -> !p.getFileName().toString().equals(FileNames.AUTHORIZED_KEYS))
                    .sorted()
                    .forEach(p -> printer.println("  ", cyan("- "), textOf(p.getFileName().toString())));
            } catch (IOException e) {
            }
            printer.emptyLine();
        }

        printer.prompt(cyan(prompt + ": "));
        String value = reader.readLine();
        return value != null ? value.trim() : "";
    }

    // ========================================================================
    // Connection Test Methods
    // ========================================================================

    private int testConnectionAndSuggestFix(BufferedReader reader, Profile profile) throws IOException {
        printer.emptyLine();
        printer.println(gray("Testing connection to "), cyan("%s@%s", profile.username(), profile.hostname()), gray("..."));

        String sshCommand = profile.toSshCommand() + " -o ConnectTimeout=10 -o BatchMode=yes exit";

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("sh", "-c", sshCommand);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            printer.emptyLine();
            if (exitCode == 0) {
                printer.println(green("✓ "), textOf("Connection successful!"));
                return 0;
            } else {
                printer.println(red("✗ "), textOf("Connection failed (exit code: " + exitCode + ")"));
                return suggestSshCopyId(reader, profile);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            printer.error(red("Connection test interrupted"));
            return 1;
        } catch (IOException e) {
            printer.error(red("Connection test failed: "), textOf(e.getMessage()));
            return suggestSshCopyId(reader, profile);
        }
    }

    private int suggestSshCopyId(BufferedReader reader, Profile profile) throws IOException {
        String sshKey = profile.sshKey();

        if (sshKey == null || sshKey.isEmpty()) {
            printer.emptyLine();
            printer.println(gray("Tip: Configure an SSH key for passwordless authentication."));
            return 1;
        }

        // Derive public key path
        String pubKeyPath = sshKey.endsWith(FileExtensions.PUBLIC_KEY) ? sshKey : sshKey + FileExtensions.PUBLIC_KEY;

        if (!Files.exists(Path.of(pubKeyPath))) {
            printer.emptyLine();
            printer.println(yellow("⚠ "), textOf("Public key not found at: "), gray(pubKeyPath));
            printer.println(gray("Tip: Generate a public key or check the key path."));
            return 1;
        }

        printer.emptyLine();
        printer.println(gray("This may be a first-time connection. You can copy your public key to the server."));
        printer.emptyLine();
        printer.prompt(cyan("Copy public key to server? (y/n): "));
        String copyKey = reader.readLine();

        if (copyKey == null || !copyKey.trim().equalsIgnoreCase("y")) {
            printer.emptyLine();
            printer.println(gray("You can manually run:"));
            printer.println("  ", cyan("ssh-copy-id -i %s %s@%s", pubKeyPath, profile.username(), profile.hostname()));
            return 1;
        }

        return executeSshCopyId(pubKeyPath, profile);
    }

    private int executeSshCopyId(String pubKeyPath, Profile profile) {
        printer.emptyLine();
        printer.println(gray("Running ssh-copy-id..."));
        printer.emptyLine();

        String target = profile.username() + "@" + profile.hostname();

        List<String> command = new ArrayList<>();
        command.add("ssh-copy-id");
        command.add("-i");
        command.add(pubKeyPath);
        if (profile.port() != null && profile.port() != 22) {
            command.add("-p");
            command.add(String.valueOf(profile.port()));
        }
        command.add(target);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            printer.emptyLine();
            if (exitCode == 0) {
                printer.println(green("✓ "), textOf("Public key copied successfully!"));
                printer.println(gray("You can now connect with: "), cyan("sshman connect %s", profile.alias()));
                return 0;
            } else {
                printer.println(red("✗ "), textOf("Failed to copy public key (exit code: " + exitCode + ")"));
                return 1;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            printer.error(red("ssh-copy-id interrupted"));
            return 1;
        } catch (IOException e) {
            printer.error(red("Failed to run ssh-copy-id: "), textOf(e.getMessage()));
            printer.println(gray("Make sure ssh-copy-id is installed and in PATH"));
            return 1;
        }
    }

}
