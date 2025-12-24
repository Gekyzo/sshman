package com.sshman;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "init",
    description = "Create a .sshman file in the current directory for auto-loading SSH keys",
    mixinStandardHelpOptions = true
)
public class InitCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(
        index = "0",
        description = "SSH key name to use for this directory",
        completionCandidates = UseCommand.KeyNameCompletionCandidates.class
    )
    private String keyName;

    @Option(names = {"-f", "--force"}, description = "Overwrite existing .sshman file")
    private boolean force;

    @Option(names = {"--path"}, description = "Custom SSH directory path")
    private String customPath;

    // Private key header bytes: "-----BEGIN"
    private static final byte[] PRIVATE_KEY_MAGIC = "-----BEGIN".getBytes();
    private static final String SSHMAN_FILE = ".sshman";

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();

        // Get current working directory
        Path currentDir = Path.of(System.getProperty("user.dir"));
        Path sshmanFile = currentDir.resolve(SSHMAN_FILE);

        // Check if .sshman already exists
        if (Files.exists(sshmanFile) && !force) {
            err.println("File already exists: " + sshmanFile);
            err.println("Use --force to overwrite");
            err.println();
            err.println("Current content:");
            try {
                String content = Files.readString(sshmanFile).trim();
                err.println("  " + content);
            } catch (IOException e) {
                err.println("  (unable to read file)");
            }
            return 1;
        }

        // Validate the key exists
        Path sshDir = getSshDirectory();
        Path keyPath = sshDir.resolve(keyName);

        if (!Files.exists(keyPath)) {
            // Try to find the key recursively
            Path foundKey = findKeyRecursive(sshDir, keyName);
            if (foundKey != null) {
                keyPath = foundKey;
                // Use relative path from ssh directory
                keyName = sshDir.relativize(keyPath).toString();
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

        // Write the .sshman file
        try {
            Files.writeString(sshmanFile, keyName + "\n");
            out.println("Created " + SSHMAN_FILE + " in " + currentDir);
            out.println("SSH key: " + keyName);
            out.println();
            out.println("When you enter this directory, the shell hook will automatically run:");
            out.println("  sshman use " + keyName);
            out.println();
            out.println("To enable auto-switching, make sure you've installed the shell hooks:");
            out.println("  cd completions && ./install.sh");
            return 0;
        } catch (IOException e) {
            err.println("Failed to create .sshman file: " + e.getMessage());
            return 1;
        }
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
}
