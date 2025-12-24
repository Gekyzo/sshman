package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;

@Command(
    name = "info",
    description = "Show detailed information about a specific key",
    mixinStandardHelpOptions = true
)
public class InfoCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", description = "Key name (filename in ~/.ssh)")
    private String keyName;

    @Option(names = {"-p", "--public"}, description = "Show public key content")
    private boolean showPublicKey;

    @Option(names = {"-f", "--fingerprint"}, description = "Show key fingerprint")
    private boolean showFingerprint;

    @Option(names = {"--path"}, description = "Custom SSH directory path")
    private String customPath;

    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @Override
    public Integer call() {
        PrintWriter err = spec.commandLine().getErr();

        Path sshDir = getSshDirectory();
        
        // Try to resolve the key (supports paths like "work/project-a/id_ed25519")
        Path keyPath = sshDir.resolve(keyName);
        Path pubKeyPath = Path.of(keyPath.toString() + ".pub");

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
                    pubKeyPath = Path.of(foundKey.toString() + ".pub");
                } else {
                    err.println("Key not found: " + keyName);
                    err.println();
                    err.println("Available keys:");
                    listAvailableKeys(sshDir);
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
                .filter(this::isPrivateKey)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private void printKeyInfo(Path keyPath, Path pubKeyPath) {
        PrintWriter out = spec.commandLine().getOut();

        out.println("═══════════════════════════════════════════════════════════════");
        out.println("  SSH Key Information");
        out.println("═══════════════════════════════════════════════════════════════");
        out.println();

        // Basic info
        out.println("Name:         " + keyName);

        if (keyPath != null && Files.exists(keyPath)) {
            printPrivateKeyInfo(keyPath);
        } else {
            out.println("Private Key:  (not found)");
        }

        if (Files.exists(pubKeyPath)) {
            printPublicKeyInfo(pubKeyPath);
        } else {
            out.println("Public Key:   (not found)");
        }

        // Fingerprint (always useful)
        if (Files.exists(pubKeyPath)) {
            out.println();
            printFingerprint(pubKeyPath);
        }

        // Show public key content if requested or by default
        if (showPublicKey && Files.exists(pubKeyPath)) {
            out.println();
            printPublicKeyContent(pubKeyPath);
        }

        out.println();
        out.println("═══════════════════════════════════════════════════════════════");
    }

    private void printPrivateKeyInfo(Path keyPath) {
        PrintWriter out = spec.commandLine().getOut();

        out.println("Private Key:  " + keyPath);

        try {
            // Type
            String keyType = detectKeyType(keyPath);
            out.println("Type:         " + keyType);

            // Permissions
            String perms = getPermissions(keyPath);
            out.println("Permissions:  " + perms + formatPermissionWarning(perms));

            // Size
            long size = Files.size(keyPath);
            out.println("Size:         " + formatSize(size));

            // Modified time
            Instant modified = Files.getLastModifiedTime(keyPath).toInstant();
            out.println("Modified:     " + DATE_FORMAT.format(modified));

            // Check if encrypted
            boolean encrypted = isKeyEncrypted(keyPath);
            out.println("Encrypted:    " + (encrypted ? "yes (passphrase protected)" : "no"));

        } catch (IOException e) {
            out.println("  (error reading key details)");
        }
    }

    private void printPublicKeyInfo(Path pubKeyPath) {
        PrintWriter out = spec.commandLine().getOut();

        out.println();
        out.println("Public Key:   " + pubKeyPath);

        try {
            String content = Files.readString(pubKeyPath).trim();
            String[] parts = content.split("\\s+");

            if (parts.length >= 1) {
                out.println("Algorithm:    " + formatAlgorithm(parts[0]));
            }

            if (parts.length >= 3) {
                out.println("Comment:      " + parts[2]);
            }

            // Key length (approximate from base64)
            if (parts.length >= 2) {
                int bitLength = estimateKeyBits(parts[0], parts[1]);
                if (bitLength > 0) {
                    out.println("Key Bits:     ~" + bitLength);
                }
            }

        } catch (IOException e) {
            out.println("  (error reading public key)");
        }
    }

    private void printFingerprint(Path pubKeyPath) {
        PrintWriter out = spec.commandLine().getOut();

        out.println("Fingerprints:");

        // SHA256 fingerprint (default)
        String sha256 = getFingerprint(pubKeyPath, "sha256");
        if (sha256 != null) {
            out.println("  SHA256:     " + sha256);
        }

        // MD5 fingerprint (legacy, but sometimes needed)
        if (showFingerprint) {
            String md5 = getFingerprint(pubKeyPath, "md5");
            if (md5 != null) {
                out.println("  MD5:        " + md5);
            }
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

    private void printPublicKeyContent(Path pubKeyPath) {
        PrintWriter out = spec.commandLine().getOut();

        out.println("Public Key Content:");
        out.println("───────────────────────────────────────────────────────────────");
        try {
            String content = Files.readString(pubKeyPath).trim();
            // Wrap long lines for readability
            if (content.length() > 70) {
                out.println(wrapText(content, 70));
            } else {
                out.println(content);
            }
            out.println("───────────────────────────────────────────────────────────────");
        } catch (IOException e) {
            out.println("  (error reading public key)");
        }
    }

    private String detectKeyType(Path keyPath) {
        try (InputStream is = Files.newInputStream(keyPath)) {
            byte[] header = new byte[128];
            int bytesRead = is.read(header);

            if (bytesRead <= 0) {
                return "unknown";
            }

            String headerStr = new String(header, 0, bytesRead);

            if (headerStr.contains("OPENSSH PRIVATE KEY")) {
                // Modern OpenSSH format - could be ed25519, ecdsa, or rsa
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

            // Check for encryption indicators
            return contentStr.contains("ENCRYPTED") ||
                contentStr.contains("Proc-Type: 4,ENCRYPTED") ||
                contentStr.contains("DEK-Info:");
        } catch (IOException e) {
            return false;
        }
    }

    private String getPermissions(Path path) {
        try {
            return PosixFilePermissions.toString(Files.getPosixFilePermissions(path));
        } catch (IOException | UnsupportedOperationException e) {
            return "n/a";
        }
    }

    private String formatPermissionWarning(String perms) {
        if (perms.equals("n/a")) {
            return "";
        }
        // Private key should be 600 (rw-------)
        if (!perms.equals("rw-------")) {
            return " ⚠️  (should be rw-------)";
        }
        return " ✓";
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

    private int estimateKeyBits(String algo, String base64Key) {
        return switch (algo) {
            case "ssh-ed25519" -> 256;
            case "ecdsa-sha2-nistp256" -> 256;
            case "ecdsa-sha2-nistp384" -> 384;
            case "ecdsa-sha2-nistp521" -> 521;
            case "ssh-rsa" -> {
                // Rough estimate from base64 length
                int keyBytes = (base64Key.length() * 3) / 4;
                yield (keyBytes - 50) * 8; // Subtract overhead
            }
            default -> 0;
        };
    }

    private String wrapText(String text, int width) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int end = Math.min(i + width, text.length());
            sb.append(text, i, end);
            if (end < text.length()) {
                sb.append("\n");
            }
            i = end;
        }
        return sb.toString();
    }

    private void listAvailableKeys(Path sshDir) {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        try (var files = Files.walk(sshDir)) {
            files.filter(Files::isRegularFile)
                .filter(p -> !p.getFileName().toString().endsWith(".pub"))
                .filter(p -> !p.getFileName().toString().equals("config"))
                .filter(p -> !p.getFileName().toString().equals("known_hosts"))
                .filter(p -> !p.getFileName().toString().equals("authorized_keys"))
                .filter(this::isPrivateKey)
                .sorted()
                .forEach(p -> {
                    Path relativePath = sshDir.relativize(p);
                    out.println("  - " + relativePath);
                });
        } catch (IOException e) {
            err.println("  (error listing keys)");
        }
    }

    private boolean isPrivateKey(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] header = new byte[32];
            int bytesRead = is.read(header);
            if (bytesRead < 10) return false;
            String headerStr = new String(header, 0, bytesRead);
            return headerStr.contains("-----BEGIN");
        } catch (IOException e) {
            return false;
        }
    }
}
