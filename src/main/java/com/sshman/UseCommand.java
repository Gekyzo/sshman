package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "use",
    aliases = {"set"},
    description = "Start ssh-agent and add the specified SSH key",
    mixinStandardHelpOptions = true
)
public class UseCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(
        index = "0",
        description = "Key name (filename in ~/.ssh)",
        completionCandidates = KeyNameCompletionCandidates.class
    )
    private String keyName;

    @Option(names = {"--path"}, description = "Custom SSH directory path")
    private String customPath;

    @Option(names = {"-t", "--time"}, description = "Lifetime of identities in seconds (default: forever)")
    private Integer lifetime;

    @Option(names = {"-q", "--quiet"}, description = "Quiet mode - only output eval command")
    private boolean quiet;

    // Private key header bytes: "-----BEGIN"
    private static final byte[] PRIVATE_KEY_MAGIC = "-----BEGIN".getBytes();

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        Path sshDir = getSshDirectory();

        // Resolve the key path
        Path keyPath = sshDir.resolve(keyName);

        // Check if key exists
        if (!Files.exists(keyPath)) {
            // Try to find the key recursively
            Path foundKey = findKeyRecursive(sshDir, keyName);
            if (foundKey != null) {
                keyPath = foundKey;
            } else {
                err.println("Key not found: " + keyName);
                err.println();
                err.println("Available keys:");
                listAvailableKeys(sshDir, err);
                return 1;
            }
        }

        // Verify it's a private key
        if (!isPrivateKey(keyPath)) {
            err.println("Not a valid private key: " + keyPath);
            return 1;
        }

        // Generate the commands to start ssh-agent and add the key
        return generateAgentCommands(keyPath, out, err);
    }

    private Path getSshDirectory() {
        if (customPath != null && !customPath.isEmpty()) {
            return Path.of(customPath);
        }
        return Path.of(System.getProperty("user.home"), ".ssh");
    }

    /**
     * Recursively search for a key by filename in the SSH directory.
     */
    private Path findKeyRecursive(Path baseDir, String keyName) {
        try (Stream<Path> stream = Files.walk(baseDir)) {
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

    private int generateAgentCommands(Path keyPath, PrintWriter out, PrintWriter err) {
        if (!quiet) {
            out.println("# Starting ssh-agent and adding key: " + keyPath);
            out.println("# Copy and paste the following command, or run:");
            out.println("# eval \"$(sshman use " + keyName + " --quiet)\"");
            out.println();
        }

        // Check if ssh-agent is already running
        String sshAuthSock = System.getenv("SSH_AUTH_SOCK");
        String sshAgentPid = System.getenv("SSH_AGENT_PID");

        if (sshAuthSock != null && !sshAuthSock.isEmpty() &&
            sshAgentPid != null && !sshAgentPid.isEmpty()) {
            if (!quiet) {
                out.println("# ssh-agent is already running (SSH_AUTH_SOCK=" + sshAuthSock + ")");
                out.println("# Clearing existing keys and adding new key...");
                out.println();
            }
            // Clear existing keys and add the new one
            out.print("ssh-add -D && ssh-add");
            if (lifetime != null) {
                out.print(" -t " + lifetime);
            }
            out.println(" " + keyPath);
        } else {
            // Need to start ssh-agent
            if (!quiet) {
                out.println("# No ssh-agent running, starting a new one...");
                out.println();
            }
            
            // Generate the eval command
            out.print("eval \"$(ssh-agent -s)\" && ssh-add");
            if (lifetime != null) {
                out.print(" -t " + lifetime);
            }
            out.println(" " + keyPath);
        }

        return 0;
    }

    private void listAvailableKeys(Path sshDir, PrintWriter err) {
        try (Stream<Path> files = Files.walk(sshDir)) {
            files.filter(Files::isRegularFile)
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
     * This enables tab-completion for SSH keys.
     */
    public static class KeyNameCompletionCandidates implements Iterable<String> {
        @Override
        public java.util.Iterator<String> iterator() {
            List<String> keys = new ArrayList<>();
            Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");

            if (!Files.exists(sshDir) || !Files.isDirectory(sshDir)) {
                return keys.iterator();
            }

            try (Stream<Path> files = Files.walk(sshDir)) {
                files.filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().endsWith(".pub"))
                    .filter(p -> !p.getFileName().toString().equals("config"))
                    .filter(p -> !p.getFileName().toString().equals("known_hosts"))
                    .filter(p -> !p.getFileName().toString().equals("authorized_keys"))
                    .filter(UseCommand::isPrivateKeyStatic)
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
