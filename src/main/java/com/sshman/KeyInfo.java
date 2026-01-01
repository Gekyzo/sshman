package com.sshman;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;

import static com.sshman.utils.Dates.DATE_FORMAT;

public record KeyInfo(

    String name,
    String relativePath,
    String type,
    String permissions,
    String modified,
    boolean hasPublicKey

) {

    public static KeyInfo of(Path baseDir, Path path) {
        // Calculate relative path from base SSH directory
        Path relative = baseDir.relativize(path);
        return new KeyInfo(
            path.getFileName().toString(),
            relative.toString(),
            detectKeyType(path),
            getPermissions(path),
            getModifiedTime(path),
            Files.exists(Path.of(path + ".pub"))
        );
    }

    private static String detectKeyType(Path path) {
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

    private static String getPermissions(Path path) {
        try {
            return PosixFilePermissions.toString(
                Files.getPosixFilePermissions(path)
            );
        } catch (IOException | UnsupportedOperationException e) {
            return "n/a";
        }
    }

    private static String getModifiedTime(Path path) {
        try {
            Instant instant = Files.getLastModifiedTime(path).toInstant();
            return DATE_FORMAT.format(instant);
        } catch (IOException e) {
            return "unknown";
        }
    }
}
