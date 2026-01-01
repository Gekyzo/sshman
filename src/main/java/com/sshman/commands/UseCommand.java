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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static com.sshman.constants.SshManConstants.*;
import static com.sshman.utils.printer.Text.*;

@Command(
    name = "use",
    aliases = {"set"},
    description = "Start ssh-agent and add the specified SSH key",
    mixinStandardHelpOptions = true
)
public class UseCommand implements Callable<Integer> {

    @Mixin
    private Printer printer;

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

    // ========================================================================
    // Constants
    // ========================================================================

    private static final String HEADER_LINE = "═".repeat(65);
    private static final String SEPARATOR_LINE = "─".repeat(65);

    // ========================================================================
    // Main Entry Point
    // ========================================================================

    @Override
    public Integer call() {
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
                printer.error(red("Key not found: "), bold(keyName));
                printer.emptyLine();
                printer.println(yellow("Available keys:"));
                SshKeyUtils.listAvailableKeys(sshDir, printer,
                    SshKeyUtils.ListKeysOptions.defaults()
                        .excludeArchivedKeys(true)
                        .excludeMetaFiles(true)
                        .showEmptyMessage(true));
                return 1;
            }
        }

        // Verify it's a private key
        if (!SshKeyUtils.isPrivateKey(keyPath)) {
            printer.error(red("Not a valid private key: "), bold(keyPath.toString()));
            return 1;
        }

        // Generate the commands to start ssh-agent and add the key
        return generateAgentCommands(keyPath);
    }

    // ========================================================================
    // Directory and Path Methods
    // ========================================================================

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
        Path archiveDir = baseDir.resolve(DirectoryNames.ARCHIVED);
        try (Stream<Path> stream = Files.walk(baseDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(p -> !p.startsWith(archiveDir))  // Exclude archived directory
                .filter(p -> p.getFileName().toString().equals(keyName))
                .filter(SshKeyUtils::isPrivateKey)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    // ========================================================================
    // Agent Command Generation
    // ========================================================================

    private int generateAgentCommands(Path keyPath) {
        // Check if ssh-agent is already running
        String sshAuthSock = System.getenv(EnvironmentVariables.SSH_AUTH_SOCK);
        String sshAgentPid = System.getenv(EnvironmentVariables.SSH_AGENT_PID);
        boolean agentRunning = sshAuthSock != null && !sshAuthSock.isEmpty() &&
            sshAgentPid != null && !sshAgentPid.isEmpty();

        // Build the equivalent command
        String equivalentCommand = buildEquivalentCommand(keyPath, agentRunning);

        if (quiet) {
            // Quiet mode - only output the command for eval
            printer.println(equivalentCommand);
            return 0;
        }

        // Print header
        printer.println(bold(HEADER_LINE));
        printer.println(bold("  SSH Agent Key Setup"));
        printer.println(bold(HEADER_LINE));
        printer.emptyLine();

        // Show key info
        printer.println(label("Key"), cyan(keyPath.toString()));
        if (lifetime != null) {
            printer.println(label("Lifetime"), textOf("%d seconds", lifetime));
        }
        printer.emptyLine();

        // Show equivalent command
        printer.println(gray("Equivalent SSH command:"));
        printer.println("  ", textOf(equivalentCommand));
        printer.emptyLine();

        printer.println(gray(SEPARATOR_LINE));
        printer.emptyLine();

        // Show agent status
        if (agentRunning) {
            printer.println(green("✓ "), textOf("ssh-agent is already running"));
            printer.println("  ", label(EnvironmentVariables.SSH_AUTH_SOCK), gray(sshAuthSock));
            printer.println("  ", label(EnvironmentVariables.SSH_AGENT_PID), gray(sshAgentPid));
            printer.emptyLine();
            printer.println(yellow("→ "), textOf("Clearing existing keys and adding new key..."));
        } else {
            printer.println(yellow("! "), textOf("No ssh-agent running"));
            printer.println(yellow("→ "), textOf("Starting a new ssh-agent and adding key..."));
        }

        printer.emptyLine();
        printer.println(gray(SEPARATOR_LINE));
        printer.emptyLine();

        // Show usage instructions
        printer.println(bold("To activate this key, run:"));
        printer.emptyLine();
        printer.println("  ", cyan("eval \"$(sshman use %s --quiet)\"", keyName));
        printer.emptyLine();

        printer.println(gray("Or copy and paste the command below:"));
        printer.emptyLine();
        printer.println("  ", bold(equivalentCommand));

        printer.emptyLine();
        printer.println(bold(HEADER_LINE));

        return 0;
    }

    /**
     * Build the equivalent SSH command based on agent status.
     */
    private String buildEquivalentCommand(Path keyPath, boolean agentRunning) {
        StringBuilder cmd = new StringBuilder();

        if (agentRunning) {
            // Clear existing keys and add the new one
            cmd.append("ssh-add -D && ssh-add");
        } else {
            // Need to start ssh-agent
            cmd.append("eval \"$(ssh-agent -s)\" && ssh-add");
        }

        if (lifetime != null) {
            cmd.append(" -t ").append(lifetime);
        }

        cmd.append(" ").append(keyPath);

        return cmd.toString();
    }

    // ========================================================================
    // Completion Candidates
    // ========================================================================

    /**
     * Custom completion candidates for key names.
     * This enables tab-completion for SSH keys.
     */
    public static class KeyNameCompletionCandidates implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return SshKeyUtils.getKeyCompletionCandidates(SshKeyUtils.KeyScanMode.NON_ARCHIVED);
        }
    }
}
