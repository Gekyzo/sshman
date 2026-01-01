package com.sshman.commands;

import com.sshman.Profile;
import com.sshman.ProfileStorage;
import com.sshman.constants.SshManConstants;
import com.sshman.utils.printer.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import static com.sshman.constants.SshManConstants.*;
import static com.sshman.utils.printer.Text.*;

@Command(
    name = "connect-new",
    description = "Create a new SSH connection profile",
    mixinStandardHelpOptions = true
)
public class ConnectNewCommand implements Callable<Integer> {

    @Mixin
    private Printer printer;

    private final ProfileStorage storage = new ProfileStorage();

    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);

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

            storage.addProfile(profile);

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

}
