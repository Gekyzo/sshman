package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "list",
    description = "List all SSH keys in ~/.ssh",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Option(names = {"-l", "--long"}, description = "Show detailed information")
    private boolean longFormat;

    @Option(names = {"-a", "--all"}, description = "Include all key files (not just private keys)")
    private boolean showAll;

    @Option(names = {"--path"}, description = "Custom SSH directory path", defaultValue = "")
    private String customPath;

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    // Private key header bytes: "-----BEGIN"
    private static final byte[] PRIVATE_KEY_MAGIC = "-----BEGIN".getBytes();

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        Path sshDir = getSshDirectory();

        if (!Files.exists(sshDir)) {
            err.println("SSH directory not found: " + sshDir);
            return 1;
        }

        if (!Files.isDirectory(sshDir)) {
            err.println("Not a directory: " + sshDir);
            return 1;
        }

        List<KeyInfo> keys = findKeys(sshDir);

        if (keys.isEmpty()) {
            out.println("No SSH keys found in " + sshDir);
            return 0;
        }

        printKeys(keys);
        return 0;
    }

    private Path getSshDirectory() {
        if (customPath != null && !customPath.isEmpty()) {
            return Path.of(customPath);
        }
        return Path.of(System.getProperty("user.home"), ".ssh");
    }

    private List<KeyInfo> findKeys(Path sshDir) {
        PrintWriter err = spec.commandLine().getErr();
        List<KeyInfo> keys = new ArrayList<>();

        try (Stream<Path> files = Files.list(sshDir)) {
            files.filter(Files::isRegularFile)
                .filter(this::isKeyFile)
                .sorted()
                .forEach(path -> {
                    try {
                        keys.add(new KeyInfo(path));
                    } catch (IOException e) {
                        // Skip files we can't read
                    }
                });
        } catch (IOException e) {
            err.println("Error reading SSH directory: " + e.getMessage());
        }

        return keys;
    }

    private boolean isKeyFile(Path path) {
        String fileName = path.getFileName().toString();

        // Skip known non-key files
        if (fileName.equals("config") ||
            fileName.equals("known_hosts") ||
            fileName.equals("known_hosts.old") ||
            fileName.equals("authorized_keys") ||
            fileName.endsWith(".db") ||
            fileName.endsWith(".old") ||
            fileName.endsWith(".bak") ||
            fileName.startsWith(".")) {
            return false;
        }

        if (showAll) {
            return fileName.endsWith(".pub") || isPrivateKey(path);
        }

        return isPrivateKey(path);
    }

    private boolean isPrivateKey(Path path) {
        String fileName = path.getFileName().toString();

        if (fileName.endsWith(".pub")) {
            return false;
        }

        // Read first bytes safely (binary-safe)
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

    private void printKeys(List<KeyInfo> keys) {
        if (longFormat) {
            printLongFormat(keys);
        } else {
            printShortFormat(keys);
        }
    }

    private void printShortFormat(List<KeyInfo> keys) {
        PrintWriter out = spec.commandLine().getOut();

        out.println("SSH Keys in " + getSshDirectory() + ":");
        out.println();

        for (KeyInfo key : keys) {
            String pubIndicator = key.hasPublicKey ? " (+ .pub)" : "";
            out.printf("  %s%s%n", key.name, pubIndicator);
        }

        out.println();
        out.printf("Total: %d key(s)%n", keys.size());
    }

    private void printLongFormat(List<KeyInfo> keys) {
        PrintWriter out = spec.commandLine().getOut();

        out.println("SSH Keys in " + getSshDirectory() + ":");
        out.println();

        out.printf("  %-20s %-10s %-10s %-16s %s%n",
            "NAME", "TYPE", "PERMS", "MODIFIED", "PUBLIC");
        out.println("  " + "-".repeat(70));

        for (KeyInfo key : keys) {
            out.printf("  %-20s %-10s %-10s %-16s %s%n",
                truncate(key.name, 20),
                key.type,
                key.permissions,
                key.modified,
                key.hasPublicKey ? "yes" : "no"
            );
        }

        out.println();
        out.printf("Total: %d key(s)%n", keys.size());
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private class KeyInfo {
        final String name;
        final String type;
        final String permissions;
        final String modified;
        final boolean hasPublicKey;

        KeyInfo(Path path) throws IOException {
            this.name = path.getFileName().toString();
            this.type = detectKeyType(path);
            this.permissions = getPermissions(path);
            this.modified = getModifiedTime(path);
            this.hasPublicKey = Files.exists(Path.of(path.toString() + ".pub"));
        }

        private String detectKeyType(Path path) {
            try (InputStream is = Files.newInputStream(path)) {
                byte[] header = new byte[128];
                int bytesRead = is.read(header);

                if (bytesRead <= 0) {
                    return "unknown";
                }

                String headerStr = new String(header, 0, bytesRead);

                if (headerStr.contains("RSA")) {
                    return "RSA";
                } else if (headerStr.contains("EC")) {
                    return "ECDSA";
                } else if (headerStr.contains("OPENSSH")) {
                    return "ED25519";
                } else if (headerStr.contains("DSA")) {
                    return "DSA";
                }

                return "unknown";
            } catch (IOException e) {
                return "error";
            }
        }

        private String getPermissions(Path path) {
            try {
                return PosixFilePermissions.toString(
                    Files.getPosixFilePermissions(path)
                );
            } catch (IOException | UnsupportedOperationException e) {
                return "n/a";
            }
        }

        private String getModifiedTime(Path path) {
            try {
                Instant instant = Files.getLastModifiedTime(path).toInstant();
                return DATE_FORMAT.format(instant);
            } catch (IOException e) {
                return "unknown";
            }
        }
    }
}
