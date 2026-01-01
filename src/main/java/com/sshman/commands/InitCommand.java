package com.sshman.commands;

import com.sshman.constants.SshManConstants;
import com.sshman.utils.SshKeyUtils;
import com.sshman.utils.printer.Printer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static com.sshman.constants.SshManConstants.*;
import static com.sshman.utils.printer.Text.*;

@Command(
    name = "init",
    description = "Create a .sshman file in the current directory for auto-loading SSH keys",
    mixinStandardHelpOptions = true
)
public class InitCommand implements Callable<Integer> {

    @Mixin
    private Printer printer;

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

    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);

    @Override
    public Integer call() {
        // Get current working directory
        Path currentDir = Path.of(System.getProperty(SystemProperties.USER_DIR));
        Path sshmanFile = currentDir.resolve(FileNames.SSHMAN_FILE);

        // Check if .sshman already exists
        if (Files.exists(sshmanFile) && !force) {
            printer.error(red("File already exists: "), bold(sshmanFile.toString()));
            printer.error(yellow("Use --force to overwrite"));
            printer.error("");
            printer.error(gray("Current content:"));
            try {
                String content = Files.readString(sshmanFile).trim();
                printer.error("  ", cyan(content));
            } catch (IOException e) {
                printer.error("  ", gray("(unable to read file)"));
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
                printer.error(red("Key not found: "), bold(keyName));
                printer.emptyLine();
                printer.println(yellow("Available keys:"));
                SshKeyUtils.listAvailableKeys(sshDir, printer,
                    SshKeyUtils.ListKeysOptions.defaults());
                return 1;
            }
        }

        // Verify it's a private key
        if (!SshKeyUtils.isPrivateKey(keyPath)) {
            printer.error(red("Not a valid private key: "), bold(keyPath.toString()));
            return 1;
        }

        // Write the .sshman file
        try {
            Files.writeString(sshmanFile, keyName + "\n");

            // Print success header
            printer.println(bold(HEADER_LINE));
            printer.println(bold("  SSH Key Auto-Loading Configured"));
            printer.println(bold(HEADER_LINE));
            printer.emptyLine();

            printer.println(green("✓ "), textOf("Created "), bold(FileNames.SSHMAN_FILE), textOf(" in "), cyan(currentDir.toString()));
            printer.println(label("SSH key"), cyan(keyName));
            printer.emptyLine();

            printer.println(gray(SEPARATOR_LINE));
            printer.emptyLine();

            printer.println(bold("Auto-Loading Behavior:"));
            printer.println(gray("When you enter this directory, the shell hook will automatically run:"));
            printer.println("  ", cyan("sshman use %s", keyName));
            printer.emptyLine();

            printer.println(yellow("⚠ Note: "), textOf("Shell hooks must be installed for auto-switching"));
            printer.println(gray("To install hooks, run:"));
            printer.println("  ", bold("cd completions && ./install.sh"));

            printer.emptyLine();
            printer.println(bold(HEADER_LINE));

            return 0;
        } catch (IOException e) {
            printer.error(red("Failed to create .sshman file: "), textOf(e.getMessage()));
            return 1;
        }
    }

    private Path getSshDirectory() {
        if (customPath != null && !customPath.isEmpty()) {
            return Path.of(customPath);
        }
        return Path.of(System.getProperty(SystemProperties.USER_HOME), DirectoryNames.SSH);
    }

    /**
     * Recursively search for a key by filename in the SSH directory.
     */
    private Path findKeyRecursive(Path baseDir, String keyName) {
        try (Stream<Path> stream = Files.walk(baseDir)) {
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

}
