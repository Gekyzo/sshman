package com.sshman.commands;

import com.sshman.KeyMetadata;
import com.sshman.KeyUse;
import com.sshman.utils.SshKeyUtils;
import com.sshman.utils.Strings;
import com.sshman.utils.printer.Printer;
import com.sshman.utils.printer.Text;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Callable;

import static com.sshman.utils.Dates.DATE_FORMAT;
import static com.sshman.utils.printer.Text.*;

@Command(
    name = "info",
    description = "Show detailed information about a specific key",
    mixinStandardHelpOptions = true
)
public class InfoCommand implements Callable<Integer> {

    @Mixin
    private Printer printer;

    @Parameters(index = "0", description = "Key name (filename in ~/.ssh)")
    private String keyName;

    @Option(names = {"-p", "--public"}, description = "Show public key content")
    private boolean showPublicKey;

    @Option(names = {"-f", "--fingerprint"}, description = "Show key fingerprint")
    private boolean showFingerprint;

    @Option(names = {"--path"}, description = "Custom SSH directory path")
    private String customPath;

    // Constants for visual formatting
    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);

    @Override
    public Integer call() {
        Path sshDir = getSshDirectory();

        // Try to resolve the key (supports paths like "work/project-a/id_ed25519")
        Path keyPath = sshDir.resolve(keyName);
        Path pubKeyPath = Path.of(keyPath + ".pub");

        // Check if key exists
        if (!Files.exists(keyPath)) {
            // Maybe user specified the .pub file
            if (keyName.endsWith(".pub")) {
                String baseName = keyName.substring(0, keyName.length() - 4);
                keyPath = sshDir.resolve(baseName);
                pubKeyPath = sshDir.resolve(keyName);
                if (!Files.exists(keyPath)) {
                    keyPath = null; // Only public key exists
                }
            } else {
                // Try to find the key recursively
                Path foundKey = findKeyRecursive(sshDir, keyName);
                if (foundKey != null) {
                    keyPath = foundKey;
                    pubKeyPath = Path.of(foundKey + ".pub");
                } else {
                    printer.error(red("Key not found: "), bold(keyName));
                    printer.emptyLine();
                    printer.println(yellow("Available keys:"));
                    SshKeyUtils.listAvailableKeys(sshDir, printer,
                        SshKeyUtils.ListKeysOptions.defaults()
                            .excludeMetaFiles(true));
                    return 1;
                }
            }
        }

        printKeyInfo(keyPath, pubKeyPath);
        return 0;
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
        try (var stream = Files.walk(baseDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().equals(keyName))
                .filter(SshKeyUtils::isPrivateKey)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    // ========================================================================
    // Print Methods
    // ========================================================================

    private void printKeyInfo(Path keyPath, Path pubKeyPath) {
        // Header
        printer.println(bold(HEADER_LINE));
        printer.println(bold("  SSH Key Information"));
        printer.println(bold(HEADER_LINE));
        printer.emptyLine();

        // Basic info
        printer.println(label("Name"), bold(keyName));

        // Load and display metadata (including use and project)
        if (keyPath != null) {
            printMetadata(keyPath);
        }

        if (keyPath != null && Files.exists(keyPath)) {
            printPrivateKeyInfo(keyPath);
        } else {
            printer.println(label("Private Key"), gray("(not found)"));
        }

        if (Files.exists(pubKeyPath)) {
            printPublicKeyInfo(pubKeyPath);
        } else {
            printer.println(label("Public Key"), gray("(not found)"));
        }

        // Fingerprint (always useful)
        if (Files.exists(pubKeyPath)) {
            printer.emptyLine();
            printFingerprint(pubKeyPath);
        }

        // Show public key content if requested
        if (showPublicKey && Files.exists(pubKeyPath)) {
            printer.emptyLine();
            printPublicKeyContent(pubKeyPath);
        }

        printer.emptyLine();
        printer.println(bold(HEADER_LINE));
    }

    private void printMetadata(Path keyPath) {
        Optional<KeyMetadata> metadataOpt = KeyMetadata.load(keyPath);

        if (metadataOpt.isEmpty()) {
            return;
        }

        KeyMetadata metadata = metadataOpt.get();

        // Use category
        KeyUse use = metadata.use();
        printer.println(label("Use"), formatUse(use));

        // Project/subfolder (if present)
        if (metadata.hasProject()) {
            printer.println(label("Project"), cyan(formatProject(metadata.project())));
        }

        // Description (if present)
        if (metadata.hasDescription()) {
            printer.println(label("Description"), textOf(metadata.description()));
        }

        // Created info (if present)
        if (metadata.createdAt() != null) {
            printer.println(label("Created"), textOf(DATE_FORMAT.format(metadata.createdAt())));
        }

        if (metadata.createdBy() != null && !metadata.createdBy().isBlank()) {
            printer.println(label("Created By"), textOf(metadata.createdBy()));
        }

        printer.emptyLine();
    }

    private void printPrivateKeyInfo(Path keyPath) {
        printer.println(label("Private Key"), textOf(keyPath.toString()));

        try {
            // Type
            String keyType = detectKeyType(keyPath);
            printer.println(label("Type"), cyan(keyType));

            // Permissions
            String perms = getPermissions(keyPath);
            printer.println(label("Permissions"), textOf(perms), formatPermissionWarning(perms));

            // Size
            long size = Files.size(keyPath);
            printer.println(label("Size"), textOf(formatSize(size)));

            // Modified time
            Instant modified = Files.getLastModifiedTime(keyPath).toInstant();
            printer.println(label("Modified"), textOf(DATE_FORMAT.format(modified)));

            // Check if encrypted
            boolean encrypted = isKeyEncrypted(keyPath);
            printer.println(
                label("Encrypted"),
                encrypted ? green("yes (passphrase protected)") : yellow("no")
            );

        } catch (IOException e) {
            printer.println("  ", red("(error reading key details)"));
        }
    }

    private void printPublicKeyInfo(Path pubKeyPath) {
        printer.emptyLine();
        printer.println(label("Public Key"), textOf(pubKeyPath.toString()));

        try {
            String content = Files.readString(pubKeyPath).trim();
            String[] parts = content.split("\\s+");

            if (parts.length >= 1) {
                printer.println(label("Algorithm"), green(formatAlgorithm(parts[0])));
            }

            if (parts.length >= 3) {
                printer.println(label("Comment"), textOf(parts[2]));
            }

            // Key length (approximate from base64)
            if (parts.length >= 2) {
                int bitLength = estimateKeyBits(parts[0], parts[1]);
                if (bitLength > 0) {
                    printer.println(label("Key Bits"), textOf("~%d", bitLength));
                }
            }

        } catch (IOException e) {
            printer.println("  ", red("(error reading public key)"));
        }
    }

    private void printFingerprint(Path pubKeyPath) {
        printer.println(bold("Fingerprints:"));

        // SHA256 fingerprint (default)
        String sha256 = getFingerprint(pubKeyPath, "sha256");
        if (sha256 != null) {
            printer.println("  ", gray("SHA256:"), "     ", textOf(sha256));
        }

        // MD5 fingerprint (legacy, but sometimes needed)
        if (showFingerprint) {
            String md5 = getFingerprint(pubKeyPath, "md5");
            if (md5 != null) {
                printer.println("  ", gray("MD5:"), "        ", textOf(md5));
            }
        }
    }

    private void printPublicKeyContent(Path pubKeyPath) {
        printer.println(bold("Public Key Content:"));
        printer.println(gray(SEPARATOR_LINE));

        try {
            String content = Files.readString(pubKeyPath).trim();
            // Wrap long lines for readability
            if (content.length() > 70) {
                printer.println(textOf(Strings.wrapText(content, 70)));
            } else {
                printer.println(textOf(content));
            }
            printer.println(gray(SEPARATOR_LINE));
        } catch (IOException e) {
            printer.println("  ", red("(error reading public key)"));
        }
    }

    // ========================================================================
    // Formatting Methods
    // ========================================================================

    private Text formatUse(KeyUse use) {
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

    private Text formatPermissionWarning(String perms) {
        if (perms.equals("n/a")) {
            return textOf("");
        }
        // Private key should be 600 (rw-------)
        if (!perms.equals("rw-------")) {
            return yellow(" ⚠️  (should be rw-------)");
        }
        return green(" ✓");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        }
        return String.format("%.1f KB", bytes / 1024.0);
    }

    private String formatAlgorithm(String algo) {
        return switch (algo) {
            case "ssh-ed25519" -> "ED25519 (recommended)";
            case "ssh-rsa" -> "RSA";
            case "ecdsa-sha2-nistp256" -> "ECDSA (P-256)";
            case "ecdsa-sha2-nistp384" -> "ECDSA (P-384)";
            case "ecdsa-sha2-nistp521" -> "ECDSA (P-521)";
            case "ssh-dss" -> "DSA (deprecated)";
            default -> algo;
        };
    }

    // ========================================================================
    // Key Detection Methods
    // ========================================================================

    private String detectKeyType(Path keyPath) {
        try (InputStream is = Files.newInputStream(keyPath)) {
            byte[] header = new byte[128];
            int bytesRead = is.read(header);

            if (bytesRead <= 0) {
                return "unknown";
            }

            String headerStr = new String(header, 0, bytesRead);

            if (headerStr.contains("OPENSSH PRIVATE KEY")) {
                return "OpenSSH (ED25519/ECDSA/RSA)";
            } else if (headerStr.contains("RSA PRIVATE KEY")) {
                return "RSA (PEM)";
            } else if (headerStr.contains("EC PRIVATE KEY")) {
                return "ECDSA (PEM)";
            } else if (headerStr.contains("DSA PRIVATE KEY")) {
                return "DSA (PEM) - deprecated";
            } else if (headerStr.contains("PRIVATE KEY")) {
                return "PKCS#8";
            }

            return "unknown";
        } catch (IOException e) {
            return "error";
        }
    }

    private boolean isKeyEncrypted(Path keyPath) {
        try (InputStream is = Files.newInputStream(keyPath)) {
            byte[] content = new byte[512];
            int bytesRead = is.read(content);

            if (bytesRead <= 0) {
                return false;
            }

            String contentStr = new String(content, 0, bytesRead);

            return contentStr.contains("ENCRYPTED") ||
                contentStr.contains("Proc-Type: 4,ENCRYPTED") ||
                contentStr.contains("DEK-Info:");
        } catch (IOException e) {
            return false;
        }
    }

    private int estimateKeyBits(String algo, String base64Key) {
        return switch (algo) {
            case "ssh-ed25519", "ecdsa-sha2-nistp256" -> 256;
            case "ecdsa-sha2-nistp384" -> 384;
            case "ecdsa-sha2-nistp521" -> 521;
            case "ssh-rsa" -> {
                // Rough estimate from base64 length
                int keyBytes = (base64Key.length() * 3) / 4;
                yield (keyBytes - 50) * 8;
            }
            default -> 0;
        };
    }

    // ========================================================================
    // Utility Methods
    // ========================================================================

    private String getPermissions(Path path) {
        try {
            return PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
        } catch (IOException | UnsupportedOperationException e) {
            return "n/a";
        }
    }

    private String getFingerprint(Path pubKeyPath, String hashAlgo) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "ssh-keygen", "-l", "-E", hashAlgo, "-f", pubKeyPath.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.waitFor();

                if (line != null && process.exitValue() == 0) {
                    // Format: "256 SHA256:xxx comment (TYPE)"
                    // Extract just the fingerprint part
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        return parts[1];
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            // Ignore
        }
        return null;
    }

}
