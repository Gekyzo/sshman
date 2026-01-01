package com.sshman.utils;

import com.sshman.constants.SshManConstants;
import com.sshman.utils.printer.Printer;
import com.sshman.utils.printer.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.sshman.constants.SshManConstants.*;
import static com.sshman.utils.printer.Text.*;

/**
 * Utility class for SSH key operations.
 */
public final class SshKeyUtils {

    private static final byte[] PRIVATE_KEY_MAGIC = PrivateKeyHeaders.PRIVATE_KEY_MAGIC;

    private SshKeyUtils() {
        // Utility class
    }

    /**
     * Checks if a file is a private SSH key by examining its header.
     *
     * @param path the path to check
     * @return true if the file is a private key, false otherwise
     */
    public static boolean isPrivateKey(Path path) {
        if (path.getFileName().toString().endsWith(FileExtensions.PUBLIC_KEY)) {
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

    /**
     * Lists available SSH keys in a directory using a Printer with color formatting.
     *
     * @param sshDir the SSH directory to scan
     * @param printer the printer to use for output
     * @param options configuration options for filtering and display
     */
    public static void listAvailableKeys(Path sshDir, Printer printer, ListKeysOptions options) {
        try (Stream<Path> files = Files.walk(sshDir)) {
            List<Path> keys = filterKeys(files, sshDir, options).toList();

            if (keys.isEmpty() && options.showEmptyMessage) {
                printer.println(gray("  (no keys found)"));
            } else {
                for (Path key : keys) {
                    Path relativePath = sshDir.relativize(key);
                    printer.println("  ", gray("- "), textOf(relativePath.toString()));
                }
            }
        } catch (IOException e) {
            printer.error(red("  (error listing keys)"));
        }
    }

    /**
     * Lists available SSH keys in a directory using a PrintWriter (plain text output).
     *
     * @param sshDir the SSH directory to scan
     * @param writer the writer to use for output
     * @param options configuration options for filtering and display
     */
    public static void listAvailableKeys(Path sshDir, PrintWriter writer, ListKeysOptions options) {
        try (Stream<Path> files = Files.walk(sshDir)) {
            List<Path> keys = filterKeys(files, sshDir, options).toList();

            if (keys.isEmpty() && options.showEmptyMessage) {
                writer.println("  (no keys found)");
            } else {
                for (Path key : keys) {
                    Path relativePath = sshDir.relativize(key);
                    writer.println("  - " + relativePath);
                }
            }
        } catch (IOException e) {
            writer.println("  (error listing keys)");
        }
    }

    /**
     * Gets a list of SSH key paths from a directory.
     *
     * @param sshDir the SSH directory to scan
     * @param options configuration options for filtering
     * @return list of key paths
     */
    public static List<Path> getAvailableKeys(Path sshDir, ListKeysOptions options) {
        try (Stream<Path> files = Files.walk(sshDir)) {
            return filterKeys(files, sshDir, options).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Stream<Path> filterKeys(Stream<Path> files, Path sshDir, ListKeysOptions options) {
        Stream<Path> stream = files
            .filter(Files::isRegularFile)
            .filter(p -> !p.getFileName().toString().endsWith(FileExtensions.PUBLIC_KEY))
            .filter(p -> !p.getFileName().toString().equals(FileNames.CONFIG))
            .filter(p -> !p.getFileName().toString().equals(FileNames.KNOWN_HOSTS))
            .filter(p -> !p.getFileName().toString().equals(FileNames.AUTHORIZED_KEYS));

        if (options.excludeArchivedKeys) {
            Path archiveDir = sshDir.resolve(DirectoryNames.ARCHIVED);
            stream = stream.filter(p -> !p.startsWith(archiveDir));
        }

        if (options.excludeMetaFiles) {
            stream = stream.filter(p -> !p.getFileName().toString().endsWith(FileExtensions.METADATA));
        }

        return stream
            .filter(SshKeyUtils::isPrivateKey)
            .sorted();
    }

    /**
     * Configuration options for listing SSH keys.
     */
    public static class ListKeysOptions {
        private boolean excludeArchivedKeys = false;
        private boolean excludeMetaFiles = false;
        private boolean showEmptyMessage = false;

        public static ListKeysOptions defaults() {
            return new ListKeysOptions();
        }

        public ListKeysOptions excludeArchivedKeys(boolean exclude) {
            this.excludeArchivedKeys = exclude;
            return this;
        }

        public ListKeysOptions excludeMetaFiles(boolean exclude) {
            this.excludeMetaFiles = exclude;
            return this;
        }

        public ListKeysOptions showEmptyMessage(boolean show) {
            this.showEmptyMessage = show;
            return this;
        }
    }

    /**
     * Scan mode for SSH key completion candidates.
     */
    public enum KeyScanMode {
        /** Scan non-archived keys only */
        NON_ARCHIVED,
        /** Scan archived keys only */
        ARCHIVED_ONLY,
        /** Scan all keys */
        ALL
    }

    /**
     * Gets an iterator of SSH key names for tab-completion candidates.
     *
     * @param scanMode the mode determining which keys to include
     * @return iterator of relative key paths as strings
     */
    public static Iterator<String> getKeyCompletionCandidates(KeyScanMode scanMode) {
        List<String> keys = new ArrayList<>();
        Path sshDir = Path.of(System.getProperty(SystemProperties.USER_HOME), DirectoryNames.SSH);
        Path baseDir = scanMode == KeyScanMode.ARCHIVED_ONLY
            ? sshDir.resolve(DirectoryNames.ARCHIVED)
            : sshDir;

        if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            return keys.iterator();
        }

        ListKeysOptions options = ListKeysOptions.defaults()
            .excludeMetaFiles(true)
            .excludeArchivedKeys(scanMode == KeyScanMode.NON_ARCHIVED);

        getAvailableKeys(baseDir, options).forEach(p -> {
            Path relativePath = baseDir.relativize(p);
            keys.add(relativePath.toString());
        });

        return keys.iterator();
    }
}
