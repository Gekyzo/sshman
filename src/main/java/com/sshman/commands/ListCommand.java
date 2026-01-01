package com.sshman.commands;

import com.sshman.KeyInfo;
import com.sshman.utils.Strings;
import com.sshman.utils.printer.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static com.sshman.utils.printer.Text.*;
import static picocli.CommandLine.Mixin;

@Command(
    name = "list",
    description = "List all SSH keys in ~/.ssh",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    private final Set<KeyInfo> activeKeys = new HashSet<>();
    private final Set<KeyInfo> archivedKeys = new HashSet<>();

    @Mixin
    private Printer printer;

    @Option(names = {"-l", "--long"}, description = "Show detailed information")
    private boolean longFormat;

    @Option(names = {"-a", "--all"}, description = "Include all key files (not just private keys)")
    private boolean showAll;

    @Option(names = {"--path"}, description = "Custom SSH directory path", defaultValue = "")
    private String customPath;

    // Private key header bytes: "-----BEGIN"
    private static final byte[] PRIVATE_KEY_MAGIC = "-----BEGIN".getBytes();

    @Override
    public Integer call() {
        Path sshDir = getSshDirectory();

        if (!Files.exists(sshDir)) {
            printer.error("SSH directory not found: " + sshDir);
            return 1;
        }

        if (!Files.isDirectory(sshDir)) {
            printer.error("Not a directory: " + sshDir);
            return 1;
        }

        List<KeyInfo> keys = findKeys(sshDir);

        if (keys.isEmpty()) {
            printer.println("No SSH keys found in " + sshDir);
            return 0;
        }

        for (KeyInfo key : keys) {
            if (key.relativePath().startsWith("archived")) {
                archivedKeys.add(key);
            } else {
                activeKeys.add(key);
            }
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
        PrintWriter err = printer.err();
        List<KeyInfo> keys = new ArrayList<>();

        try {
            findKeysRecursive(sshDir, sshDir, keys);
        } catch (IOException e) {
            err.println("Error reading SSH directory: " + e.getMessage());
        }

        return keys;
    }

    private void findKeysRecursive(Path baseDir, Path currentDir, List<KeyInfo> keys) throws IOException {
        try (Stream<Path> entries = Files.list(currentDir)) {
            entries.sorted().forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        // Recursively scan subdirectories
                        findKeysRecursive(baseDir, path, keys);
                    } else if (Files.isRegularFile(path) && isKeyFile(path)) {
                        keys.add(KeyInfo.of(baseDir, path));
                    }
                } catch (IOException e) {
                    // Skip files/directories we can't read
                }
            });
        }
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

        for (KeyInfo key : keys) {
            if (key.relativePath().startsWith("archived")) {
                archivedKeys.add(key);
            } else {
                activeKeys.add(key);
            }
        }

        printer.println("SSH Keys in " + getSshDirectory() + ":");
        printer.emptyLine();

        for (KeyInfo key : activeKeys) {
            String pubIndicator = key.hasPublicKey() ? " (+ .pub)" : "";

            Path path = Path.of(key.relativePath());
            Path parent = path.getParent();
            String filename = path.getFileName().toString();

            if (parent != null) {
                printer.println("  ", textOf(parent + "/"), bold(filename), pubIndicator);
            } else {
                printer.println("  ", bold(filename), gray(pubIndicator));
            }
        }

        printer.print().padded().ln(" Archived keys:");

        for (KeyInfo key : archivedKeys) {
            String pubIndicator = key.hasPublicKey() ? " (+ .pub)" : "";
            printer.println("  ", gray(key.relativePath()), gray(pubIndicator));
        }

        printer.print().padded().f("Total: %d key(s) (%d active, %s)%n",
            keys.size(),
            activeKeys.size(),
            gray("%d archived", archivedKeys.size())
        );
    }

    private void printLongFormat(List<KeyInfo> keys) {

        printer.print().after().ln("SSH Keys in " + getSshDirectory() + ":");

        printer.printf("  %-35s %-10s %-10s %-16s %s%n",
            "PATH", "TYPE", "PERMS", "MODIFIED", "PUBLIC");
        printer.println("  " + "-".repeat(85));

        for (KeyInfo key : activeKeys) {

            Path path = Path.of(key.relativePath());
            Path parent = path.getParent();
            String filename = path.getFileName().toString();

            printer.printf("  %-35s %-10s %-10s %-16s %s%n",
                Strings.truncate(parent + "/" + filename, 35),
                key.type(),
                key.permissions(),
                key.modified(),
                key.hasPublicKey() ? "yes" : "no"
            );
        }

        printer.print().padded().ln(" Archived keys:");

        for (KeyInfo key : archivedKeys) {
            printer.printf("  %-35s %-10s %-10s %-16s %s%n",
                Strings.truncate(key.relativePath(), 35),
                key.type(),
                key.permissions(),
                key.modified(),
                key.hasPublicKey() ? "yes" : "no"
            );
        }

        printer.emptyLine();
        printer.printf("Total: %d key(s) (%d active, %s)%n",
            keys.size(),
            activeKeys.size(),
            gray("%d archived", archivedKeys.size())
        );
    }
}
