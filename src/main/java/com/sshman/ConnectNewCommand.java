package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
    name = "connect-new",
    description = "Create a new SSH connection profile",
    mixinStandardHelpOptions = true
)
public class ConnectNewCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    private final ProfileStorage storage = new ProfileStorage();

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            out.println("Create a new SSH connection profile");
            out.println();

            String alias = promptRequired(reader, out, "Alias");
            String hostname = promptRequired(reader, out, "Hostname");
            String username = promptRequired(reader, out, "Username");

            Integer port = promptInteger(reader, out, "Port (default: 22)", 22);

            Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
            String sshKey = promptWithSuggestions(reader, out, "SSH key path (optional)", sshDir);

            if (sshKey != null && !sshKey.isEmpty()) {
                Path keyPath = Path.of(sshKey);
                if (!keyPath.isAbsolute()) {
                    keyPath = sshDir.resolve(sshKey);
                }

                if (!Files.exists(keyPath)) {
                    err.println("Warning: SSH key not found at " + keyPath);
                    String confirm = promptRequired(reader, out, "Continue anyway? (y/n)");
                    if (!confirm.equalsIgnoreCase("y")) {
                        out.println("Cancelled");
                        return 1;
                    }
                }
                sshKey = keyPath.toString();
            }

            Profile profile = new Profile(alias, hostname, username, port, sshKey);

            storage.addProfile(profile);

            out.println();
            out.println("Profile created successfully!");
            out.println("  Alias:    " + alias);
            out.println("  Hostname: " + hostname);
            out.println("  Username: " + username);
            out.println("  Port:     " + port);
            if (sshKey != null && !sshKey.isEmpty()) {
                out.println("  SSH Key:  " + sshKey);
            }
            out.println();
            out.println("To connect, run:");
            out.println("  sshman connect " + alias);

            return 0;

        } catch (IOException e) {
            err.println("Error reading input: " + e.getMessage());
            return 1;
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    private String promptRequired(BufferedReader reader, PrintWriter out, String prompt) throws IOException {
        String value;
        do {
            out.print(prompt + ": ");
            out.flush();
            value = reader.readLine();
            if (value == null || value.trim().isEmpty()) {
                out.println("This field is required. Please enter a value.");
            }
        } while (value == null || value.trim().isEmpty());
        return value.trim();
    }

    private Integer promptInteger(BufferedReader reader, PrintWriter out, String prompt, Integer defaultValue) throws IOException {
        out.print(prompt + ": ");
        out.flush();
        String value = reader.readLine();

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            out.println("Invalid number, using default: " + defaultValue);
            return defaultValue;
        }
    }

    private String promptWithSuggestions(BufferedReader reader, PrintWriter out, String prompt, Path sshDir) throws IOException {
        if (Files.exists(sshDir) && Files.isDirectory(sshDir)) {
            out.println("Available SSH keys in " + sshDir + ":");
            try (var files = Files.list(sshDir)) {
                files.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".pub"))
                    .filter(p -> !p.getFileName().toString().equals("config"))
                    .filter(p -> !p.getFileName().toString().equals("known_hosts"))
                    .filter(p -> !p.getFileName().toString().equals("authorized_keys"))
                    .sorted()
                    .forEach(p -> out.println("  - " + p.getFileName()));
            } catch (IOException e) {
            }
            out.println();
        }

        out.print(prompt + ": ");
        out.flush();
        String value = reader.readLine();
        return value != null ? value.trim() : "";
    }
}
